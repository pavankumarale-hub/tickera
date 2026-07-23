import { useState, useEffect, useCallback, useRef, createContext, useContext } from 'react'
import {
  Ticket, CreditCard, Bell, LayoutDashboard, Plus, X, Check,
  Clock, CheckCircle2, XCircle, AlertCircle, Info,
  Activity, Hash, Users, DollarSign, Zap, ChevronRight,
  Loader2, RefreshCw, ArrowUpRight, TrendingUp,
} from 'lucide-react'
import {
  createBooking, confirmBooking, cancelBooking,
  getAllBookings, getPayments, getNotifications,
} from './api.js'

/* ─────────────────────────────────── constants ───────────────────────────── */

const TERMINAL = new Set(['PAID', 'CANCELLED'])
const POLL_MS  = 2500

const STATUS_META = {
  CREATED:   { label: 'Created',   dotClass: 'dot-CREATED',   badgeClass: 'status-CREATED' },
  CONFIRMED: { label: 'Confirmed', dotClass: 'dot-CONFIRMED',  badgeClass: 'status-CONFIRMED' },
  PAID:      { label: 'Paid',      dotClass: 'dot-PAID',       badgeClass: 'status-PAID' },
  CANCELLED: { label: 'Cancelled', dotClass: 'dot-CANCELLED',  badgeClass: 'status-CANCELLED' },
}

/* ─────────────────────────────────── utils ───────────────────────────────── */

function timeAgo(val) {
  if (!val) return '—'
  const s = Math.floor((Date.now() - new Date(val).getTime()) / 1000)
  if (s < 5)    return 'just now'
  if (s < 60)   return `${s}s ago`
  if (s < 3600) return `${Math.floor(s / 60)}m ago`
  if (s < 86400)return `${Math.floor(s / 3600)}h ago`
  return new Date(val).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

function fmtDate(val) {
  if (!val) return '—'
  return new Date(val).toLocaleString('en-US', {
    month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function fmtMoney(amount, currency = 'USD') {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount ?? 0)
}

function shortId(id) {
  return id ? id.slice(0, 8).toUpperCase() : '—'
}

/* ─────────────────────────────────── toast system ────────────────────────── */

const ToastCtx = createContext(null)

function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const idRef = useRef(0)

  const push = useCallback((type, title, desc) => {
    const id = ++idRef.current
    setToasts(t => [...t, { id, type, title, desc }])
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 4200)
  }, [])

  const dismiss = useCallback((id) => setToasts(t => t.filter(x => x.id !== id)), [])

  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div className="toast-container">
        {toasts.map(t => (
          <Toast key={t.id} toast={t} onDismiss={() => dismiss(t.id)} />
        ))}
      </div>
    </ToastCtx.Provider>
  )
}

function Toast({ toast, onDismiss }) {
  const Icon = toast.type === 'success' ? CheckCircle2
             : toast.type === 'error'   ? AlertCircle
             : Info
  return (
    <div className={`toast toast-${toast.type}`}>
      <Icon size={16} className="toast-icon" />
      <div className="toast-body">
        <div className="toast-title">{toast.title}</div>
        {toast.desc && <div className="toast-desc">{toast.desc}</div>}
      </div>
      <button className="btn btn-icon" style={{ width: 22, height: 22 }} onClick={onDismiss}>
        <X size={12} />
      </button>
    </div>
  )
}

function useToast() { return useContext(ToastCtx) }

/* ─────────────────────────────────── status badge ────────────────────────── */

function StatusBadge({ status, size = 'md' }) {
  const meta = STATUS_META[status] ?? { label: status, dotClass: '', badgeClass: '' }
  return (
    <span className={`status-badge ${meta.badgeClass}`}>
      <span className={`status-dot ${meta.dotClass}`} />
      {meta.label}
    </span>
  )
}

/* ─────────────────────────────────── skeleton ────────────────────────────── */

function SkeletonRows({ n = 4 }) {
  return Array.from({ length: n }, (_, i) => (
    <div key={i} className="skel-row">
      {[90, 0, 110, 40, 80, 55, 80].map((w, j) => (
        <div key={j} className="skel-cell">
          <div
            className="skeleton skel-bar"
            style={{ width: w || '85%', opacity: 1 - i * 0.12 }}
          />
        </div>
      ))}
    </div>
  ))
}

