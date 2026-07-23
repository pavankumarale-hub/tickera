#!/usr/bin/env bash
#
# End-to-end happy-path demo. Assumes `docker compose up` is healthy.
#
#   Booking (CREATED) --confirm--> BookingConfirmed --Kafka--> Payment
#        ^                                                        |
#        |________________ PaymentCompleted <--Kafka-------------/
#
set -euo pipefail

BOOKING=http://localhost:8081
PAYMENT=http://localhost:8082
NOTIFY=http://localhost:8083

say() { printf "\n\033[1;36m▶ %s\033[0m\n" "$1"; }

say "1. Create a booking (lands in CREATED)"
CREATE_RESP=$(curl -fsS -X POST "$BOOKING/api/v1/bookings" \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"cust-42","eventName":"Symphony Gala","seats":3,"amount":240.00,"currency":"USD"}')
echo "$CREATE_RESP"
BOOKING_ID=$(printf '%s' "$CREATE_RESP" | python3 -c 'import sys,json;print(json.load(sys.stdin)["bookingId"])')
say "   bookingId = $BOOKING_ID"

say "2. Confirm the booking (emits BookingConfirmed -> starts the payment saga)"
curl -fsS -X POST "$BOOKING/api/v1/bookings/$BOOKING_ID/confirm" >/dev/null
echo "   confirmed."

say "3. Wait for the saga to settle (payment-service charges, booking-service marks PAID)"
for i in $(seq 1 20); do
  STATUS=$(curl -fsS "$BOOKING/api/v1/bookings/$BOOKING_ID" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin)["status"])')
  echo "   [$i] booking status = $STATUS"
  [ "$STATUS" = "PAID" ] && break
  sleep 1
done

say "4. Payment recorded in payment-service"
curl -fsS "$PAYMENT/api/v1/payments?bookingId=$BOOKING_ID"; echo

say "5. Notifications materialised from the event stream"
curl -fsS "$NOTIFY/api/v1/notifications?bookingId=$BOOKING_ID"; echo

say "Done. Open Swagger UIs: :8081 / :8082 / :8083 /swagger-ui.html  •  Grafana :3000"
