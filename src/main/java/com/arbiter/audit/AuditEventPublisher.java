package com.arbiter.audit;

import com.arbiter.domain.FraudScore;
import com.arbiter.domain.JournalEntry;
import com.arbiter.domain.PaymentInstruction;
import com.arbiter.domain.SettlementResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.zip.CRC32;

public final class AuditEventPublisher {
    private static final int RECENT_EVENT_LIMIT = 500;

    private final Path auditFile;
    private final Deque<String> recentEvents = new ArrayDeque<>();

    public AuditEventPublisher(Path auditFile) {
        try {
            Files.createDirectories(auditFile.getParent());
            this.auditFile = auditFile;
            if (!Files.exists(auditFile)) {
                Files.createFile(auditFile);
            }
            replayRecentEvents();
        } catch (IOException ex) {
            throw new AuditException("failed to initialize audit log: " + ex.getMessage());
        }
    }

    public synchronized void paymentInitiated(PaymentInstruction payment) {
        append("arbiter.payments.initiated " + payment.endToEndId());
    }

    public synchronized void fraudScored(PaymentInstruction payment, FraudScore score) {
        append("arbiter.fraud.scored " + payment.endToEndId() + " score=" + score.value());
    }

    public synchronized void journalEntryPosted(JournalEntry entry) {
        append("arbiter.journal.entries " + entry.transactionId() + " " + entry.entryType() + " " + entry.amount());
    }

    public synchronized void settlementCompleted(SettlementResult result) {
        append("arbiter.payments.settled " + result.transactionId());
        append("arbiter.audit.immutable " + result.transactionId());
    }

    public synchronized void settlementDeclined(PaymentInstruction payment, FraudScore score) {
        append("arbiter.payments.declined " + payment.endToEndId() + " score=" + score.value());
        append("arbiter.audit.immutable " + payment.endToEndId());
    }

    public synchronized List<String> events() {
        return List.copyOf(recentEvents);
    }

    private void append(String event) {
        String payload = Instant.now() + " " + event;
        String line = payload + "|" + checksum(payload) + System.lineSeparator();
        ByteBuffer buffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
        try (FileChannel channel = FileChannel.open(auditFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
             FileLock ignored = channel.lock()) {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
            remember(payload);
        } catch (IOException ex) {
            throw new AuditException("failed durable audit append: " + ex.getMessage());
        }
    }

    private void replayRecentEvents() throws IOException {
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(auditFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                remember(verify(line, lineNumber));
            }
        }
    }

    private void remember(String event) {
        recentEvents.addLast(event);
        while (recentEvents.size() > RECENT_EVENT_LIMIT) {
            recentEvents.removeFirst();
        }
    }

    private String verify(String line, int lineNumber) {
        int split = line.lastIndexOf('|');
        if (split < 0) {
            throw new AuditException("missing audit checksum at line " + lineNumber);
        }
        String payload = line.substring(0, split);
        String expected = line.substring(split + 1);
        String actual = checksum(payload);
        if (!actual.equals(expected)) {
            throw new AuditException("audit checksum mismatch at line " + lineNumber);
        }
        return payload;
    }

    private static String checksum(String payload) {
        CRC32 crc32 = new CRC32();
        crc32.update(payload.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc32.getValue());
    }
}

