const ok = async (res) => {
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText)
    throw new Error(`${res.status}: ${text}`)
  }
  return res.json()
}

export const createBooking = (body) =>
  fetch('/api/v1/bookings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then(ok)

export const confirmBooking = (id) =>
  fetch(`/api/v1/bookings/${id}/confirm`, { method: 'POST' }).then(ok)

export const cancelBooking = (id, reason) =>
  fetch(`/api/v1/bookings/${id}/cancel`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  }).then(ok)

export const getBooking = (id) =>
  fetch(`/api/v1/bookings/${id}`).then(ok)

export const getAllBookings = () =>
  fetch('/api/v1/bookings').then(ok)

export const getPayments = (bookingId) =>
  fetch(`/api/v1/payments?bookingId=${bookingId}`).then(ok)

export const getNotifications = (bookingId) =>
  fetch(`/api/v1/notifications?bookingId=${bookingId}`).then(ok)
