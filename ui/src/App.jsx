import { useState, useEffect, useCallback, useRef } from 'react'
import {
  createBooking, confirmBooking, cancelBooking,
  getAllBookings, getPayments, getNotifications,
} from './api.js'

const POLL_MS = 2500
const TERMINAL = new Set(['PAID', 'CANCELLED'])

const STATUS_ICON = {
  CREATED:   '🎫',
  CONFIRMED: '⏳',
  PAID:      '✅',
  CANCELLED: '❌',
}

function Badge({ status }) {
  return (
    <span className={`badge badge-${status}`}>
      {STATUS_ICON[status] ?? '•'} {status}
    </span>
  )
}

function BookingCard({ booking, onAction }) {
  const [payments, setPayments] = useState(null)
  const [notifications, setNotifications] = useState(null)
  const [expanded, setExpanded] = useState(false)
  const [acting, setActing] = useState(false)

  const isActive    = !TERMINAL.has(booking.status)
  const cardClass   = ['booking-card',
    booking.status === 'CONFIRMED' ? 'is-active' : '',
    booking.status === 'PAID'      ? 'is-paid'   : '',
    booking.status === 'CANCELLED' ? 'is-cancelled' : '',
  ].filter(Boolean).join(' ')

  const loadDetails = useCallback(async () => {
    const [p, n] = await Promise.allSettled([
      getPayments(booking.bookingId),
      getNotifications(booking.bookingId),
    ])
    if (p.status === 'fulfilled') setPayments(p.value)
    if (n.status === 'fulfilled') setNotifications(n.value)
  }, [booking.bookingId])

  const toggleExpanded = () => {
    if (!expanded) loadDetails()
    setExpanded(v => !v)
  }

  const act = async (fn) => {
    setActing(true)
    try { await fn() } finally { setActing(false) }
    onAction()
    loadDetails()
  }

  const fmt = (amt, cur) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: cur ?? 'USD' }).format(amt)

  return (
    <div className={cardClass}>
      <div className="bc-header">
        <div>
          <div className="bc-title">{booking.eventName}</div>
          <div className="bc-id">{booking.bookingId}</div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
          {isActive && <span className="pulse" title="Polling" />}
          <Badge status={booking.status} />
        </div>
      </div>

      <div className="bc-meta">
        <div className="bc-meta-item">
          <span className="label">Customer</span>
          <span className="value">{booking.customerId}</span>
        </div>
        <div className="bc-meta-item">
          <span className="label">Seats</span>
          <span className="value">{booking.seats}</span>
        </div>
        <div className="bc-meta-item">
          <span className="label">Amount</span>
          <span className="value">{fmt(booking.amount, booking.currency)}</span>
        </div>
      </div>

      {booking.status === 'CREATED' && (
        <div className="bc-actions">
          <button
            className="btn btn-confirm"
            disabled={acting}
            onClick={() => act(() => confirmBooking(booking.bookingId))}
          >
            {acting ? <span className="spinner-sm" /> : null} Confirm
          </button>
          <button
            className="btn btn-cancel"
            disabled={acting}
            onClick={() => act(() => cancelBooking(booking.bookingId, 'User cancelled'))}
          >
            Cancel
          </button>
        </div>
      )}

      <div className="divider" />
      <div className="refresh-bar" style={{ cursor: 'pointer' }} onClick={toggleExpanded}>
        <span>{expanded ? '▲' : '▼'}</span>
        <span>{expanded ? 'Hide' : 'Show'} payment & notifications</span>
      </div>

      {expanded && (
        <>
          <div className="bc-detail">
            <div className="bc-detail-title">Payment</div>
            {payments === null ? (
              <span className="spinner-sm" />
            ) : payments.length === 0 ? (
              <span style={{ color: 'var(--gray-400)', fontSize: '.8rem' }}>No payment yet</span>
            ) : payments.map((p) => (
              <div key={p.paymentId} className="payment-row">
                <Badge status={p.status} />
                <span className="amount">{fmt(p.amount, p.currency)}</span>
                {p.transactionId && (
                  <span style={{ color: 'var(--gray-400)', fontSize: '.75rem', fontFamily: 'monospace' }}>
                    txn: {p.transactionId}
                  </span>
                )}
                {p.declineReason && (
                  <span className="payment-declined">({p.declineReason})</span>
                )}
              </div>
            ))}
          </div>

          <div className="bc-detail">
            <div className="bc-detail-title">Notifications</div>
            {notifications === null ? (
              <span className="spinner-sm" />
            ) : notifications.length === 0 ? (
              <span style={{ color: 'var(--gray-400)', fontSize: '.8rem' }}>No notifications yet</span>
            ) : (
              <div className="notif-list">
                {notifications.map((n) => (
                  <div key={n.notificationId} className="notif-item">
                    <div>{n.message ?? n.type ?? 'Notification'}</div>
                    <div className="notif-time">{n.sentAt ?? n.createdAt ?? ''}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}

const BLANK = { customerId: '', eventName: '', seats: '1', amount: '', currency: 'USD' }

function BookingForm({ onCreated }) {
  const [form, setForm] = useState(BLANK)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const set = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const b = await createBooking({
        customerId: form.customerId,
        eventName:  form.eventName,
        seats:      Number(form.seats),
        amount:     parseFloat(form.amount),
        currency:   form.currency,
      })
      setForm(BLANK)
      onCreated(b)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <div className="card-header">🎟 New Booking</div>
      <div className="card-body">
        {error && <div className="error-banner">{error}</div>}
        <form className="form" onSubmit={submit}>
          <div className="field">
            <label>Customer ID</label>
            <input required placeholder="customer-001" value={form.customerId} onChange={set('customerId')} />
          </div>
          <div className="field">
            <label>Event Name</label>
            <input required placeholder="Rock Concert 2025" value={form.eventName} onChange={set('eventName')} />
          </div>
          <div className="row-2">
            <div className="field">
              <label>Seats</label>
              <input required type="number" min="1" max="100" value={form.seats} onChange={set('seats')} />
            </div>
            <div className="field">
              <label>Currency</label>
              <select value={form.currency} onChange={set('currency')}>
                <option>USD</option>
                <option>EUR</option>
                <option>GBP</option>
                <option>INR</option>
              </select>
            </div>
          </div>
          <div className="field">
            <label>Amount</label>
            <input
              required type="number" min="0.01" step="0.01"
              placeholder="499.00"
              value={form.amount} onChange={set('amount')}
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? <span className="spinner" /> : '🎫 Book Now'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default function App() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const timerRef = useRef(null)

  const fetchAll = useCallback(async () => {
    try {
      const data = await getAllBookings()
      setBookings(Array.isArray(data) ? data.sort((a, b) =>
        (b.createdAt ?? b.bookingId).localeCompare(a.createdAt ?? a.bookingId)
      ) : [])
    } catch {
      // keep stale data on transient errors
    } finally {
      setLoading(false)
    }
  }, [])

  const hasActive = bookings.some(b => !TERMINAL.has(b.status))

  useEffect(() => {
    fetchAll()
  }, [fetchAll])

  useEffect(() => {
    if (hasActive) {
      timerRef.current = setInterval(fetchAll, POLL_MS)
    }
    return () => clearInterval(timerRef.current)
  }, [hasActive, fetchAll])

  const handleCreated = (booking) => {
    setBookings(prev => [booking, ...prev])
    setTimeout(fetchAll, 600)
  }

  return (
    <div className="app">
      <header>
        <span className="logo">🎭</span>
        <h1>Tickera <span>Event Ticket Booking</span></h1>
      </header>

      <div className="layout">
        <aside>
          <BookingForm onCreated={handleCreated} />

          <div className="card" style={{ marginTop: 16 }}>
            <div className="card-header">📊 Stats</div>
            <div className="card-body">
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                {['CREATED','CONFIRMED','PAID','CANCELLED'].map(s => (
                  <div key={s} style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>
                      {bookings.filter(b => b.status === s).length}
                    </div>
                    <Badge status={s} />
                  </div>
                ))}
              </div>
            </div>
          </div>
        </aside>

        <main>
          <div className="card">
            <div className="card-header" style={{ justifyContent: 'space-between' }}>
              <span>📋 Bookings ({bookings.length})</span>
              {hasActive && (
                <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '.75rem', fontWeight: 400, color: 'var(--amber)' }}>
                  <span className="pulse" /> live
                </span>
              )}
            </div>

            {loading ? (
              <div className="card-body" style={{ textAlign: 'center', padding: 48 }}>
                <span className="spinner-sm" /> Loading…
              </div>
            ) : bookings.length === 0 ? (
              <div className="empty">
                <div className="empty-icon">🎭</div>
                <p>No bookings yet — create one to get started!</p>
              </div>
            ) : (
              <div className="card-body">
                <div className="booking-list">
                  {bookings.map(b => (
                    <BookingCard key={b.bookingId} booking={b} onAction={fetchAll} />
                  ))}
                </div>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  )
}
