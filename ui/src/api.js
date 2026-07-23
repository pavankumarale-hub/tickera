const ok = async (res) => {
  if (!res.ok) {
    const body = await res.text().catch(() => res.statusText)
    // try to extract Spring error message
    try {
      const j = JSON.parse(body)
      throw new Error(j.message ?? j.error ?? body)
    } catch (e) {
      if (e.message !== body) throw e
      throw new Error(body || `HTTP ${res.status}`)
    }
  }
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

// Booking endpoints (booking-service :8081)
export const createBooking  = (body) =>
  fetch('/api/v1/bookings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(ok)

export const confirmBooking = (id) =>
  fetch(`/api/v1/bookings/${id}/confirm`, { method: 'POST' }).then(ok)

export const cancelBooking  = (id, reason) =>
  fetch(`/api/v1/bookings/${id}/cancel`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  }).then(ok)

export const getBooking     = (id) =>
  fetch(`/api/v1/bookings/${id}`).then(ok)

// Returns List<BookingResponse>: [{bookingId, customerId, eventName, seats, amount, currency, status, paymentId, updatedAt}]
export const getAllBookings  = () =>
  fetch('/api/v1/bookings').then(ok)

// Payment endpoint (payment-service :8082)
// Returns List<PaymentResponse>: [{paymentId, bookingId, amount, currency, status (APPROVED|DECLINED), reason, createdAt}]
export const getPayments    = (bookingId) =>
  fetch(`/api/v1/payments?bookingId=${encodeURIComponent(bookingId)}`).then(ok)

// Notification endpoint (notification-service :8083)
// Returns List<Notification>: [{id, bookingId, channel, message, createdAt}]
export const getNotifications = (bookingId) =>
  fetch(`/api/v1/notifications?bookingId=${encodeURIComponent(bookingId)}`).then(ok)
