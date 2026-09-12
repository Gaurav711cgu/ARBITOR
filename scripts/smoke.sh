#!/usr/bin/env bash
set -euo pipefail

API_KEY="${ARBITER_API_KEY:?set ARBITER_API_KEY to the key printed by ./scripts/run.sh}"
BASE_URL="${ARBITER_BASE_URL:-http://localhost:8080}"

curl -fsS "$BASE_URL/health" >/dev/null
curl -fsS -X POST "$BASE_URL/accounts" \
  -H "content-type: application/json" \
  -H "x-arbiter-api-key: $API_KEY" \
  -d '{"accountId":"SmokeA","currency":"USD","openingBalance":"1000.00"}' >/dev/null
curl -fsS -X POST "$BASE_URL/accounts" \
  -H "content-type: application/json" \
  -H "x-arbiter-api-key: $API_KEY" \
  -d '{"accountId":"SmokeB","currency":"USD","openingBalance":"0.00"}' >/dev/null
curl -fsS -X POST "$BASE_URL/payments" \
  -H "content-type: application/json" \
  -H "x-arbiter-api-key: $API_KEY" \
  -d '{"endToEndId":"SMOKE-001","debtorAccount":"SmokeA","creditorAccount":"SmokeB","amount":"25.00","currency":"USD","purposeCode":"GDDS"}' >/dev/null
curl -fsS "$BASE_URL/reconcile" -H "x-arbiter-api-key: $API_KEY" | grep -q '"balanced":true'

echo "Arbiter smoke test passed"

