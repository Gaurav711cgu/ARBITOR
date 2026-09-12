package com.arbiter.iso20022;

import com.arbiter.domain.PaymentInstruction;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;

public final class Iso20022MessageParser {
    public PaymentInstruction parsePain001(String xmlPayload) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlPayload)));

            String endToEndId = text(document, "EndToEndId");
            String debtor = firstText(document, "Dbtr", "Nm");
            String creditor = lastText(document, "Cdtr", "Nm");
            String amount = attributeOrText(document, "InstdAmt", "Ccy");
            String currency = attribute(document, "InstdAmt", "Ccy");
            String purposeCode = optionalText(document, "Cd", "OTHR");

            return new PaymentInstruction(
                    endToEndId,
                    debtor,
                    creditor,
                    new BigDecimal(amount),
                    currency,
                    purposeCode,
                    "iso20022"
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid pain.001 payload: " + ex.getMessage(), ex);
        }
    }

    private static String text(Document document, String tag) {
        NodeList nodes = document.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            throw new IllegalArgumentException("missing tag: " + tag);
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static String optionalText(Document document, String tag, String fallback) {
        NodeList nodes = document.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent().isBlank()) {
            return fallback;
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static String firstText(Document document, String parentTag, String childTag) {
        NodeList parents = document.getElementsByTagName(parentTag);
        if (parents.getLength() == 0) {
            throw new IllegalArgumentException("missing tag: " + parentTag);
        }
        NodeList children = ((org.w3c.dom.Element) parents.item(0)).getElementsByTagName(childTag);
        if (children.getLength() == 0) {
            throw new IllegalArgumentException("missing child tag: " + childTag);
        }
        return children.item(0).getTextContent().trim();
    }

    private static String lastText(Document document, String parentTag, String childTag) {
        NodeList parents = document.getElementsByTagName(parentTag);
        if (parents.getLength() == 0) {
            throw new IllegalArgumentException("missing tag: " + parentTag);
        }
        NodeList children = ((org.w3c.dom.Element) parents.item(parents.getLength() - 1)).getElementsByTagName(childTag);
        if (children.getLength() == 0) {
            throw new IllegalArgumentException("missing child tag: " + childTag);
        }
        return children.item(0).getTextContent().trim();
    }

    private static String attribute(Document document, String tag, String attributeName) {
        NodeList nodes = document.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            throw new IllegalArgumentException("missing tag: " + tag);
        }
        String value = ((org.w3c.dom.Element) nodes.item(0)).getAttribute(attributeName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing attribute " + attributeName + " on " + tag);
        }
        return value.trim();
    }

    private static String attributeOrText(Document document, String tag, String attributeName) {
        NodeList nodes = document.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            throw new IllegalArgumentException("missing tag: " + tag);
        }
        String text = nodes.item(0).getTextContent();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("missing amount text for " + tag + " with " + attributeName);
        }
        return text.trim();
    }
}