/* ─────────────────────────────────── stats ───────────────────────────────── */

function StatCard({ label, value, sub, icon: Icon, iconBg, iconColor }) {
  return (
    <div className="stat-card">
      <div className="stat-card-header">
        <span className="stat-label">{label}</span>
        <div className="stat-icon" style={{ background: iconBg }}>
          <Icon size={14} color={iconColor} />
        </div>
      </div>
      <div className="stat-value">{value}</div>
      {sub && <div className="stat-sub">{sub}</div>}
    </div>
  )
}

function StatsRow({ bookings }) {
  const byStatus = (s) => bookings.filter(b => b.status === s).length
  const paidRev  = bookings
    .filter(b => b.status === 'PAID')
    .reduce((sum, b) => sum + Number(b.amount ?? 0), 0)

  return (
    <div className="stats-grid">
      <StatCard
        label="Total Bookings"
        value={bookings.length}
        sub={`${byStatus('CREATED') + byStatus('CONFIRMED')} in progress`}
        icon={Ticket}
        iconBg="rgba(99,102,241,0.12)"
        iconColor="var(--a2)"
      />
      <StatCard
        label="Awaiting"
        value={byStatus('CREATED')}
        sub="need confirmation"
        icon={Clock}
        iconBg="rgba(59,130,246,0.12)"
        iconColor="var(--created-c)"
      />
      <StatCard
        label="Revenue"
        value={fmtMoney(paidRev)}
        sub={`${byStatus('PAID')} paid bookings`}
        icon={TrendingUp}
        iconBg="rgba(16,185,129,0.12)"
        iconColor="var(--paid-c)"
      />
      <StatCard
        label="Cancelled"
        value={byStatus('CANCELLED')}
        sub={bookings.length ? `${Math.round(byStatus('CANCELLED') / bookings.length * 100)}% of total` : '—'}
        icon={XCircle}
        iconBg="rgba(113,113,122,0.10)"
        iconColor="var(--cancelled-c)"
      />
    </div>
  )
}

/* ─────────────────────────────────── timeline ─────────────────────────────── */

const TL_STEPS = {
  CREATED:   ['created', 'pending_confirm'],
  CONFIRMED: ['created', 'confirmed', 'processing'],
  PAID:      ['created', 'confirmed', 'paid'],
  CANCELLED: ['created', 'cancelled'],
}

const TL_META = {
  created:         { label: 'Booking created',       icon: Ticket },
  confirmed:       { label: 'Seats confirmed',        icon: CheckCircle2 },
  processing:      { label: 'Processing payment',     icon: Loader2, spinning: true },
  paid:            { label: 'Payment successful',     icon: CheckCircle2 },
  pending_confirm: { label: 'Awaiting confirmation',  icon: Clock, pending: true },
  cancelled:       { label: 'Booking cancelled',      icon: XCircle, cancelled: true },
}

