const state = {
  activePane: "account",
  apiKey: localStorage.getItem("arbiterApiKey") || "",
};

const byId = (id) => document.getElementById(id);

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function asJson(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function nextTransactionId(prefix = "WEB") {
  return prefix + "-" + new Date().toISOString().replace(/[-:.TZ]/g, "").slice(0, 14);
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (state.apiKey && path !== "/health" && path !== "/config") {
    headers.set("x-arbiter-api-key", state.apiKey);
  }
  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  let payload;
  try {
    payload = JSON.parse(text);
  } catch {
    payload = { raw: text };
  }
  if (!response.ok) {
    throw new Error(payload.message || payload.error || response.statusText);
  }
  return payload;
}

function syncAuthState() {
  byId("apiKeyInput").value = state.apiKey;
  byId("authText").textContent = state.apiKey
    ? "API key loaded for ledger operations"
    : "API key required for ledger operations";
}

function setResponse(status, payload) {
  byId("responseStatus").textContent = status;
  byId("resultBox").textContent = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
}

function setFormError(id, message) {
  byId(id).textContent = message || "";
}

function activatePane(pane) {
  state.activePane = pane;
  document.querySelectorAll(".tab").forEach((button) => {
    const active = button.id.toLowerCase().startsWith(pane);
    button.classList.toggle("active", active);
    button.setAttribute("aria-selected", String(active));
  });
  document.querySelectorAll(".operation-form").forEach((form) => {
    form.classList.toggle("active", form.dataset.pane === pane);
  });
}

async function refreshHealth() {
  try {
    await request("/health");
    byId("healthDot").className = "dot ok";
    byId("healthText").textContent = "Backend connected";
  } catch {
    byId("healthDot").className = "dot fail";
    byId("healthText").textContent = "Backend unavailable";
  }
}

async function refreshAccounts() {
  const accounts = await request("/accounts");
  byId("accountCount").textContent = accounts.length + " accounts";
  byId("accountsBody").innerHTML = accounts.length
    ? accounts.map((account) => `
      <tr>
        <td>${escapeHtml(account.accountId)}</td>
        <td>${escapeHtml(account.currency)}</td>
        <td>${escapeHtml(account.balance)}</td>
      </tr>
    `).join("")
    : `<tr><td colspan="3">No accounts yet.</td></tr>`;
}

async function refreshReconcile() {
  const report = await request("/reconcile");
  byId("journalMetric").textContent = report.globalJournalSum;
  byId("entryCount").textContent = String(report.entryCount);
  byId("issueCount").textContent = String(report.issues.length);
  byId("reconcileBadge").className = "state-badge " + (report.balanced ? "ok" : "fail");
  byId("reconcileBadge").textContent = report.balanced ? "Balanced" : "Failed";
  byId("reconcileText").textContent = report.balanced
    ? `Opening balances and journal history reconcile across ${report.accountCount} accounts.`
    : `Reconciliation failed across ${report.issues.length} issue(s).`;
}

async function refreshAudit() {
  const audit = await request("/audit");
  byId("auditCount").textContent = audit.eventCount + " events";
  const events = audit.events.slice(-12).reverse();
  byId("auditList").innerHTML = events.length
    ? events.map((event) => `<li>${escapeHtml(event)}</li>`).join("")
    : "<li>No live events yet.</li>";
}

async function refreshBenchmarks() {
  const benchmark = await request("/benchmarks/latest");
  if (!benchmark.available) {
    return;
  }
  const result = benchmark.result;
  byId("settledMetric").textContent = String(result.settled);
  byId("latencyMetric").textContent = result.p99Micros + " us";
  byId("duplicateMetric").textContent = String(result.duplicateResponses);
  byId("errorMetric").textContent = String(result.errors);
}

async function refreshAll() {
  await refreshHealth();
  await Promise.all([
    refreshAccounts().catch(() => {}),
    refreshReconcile().catch(() => {}),
    refreshAudit().catch(() => {}),
    refreshBenchmarks().catch(() => {}),
  ]);
}

async function ensureDemoAccounts() {
  const accounts = await request("/accounts").catch(() => []);
  const ids = new Set(accounts.map((account) => account.accountId));
  const defaults = [
    { accountId: "CustomerA", currency: "USD", openingBalance: "1000.00" },
    { accountId: "CustomerB", currency: "USD", openingBalance: "100.00" },
  ];
  for (const account of defaults) {
    if (!ids.has(account.accountId)) {
      await request("/accounts", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(account),
      }).catch(() => {});
    }
  }
}

byId("accountTab").addEventListener("click", () => activatePane("account"));
byId("paymentTab").addEventListener("click", () => activatePane("payment"));
byId("isoTab").addEventListener("click", () => activatePane("iso"));
byId("refreshButton").addEventListener("click", refreshAll);

byId("authForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  state.apiKey = new FormData(event.currentTarget).get("apiKey").trim();
  if (state.apiKey) {
    localStorage.setItem("arbiterApiKey", state.apiKey);
  } else {
    localStorage.removeItem("arbiterApiKey");
  }
  syncAuthState();
  await refreshAll();
});

byId("accountForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  setFormError("accountError", "");
  try {
    const result = await request("/accounts", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(asJson(event.currentTarget)),
    });
    setResponse("created", result);
    await refreshAll();
  } catch (error) {
    setFormError("accountError", error.message);
    setResponse("error", error.message);
  }
});

byId("paymentForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  setFormError("paymentError", "");
  try {
    const payload = asJson(event.currentTarget);
    const result = await request("/payments", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(payload),
    });
    setResponse(result.status.toLowerCase(), result);
    byId("endToEndId").value = nextTransactionId("WEB");
    await refreshAll();
  } catch (error) {
    setFormError("paymentError", error.message);
    setResponse("error", error.message);
  }
});

byId("isoForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  setFormError("isoError", "");
  try {
    const xmlPayload = new FormData(event.currentTarget).get("xmlPayload");
    const result = await request("/payments/iso20022", {
      method: "POST",
      headers: { "content-type": "application/xml" },
      body: xmlPayload,
    });
    setResponse(result.status.toLowerCase(), result);
    const nextId = nextTransactionId("ISO-WEB");
    const textarea = event.currentTarget.elements.xmlPayload;
    textarea.value = textarea.value.replace(/<EndToEndId>[^<]+<\/EndToEndId>/, `<EndToEndId>${nextId}</EndToEndId>`);
    await refreshAll();
  } catch (error) {
    setFormError("isoError", error.message);
    setResponse("error", error.message);
  }
});

syncAuthState();
ensureDemoAccounts().finally(refreshAll);
