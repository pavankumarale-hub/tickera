#!/usr/bin/env bash
#
# End-to-end demo against a running `docker compose up` stack.
#
# Usage:
#   ./scripts/demo.sh           # happy path  — booking lands in PAID
#   ./scripts/demo.sh --fail    # failure path — amount > $1 000, DECLINED → CANCELLED
#
# The two paths exercise every service and every state:
#
#   Happy:   CREATED → CONFIRMED → PAID
#   Failure: CREATED → CONFIRMED → CANCELLED  (saga compensation via PaymentFailed)
#
set -euo pipefail

BOOKING=http://localhost:8081
PAYMENT=http://localhost:8082
NOTIFY=http://localhost:8083

FAIL_MODE=false
[[ "${1:-}" == "--fail" ]] && FAIL_MODE=true

# ── helpers ──────────────────────────────────────────────────────────────────
say()  { printf "\n\033[1;36m▶ %s\033[0m\n" "$1"; }
ok()   { printf "  \033[1;32m✔\033[0m %s\n" "$1"; }
warn() { printf "  \033[1;33m⚠\033[0m %s\n" "$1"; }
json() { python3 -c 'import sys,json;print(json.dumps(json.load(sys.stdin),indent=2))'; }
field() { python3 -c "import sys,json;print(json.load(sys.stdin)[\"$1\"])"; }

# ── 0. Wait for services to be ready ─────────────────────────────────────────
say "0. Checking service health"
for svc in "$BOOKING" "$PAYMENT" "$NOTIFY"; do
  for i in $(seq 1 20); do
    if curl -fsS "$svc/actuator/health" 2>/dev/null | python3 -c \
        'import sys,json; s=json.load(sys.stdin)["status"]; exit(0 if s=="UP" else 1)' 2>/dev/null; then
      ok "$svc is UP"
      break
    fi
    [[ $i -eq 20 ]] && { warn "$svc not ready after 20 s — is docker compose up running?"; exit 1; }
    sleep 1
  done
done

# ── choose amount based on mode ───────────────────────────────────────────────
if $FAIL_MODE; then
  AMOUNT=2000.00
  say "=== FAILURE / COMPENSATION PATH (amount $AMOUNT > \$1 000 → DECLINED → CANCELLED) ==="
else
  AMOUNT=240.00
  say "=== HAPPY PATH (amount $AMOUNT → PAID) ==="
fi

# ── 1. Create ─────────────────────────────────────────────────────────────────
say "1. Create a booking (lands in CREATED)"
CREATE_RESP=$(curl -fsS -X POST "$BOOKING/api/v1/bookings" \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"cust-42\",\"eventName\":\"Symphony Gala\",\"seats\":3,\"amount\":$AMOUNT,\"currency\":\"USD\"}")
echo "$CREATE_RESP" | json
BOOKING_ID=$(printf '%s' "$CREATE_RESP" | field bookingId)
ok "bookingId = $BOOKING_ID"

# ── 2. Confirm ────────────────────────────────────────────────────────────────
say "2. Confirm the booking (emits BookingConfirmed → Kafka → payment-service)"
curl -fsS -X POST "$BOOKING/api/v1/bookings/$BOOKING_ID/confirm" >/dev/null
ok "confirmed"

# ── 3. Wait for saga to settle ────────────────────────────────────────────────
if $FAIL_MODE; then
  EXPECTED="CANCELLED"
else
  EXPECTED="PAID"
fi

say "3. Waiting for booking to reach $EXPECTED …"
SETTLED=false
for i in $(seq 1 30); do
  STATUS=$(curl -fsS "$BOOKING/api/v1/bookings/$BOOKING_ID" | field status)
  printf "   [%2d] status = %s\n" "$i" "$STATUS"
  if [[ "$STATUS" == "$EXPECTED" ]]; then
    SETTLED=true
    break
  fi
  sleep 1
done

$SETTLED || { warn "Booking did not reach $EXPECTED within 30 s"; exit 1; }
ok "Booking is $EXPECTED"

# ── 4. Read payment record ────────────────────────────────────────────────────
say "4. Payment record in payment-service"
PAYMENT_RESP=$(curl -fsS "$PAYMENT/api/v1/payments?bookingId=$BOOKING_ID")
echo "$PAYMENT_RESP" | json
if $FAIL_MODE; then
  PSTATUS=$(printf '%s' "$PAYMENT_RESP" | python3 -c \
    'import sys,json; r=json.load(sys.stdin); print(r[0]["status"] if isinstance(r,list) else r.get("status","—"))')
  ok "Payment status = $PSTATUS"
fi

# ── 5. Read notifications ─────────────────────────────────────────────────────
say "5. Notifications in notification-service"
curl -fsS "$NOTIFY/api/v1/notifications?bookingId=$BOOKING_ID" | json

# ── 6. Final summary ──────────────────────────────────────────────────────────
say "Done ✔"
printf "\n"
printf "  \033[1mBooking ID:\033[0m  %s\n" "$BOOKING_ID"
printf "  \033[1mFinal status:\033[0m %s\n" "$EXPECTED"
printf "\n"
printf "  \033[1;36mAPI endpoints\033[0m\n"
printf "    Booking:   %s/api/v1/bookings/%s\n"  "$BOOKING" "$BOOKING_ID"
printf "    Payments:  %s/api/v1/payments?bookingId=%s\n" "$PAYMENT" "$BOOKING_ID"
printf "    Notify:    %s/api/v1/notifications?bookingId=%s\n" "$NOTIFY" "$BOOKING_ID"
printf "\n"
printf "  \033[1;36mDashboards\033[0m\n"
printf "    React UI:    http://localhost:3001\n"
printf "    Grafana:     http://localhost:3000  (user: admin / admin)\n"
printf "    Prometheus:  http://localhost:9090\n"
printf "\n"
printf "  \033[1;36mSwagger UIs\033[0m\n"
printf "    Booking:      http://localhost:8081/swagger-ui.html\n"
printf "    Payment:      http://localhost:8082/swagger-ui.html\n"
printf "    Notification: http://localhost:8083/swagger-ui.html\n"