function Timeline({ booking }) {
  const steps = TL_STEPS[booking.status] ?? ['created']
  const updAt = booking.updatedAt

  return (
    <div className="timeline">
      {steps.map((key, idx) => {
        const meta     = TL_META[key]
        const isLast   = idx === steps.length - 1
        const showTime = isLast && updAt
        const isPend   = meta.pending
        const isSpin   = meta.spinning
        const isCanc   = meta.cancelled

        return (
          <div key={key} className="tl-item">
            <div className="tl-dot-wrap">
              {isSpin ? (
                <span
                  className="tl-dot spinning"
                  style={{ borderTopColor: 'var(--confirmed-c)' }}
                />
              ) : (
                <span
                  className={`tl-dot ${isPend ? 'pending' : ''}`}
                  style={!isPend ? {
                    background: isCanc ? 'var(--cancelled-c)'
                      : key === 'paid' ? 'var(--paid-c)'
                      : key === 'confirmed' ? 'var(--confirmed-c)'
                      : 'var(--a2)',
                  } : {}}
                />
              )}
            </div>
            <div className="tl-content">
              <div className={`tl-title ${isPend || isSpin ? 'muted' : ''}`}
                style={isCanc ? { color: 'var(--cancelled-c)' } : {}}>
                {meta.label}
                {isSpin && (
                  <span style={{ marginLeft: 6, display: 'inline-block' }}>
                    <span className="spinner-sm" />
                  </span>
                )}
              </div>
              {showTime && <div className="tl-time">{fmtDate(updAt)}</div>}
              {!showTime && !isPend && !isSpin && (
                <div className="tl-time">—</div>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}

/* ─────────────────────────────────── detail panel ────────────────────────── */

const PAYMENT_META = {
  APPROVED: { color: 'var(--paid-c)',  label: 'Approved' },
  DECLINED: { color: 'var(--danger)',  label: 'Declined' },
}

function DetailPanel({ booking, onClose, onAction }) {
  const toast   = useToast()
  const [payments, setPayments]         = useState(null)
  const [notifications, setNotifications] = useState(null)
  const [acting, setActing]             = useState(null) // 'confirm' | 'cancel'
  const prevStatusRef = useRef(booking?.status)

  const load = useCallback(async (id) => {
    const [p, n] = await Promise.allSettled([
      getPayments(id),
      getNotifications(id),
    ])
    if (p.status === 'fulfilled') setPayments(Array.isArray(p.value) ? p.value : [])
    if (n.status === 'fulfilled') setNotifications(Array.isArray(n.value) ? n.value : [])
  }, [])

  useEffect(() => {
    if (!booking) return
    setPayments(null)
    setNotifications(null)
    load(booking.bookingId)
  }, [booking?.bookingId, load])

  // reload details when status changes (booking updated by polling)
  useEffect(() => {
    if (!booking) return
    if (prevStatusRef.current !== booking.status) {
      prevStatusRef.current = booking.status
      load(booking.bookingId)
    }
  }, [booking?.status, booking?.bookingId, load])

  const act = async (type, fn, successMsg) => {
    setActing(type)
    try {
      await fn()
      toast('success', successMsg)
      onAction()
    } catch (err) {
      toast('error', 'Action failed', err.message)
    } finally {
      setActing(null)
    }
  }

  if (!booking) return null

  const pm = PAYMENT_META[payments?.[0]?.status]

  return (
    <div className="panel-inner">
      {/* Header */}
      <div className="panel-topbar">
        <span className="panel-topbar-label">Booking details</span>
        <button className="btn btn-icon" onClick={onClose} aria-label="Close">
          <X size={14} />
        </button>
      </div>

      <div className="panel-body">
        {/* Hero */}
        <div className="panel-hero">
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 8 }}>
            <div className="panel-event-name">{booking.eventName}</div>
            <StatusBadge status={booking.status} />
          </div>

          <div className="panel-amount">{fmtMoney(booking.amount, booking.currency)}</div>

          <div className="panel-meta-row">
            <div className="panel-meta-item">
              <Users size={12} />
              <span>{booking.customerId}</span>
            </div>
            <div className="panel-meta-item">
              <Ticket size={12} />
              <span>{booking.seats} seat{booking.seats !== 1 ? 's' : ''}</span>
            </div>
            <div className="panel-meta-item">
              <Hash size={12} />
              <span className="mono" style={{ fontSize: 11 }}>{shortId(booking.bookingId)}</span>
            </div>
          </div>
        </div>

        {/* Actions */}
        {booking.status === 'CREATED' && (
          <>
            <div className="panel-divider" />
            <div className="panel-actions">
              <button
                className="btn btn-confirm"
                style={{ flex: 1 }}
                disabled={!!acting}
                onClick={() => act('confirm',
                  () => confirmBooking(booking.bookingId),
                  'Booking confirmed — payment processing'
                )}
              >
                {acting === 'confirm' ? <span className="spinner-sm" /> : <Check size={13} />}
                Confirm booking
              </button>
              <button
                className="btn btn-danger"
                style={{ flex: 1 }}
                disabled={!!acting}
                onClick={() => act('cancel',
                  () => cancelBooking(booking.bookingId, 'Cancelled by user'),
                  'Booking cancelled'
                )}
              >
                {acting === 'cancel' ? <span className="spinner-sm" /> : <X size={13} />}
                Cancel
              </button>
            </div>
          </>
        )}

        <div className="panel-divider" />

        {/* Timeline */}
        <div className="panel-section">
          <div className="panel-section-label">
            <Activity size={11} />
            Timeline
          </div>
          <Timeline booking={booking} />
        </div>

        <div className="panel-divider" />

        {/* Payment */}
        <div className="panel-section">
          <div className="panel-section-label">
            <CreditCard size={11} />
            Payment
          </div>

          {payments === null ? (
            <div className="inline-loading"><span className="spinner-sm" /> Loading…</div>
          ) : payments.length === 0 ? (
            <div style={{ fontSize: 12, color: 'var(--t4)', padding: '4px 0' }}>
              No payment record yet
            </div>
          ) : payments.map(p => (
            <div key={p.paymentId} className="payment-card">
              <div className="payment-card-header">
                <span className="payment-card-id">
                  {p.paymentId ? shortId(p.paymentId) : '—'}
                </span>
                {pm && (
                  <span
                    className="status-badge"
                    style={{ color: pm.color, background: `${pm.color}14`, borderColor: `${pm.color}30` }}
                  >
                    <span className="status-dot" style={{ background: pm.color }} />
                    {pm.label}
                  </span>
                )}
              </div>
              <div className="payment-card-amount">
                {fmtMoney(p.amount, p.currency)}
              </div>
              {p.reason && (
                <div className="payment-reason">
                  <AlertCircle size={12} />
                  {p.reason}
                </div>
              )}
              {p.createdAt && (
                <div style={{ fontSize: 11, color: 'var(--t4)', marginTop: 2 }}>
                  {fmtDate(p.createdAt)}
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="panel-divider" />

        {/* Notifications */}
        <div className="panel-section">
          <div className="panel-section-label">
            <Bell size={11} />
            Notifications
          </div>
          {notifications === null ? (
            <div className="inline-loading"><span className="spinner-sm" /> Loading…</div>
          ) : notifications.length === 0 ? (
            <div style={{ fontSize: 12, color: 'var(--t4)', padding: '4px 0' }}>
              No notifications sent
            </div>
          ) : notifications.map(n => (
            <div key={n.id} className="notif-item">
              <div className="notif-channel">
                <Zap size={9} />
                {n.channel ?? 'system'}
              </div>
              <div className="notif-message">{n.message}</div>
              {n.createdAt && (
                <div className="notif-time">{fmtDate(n.createdAt)}</div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

/* ─────────────────────────────────── create modal ─────────────────────────── */

const BLANK = { customerId: '', eventName: '', seats: '2', amount: '', currency: 'USD' }

function CreateModal({ onClose, onCreated }) {
  const toast  = useToast()
  const [form, setForm]     = useState(BLANK)
  const [loading, setLoading] = useState(false)
  const [error, setError]   = useState(null)
  const firstRef = useRef(null)

  useEffect(() => { firstRef.current?.focus() }, [])

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }))

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
      toast('success', 'Booking created', `${form.eventName} · ${fmtMoney(form.amount, form.currency)}`)
      setForm(BLANK)
      onCreated(b)
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const onBackdrop = e => { if (e.target === e.currentTarget) onClose() }

  return (
    <div className="modal-backdrop" onClick={onBackdrop}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div className="modal-header">
          <div>
            <div className="modal-title" id="modal-title">New Booking</div>
            <div className="modal-subtitle">Reserve seats for an event</div>
          </div>
          <button className="btn btn-icon" onClick={onClose} aria-label="Close">
            <X size={14} />
          </button>
        </div>

        <form onSubmit={submit}>
          <div className="modal-body">
            {error && (
              <div className="error-bar">
                <AlertCircle size={14} style={{ flexShrink: 0, marginTop: 1 }} />
                {error}
              </div>
            )}

            <div className="field">
              <label>Customer ID</label>
              <input
                ref={firstRef}
                required
                placeholder="e.g. customer-001"
                value={form.customerId}
                onChange={set('customerId')}
              />
            </div>

            <div className="field">
              <label>Event Name</label>
              <input
                required
                placeholder="e.g. Rock Concert 2025"
                value={form.eventName}
                onChange={set('eventName')}
              />
            </div>

            <div className="field-row">
              <div className="field">
                <label>Seats</label>
                <input
                  required type="number" min="1" max="100"
                  value={form.seats}
                  onChange={set('seats')}
                />
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
                value={form.amount}
                onChange={set('amount')}
              />
              <span className="field-hint">
                Amounts over $1,000 are auto-declined by the payment simulator
              </span>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? <span className="spinner" /> : <Plus size={14} />}
              Create booking
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

/* ─────────────────────────────────── booking row ──────────────────────────── */

function BookingRow({ booking, selected, onClick, onAction }) {
  const toast   = useToast()
  const [acting, setActing] = useState(null)

  const act = async (e, type, fn, msg) => {
    e.stopPropagation()
    setActing(type)
    try {
      await fn()
      toast('success', msg)
      onAction()
    } catch (err) {
      toast('error', 'Failed', err.message)
    } finally {
      setActing(null)
    }
  }

  const meta = STATUS_META[booking.status] ?? {}

  return (
    <div
      className={`list-row row-grid ${selected ? 'selected' : ''}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={e => e.key === 'Enter' && onClick()}
      aria-selected={selected}
    >
      {/* Status */}
      <div className="col">
        <StatusBadge status={booking.status} />
      </div>

      {/* Event */}
      <div className="col overflow" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: 1 }}>
        <div className="event-name">{booking.eventName}</div>
        <div className="booking-id">{shortId(booking.bookingId)}</div>
      </div>

      {/* Customer */}
      <div className="col overflow">
        <div className="customer-id">{booking.customerId}</div>
      </div>

      {/* Seats */}
      <div className="col right">
        <span className="seats-val">{booking.seats}</span>
      </div>

      {/* Amount */}
      <div className="col right">
        <span className="amount-val">{fmtMoney(booking.amount, booking.currency)}</span>
      </div>

      {/* Updated */}
      <div className="col right">
        <span className="time-val">{timeAgo(booking.updatedAt)}</span>
      </div>

      {/* Actions */}
      <div className="col right actions">
        {booking.status === 'CREATED' && (
          <>
            <button
              className="btn btn-confirm"
              disabled={!!acting}
              onClick={e => act(e, 'confirm',
                () => confirmBooking(booking.bookingId),
                'Booking confirmed'
              )}
              aria-label="Confirm booking"
            >
              {acting === 'confirm' ? <span className="spinner-sm" /> : <Check size={11} />}
              Confirm
            </button>
            <button
              className="btn btn-danger"
              disabled={!!acting}
              onClick={e => act(e, 'cancel',
                () => cancelBooking(booking.bookingId, 'Cancelled by user'),
                'Booking cancelled'
              )}
              aria-label="Cancel booking"
            >
              {acting === 'cancel' ? <span className="spinner-sm" /> : null}
              Cancel
            </button>
          </>
        )}
        {!['CREATED'].includes(booking.status) && (
          <ChevronRight size={14} style={{ color: 'var(--t4)' }} />
        )}
      </div>
    </div>
  )
}

/* ─────────────────────────────────── sidebar ──────────────────────────────── */

function Sidebar({ pollActive }) {
  const navItems = [
    { icon: LayoutDashboard, label: 'Overview', active: false },
    { icon: Ticket,          label: 'Bookings',  active: true  },
    { icon: CreditCard,      label: 'Payments',  active: false },
    { icon: Bell,            label: 'Notifications', active: false },
  ]

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="logo-mark">
          <Ticket size={14} color="white" />
        </div>
        <span className="brand-name">
          Tick<span>era</span>
        </span>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-section-label">Main</div>
        {navItems.map(item => (
          <button key={item.label} className={`nav-item ${item.active ? 'active' : ''}`}>
            <item.icon size={15} />
            {item.label}
          </button>
        ))}
      </nav>

      <div className="sidebar-footer">
        {pollActive ? (
          <div className="live-pill">
            <span className="live-dot" />
            Live
          </div>
        ) : (
          <div className="live-pill" style={{
            background: 'var(--s3)',
            borderColor: 'var(--bd)',
            color: 'var(--t3)',
          }}>
            <span className="live-dot" style={{ background: 'var(--t4)', animation: 'none' }} />
            Idle
          </div>
        )}
      </div>
    </aside>
  )
}

/* ─────────────────────────────────── app ──────────────────────────────────── */

export default function App() {
  const [bookings, setBookings]     = useState([])
  const [loading, setLoading]       = useState(true)
  const [selectedId, setSelectedId] = useState(null)
  const [showCreate, setShowCreate] = useState(false)
  const timerRef = useRef(null)

  const fetchAll = useCallback(async () => {
    try {
      const data = await getAllBookings()
      setBookings(prev => {
        const next = Array.isArray(data) ? data : []
        // sort by updatedAt desc, fall back to bookingId
        next.sort((a, b) => {
          const ta = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
          const tb = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
          return tb - ta
        })
        return next
      })
    } catch {
      // keep stale data on transient errors
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchAll() }, [fetchAll])

  const hasActive = bookings.some(b => !TERMINAL.has(b.status))

  useEffect(() => {
    clearInterval(timerRef.current)
    if (hasActive) {
      timerRef.current = setInterval(fetchAll, POLL_MS)
    }
    return () => clearInterval(timerRef.current)
  }, [hasActive, fetchAll])

  const handleCreated = (booking) => {
    setBookings(prev => [booking, ...prev])
    setSelectedId(booking.bookingId)
    setTimeout(fetchAll, 800)
  }

  const selectedBooking = bookings.find(b => b.bookingId === selectedId) ?? null

  return (
    <ToastProvider>
      <div className="app-shell">
        <Sidebar pollActive={hasActive} />

        <div className="main-area">
          {/* Top bar */}
          <div className="topbar">
            <div className="topbar-left">
              <span className="topbar-title">Bookings</span>
              {!loading && (
                <span className="topbar-count">{bookings.length}</span>
              )}
              {hasActive && (
                <div className="polling-badge">
                  <span className="spinner-sm" />
                  Live
                </div>
              )}
            </div>
            <div className="topbar-right">
              <button
                className="btn btn-ghost btn-icon"
                onClick={fetchAll}
                aria-label="Refresh"
                title="Refresh"
              >
                <RefreshCw size={13} />
              </button>
              <button
                className="btn btn-primary"
                onClick={() => setShowCreate(true)}
              >
                <Plus size={14} />
                New Booking
              </button>
            </div>
          </div>

          {/* Body: content + panel */}
          <div className="body-wrap">
            {/* Scrollable content */}
            <div className="page-content">
              <StatsRow bookings={bookings} />

              <div className="section">
                <div className="section-header">
                  <span className="section-title">
                    <Ticket size={14} />
                    All Bookings
                  </span>
                </div>

                {/* Table header */}
                <div className="list-header row-grid">
                  {['Status', 'Event', 'Customer', 'Seats', 'Amount', 'Updated', ''].map(h => (
                    <div key={h} className={`col${h === 'Seats' || h === 'Amount' || h === 'Updated' ? ' right' : ''}`}>
                      {h}
                    </div>
                  ))}
                </div>

                {/* Table body */}
                <div className="list-body">
                  {loading ? (
                    <SkeletonRows n={5} />
                  ) : bookings.length === 0 ? (
                    <div className="empty-state">
                      <div className="empty-icon"><Ticket size={22} /></div>
                      <div className="empty-title">No bookings yet</div>
                      <div className="empty-desc">
                        Create your first booking to see it appear here in real time.
                      </div>
                      <button
                        className="btn btn-primary"
                        style={{ marginTop: 8 }}
                        onClick={() => setShowCreate(true)}
                      >
                        <Plus size={14} /> New Booking
                      </button>
                    </div>
                  ) : (
                    bookings.map(b => (
                      <BookingRow
                        key={b.bookingId}
                        booking={b}
                        selected={b.bookingId === selectedId}
                        onClick={() => setSelectedId(
                          prev => prev === b.bookingId ? null : b.bookingId
                        )}
                        onAction={fetchAll}
                      />
                    ))
                  )}
                </div>
              </div>
            </div>

            {/* Detail panel */}
            <div className={`detail-panel ${selectedBooking ? 'open' : ''}`}>
              <DetailPanel
                booking={selectedBooking}
                onClose={() => setSelectedId(null)}
                onAction={fetchAll}
              />
            </div>
          </div>
        </div>
      </div>

      {showCreate && (
        <CreateModal
          onClose={() => setShowCreate(false)}
          onCreated={handleCreated}
        />
      )}
    </ToastProvider>
  )
}
