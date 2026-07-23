#!/usr/bin/env bash
#
# Seed the Tickera system with a rich, realistic dataset for UI demonstration.
#
# Creates 9 bookings spanning all four statuses:
#   PAID       — 4 confirmed bookings, amount ≤ $1 000 (happy path)
#   CANCELLED  — 2 confirmed bookings, amount  > $1 000 (payment declined)
#   CREATED    — 3 bookings left unconfirmed (pending queue)
#
# Usage:  ./scripts/seed.sh
#         make seed
#
set -euo pipefail

BOOKING=http://localhost:8081

# ── helpers ───────────────────────────────────────────────────────────────────
say()  { printf "\n\033[1;36m▶ %s\033[0m\n" "$1"; }
ok()   { printf "  \033[1;32m✔\033[0m %s\n" "$1"; }
warn() { printf "  \033[1;33m⚠\033[0m %s\n" "$1"; }
step() { printf "    %s" "$1"; }
done_() { printf " \033[32m✓\033[0m\n"; }

# Create a booking and print its ID
create() {
  local customer="$1" event="$2" seats="$3" amount="$4" currency="${5:-USD}"
  curl -fsS -X POST "$BOOKING/api/v1/bookings" \
    -H 'Content-Type: application/json' \
    -d "{
      \"customerId\": \"$customer\",
      \"eventName\":  \"$event\",
      \"seats\":       $seats,
      \"amount\":      $amount,
      \"currency\":   \"$currency\"
    }" | python3 -c 'import sys,json; print(json.load(sys.stdin)["bookingId"])'
}

confirm_booking() {
  curl -fsS -X POST "$BOOKING/api/v1/bookings/$1/confirm" >/dev/null
}

# Poll until booking reaches expected status (or 25 s timeout)
wait_for() {
  local id="$1" expected="$2"
  for _ in $(seq 1 25); do
    local status
    status=$(curl -fsS "$BOOKING/api/v1/bookings/$id" \
              | python3 -c 'import sys,json; print(json.load(sys.stdin)["status"])')
    if [[ "$status" == "$expected" ]]; then
      return 0
    fi
    sleep 1
  done
  warn "Booking $id did not reach $expected within 25 s"
  return 1
}

# ── 0. Health check ───────────────────────────────────────────────────────────
say "0. Checking booking-service health"
for i in $(seq 1 30); do
  if curl -fsS "$BOOKING/actuator/health" 2>/dev/null \
       | python3 -c 'import sys,json; s=json.load(sys.stdin)["status"]; exit(0 if s=="UP" else 1)' \
       2>/dev/null; then
    ok "booking-service is UP"
    break
  fi
  [[ $i -eq 30 ]] && { warn "Service not ready after 30 s — is docker compose up running?"; exit 1; }
  sleep 1
done

# ── 1. Happy-path bookings (will end up PAID) ─────────────────────────────────
say "1. Creating happy-path bookings (amount ≤ \$1 000 → PAID)"

step "Taylor Swift: Eras Tour      (alice,  2 seats, \$349) "
B1=$(create "alice"  "Taylor Swift: Eras Tour"    2  349.00  "USD"); done_

step "NBA Finals – Game 7          (bob,    4 seats, \$890) "
B2=$(create "bob"    "NBA Finals – Game 7"        4  890.00  "USD"); done_

step "Hamilton: Broadway Revival   (carol,  2 seats, \$199) "
B3=$(create "carol"  "Hamilton: Broadway Revival" 2  199.50  "USD"); done_

step "Coldplay World Tour          (dave,   3 seats, \$149) "
B4=$(create "dave"   "Coldplay World Tour"        3  149.00  "USD"); done_

say "1b. Confirming all four …"
for id in "$B1" "$B2" "$B3" "$B4"; do
  step "$id "
  confirm_booking "$id"; done_
done

say "1c. Waiting for payment settlement …"
for id in "$B1" "$B2" "$B3" "$B4"; do
  step "$id → PAID "
  if wait_for "$id" "PAID"; then done_; else printf " ✗ (timeout)\n"; fi
done

# ── 2. Failure-path bookings (amount > $1 000 → DECLINED → CANCELLED) ─────────
say "2. Creating failure-path bookings (amount > \$1 000 → CANCELLED)"

step "Super Bowl LX VIP Package    (eve,    6 seats, \$1 250) "
B5=$(create "eve"   "Super Bowl LX VIP Package"    6 1250.00  "USD"); done_

step "F1 Monaco GP – Paddock Club  (frank,  2 seats, \$2 500) "
B6=$(create "frank" "F1 Monaco GP – Paddock Club"  2 2500.00  "EUR"); done_

say "2b. Confirming both …"
for id in "$B5" "$B6"; do
  step "$id "
  confirm_booking "$id"; done_
done

say "2c. Waiting for cancellation …"
for id in "$B5" "$B6"; do
  step "$id → CANCELLED "
  if wait_for "$id" "CANCELLED"; then done_; else printf " ✗ (timeout)\n"; fi
done

# ── 3. Pending bookings (left in CREATED state) ───────────────────────────────
say "3. Creating pending bookings (no confirmation → CREATED)"

step "Cirque du Soleil: O          (grace,  3 seats,  \$95) "
B7=$(create "grace" "Cirque du Soleil: O"         3   95.00  "USD"); done_

step "Coachella Weekend Pass       (henry,  1 seat,  \$499) "
B8=$(create "henry" "Coachella Weekend Pass"      1  499.00  "USD"); done_

step "Champions League Final       (ivan,   2 seats, \$650) "
B9=$(create "ivan"  "Champions League Final"      2  650.00  "EUR"); done_

# ── Summary ───────────────────────────────────────────────────────────────────
say "Done ✔ — 9 bookings seeded"
printf "\n"
printf "  \033[32m%-12s\033[0m %s\n" "PAID (4):"      "Eras Tour · NBA Finals · Hamilton · Coldplay"
printf "  \033[31m%-12s\033[0m %s\n" "CANCELLED (2):" "Super Bowl VIP · F1 Monaco Paddock"
printf "  \033[34m%-12s\033[0m %s\n" "CREATED (3):"   "Cirque du Soleil · Coachella · Champions League"
printf "\n"
printf "  Dashboard:   http://localhost:3001\n"
printf "  Grafana:     http://localhost:3000\n"
printf "\n"
