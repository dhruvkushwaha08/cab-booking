import { useCallback, useEffect, useState } from 'react'
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMapEvents,
} from 'react-leaflet'
import L from 'leaflet'
import './App.css'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const pickupIcon = new L.DivIcon({
  className: '',
  html: '<div style="background:#16a34a;width:16px;height:16px;border:3px solid white;border-radius:50%;box-shadow:0 1px 6px #555"></div>',
  iconSize: [16, 16],
  iconAnchor: [8, 8],
})

const dropoffIcon = new L.DivIcon({
  className: '',
  html: '<div style="background:#2563eb;width:16px;height:16px;border:3px solid white;border-radius:50%;box-shadow:0 1px 6px #555"></div>',
  iconSize: [16, 16],
  iconAnchor: [8, 8],
})

function LocationPicker({ onPick }) {
  useMapEvents({
    click(event) {
      onPick([event.latlng.lat, event.latlng.lng])
    },
  })
  return null
}

const inputStyle = {
  width: '100%',
  padding: '13px 14px',
  border: '1px solid #dbe2ec',
  borderRadius: 9,
  color: '#172033',
  background: '#fff',
  outlineColor: '#2563eb',
  boxSizing: 'border-box',
}

// ---- Ride history helpers (added) ----
function getRideDate(ride) {
  return (
    ride.createdAt ||
    ride.requestedAt ||
    ride.bookedAt ||
    ride.bookingTime ||
    ride.rideDate ||
    ride.date ||
    null
  )
}

function formatRideDate(value) {
  if (!value) return 'N/A'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return String(value)
  return parsed.toLocaleString()
}

function getRideFare(ride) {
  const fare = ride.finalFare ?? ride.fare ?? ride.totalFare ?? ride.estimatedFare
  return fare === undefined || fare === null ? 'N/A' : fare
}

export default function App() {
  const [pickup, setPickup] = useState([30.4051, 78.3251])
  const [dropoff, setDropoff] = useState([30.3425, 78.0413])
  const [selecting, setSelecting] = useState('pickup')
  const [notice, setNotice] = useState(
    'Choose pickup or drop-off, then click the map.',
  )
  const [rideType, setRideType] = useState('Go')

  const [authMode, setAuthMode] = useState('login')
  const [registerRole, setRegisterRole] = useState('customer')
  const [showAuth, setShowAuth] = useState(false)
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [licenseNumber, setLicenseNumber] = useState('')
  const [vehicleModel, setVehicleModel] = useState('')
  const [vehicleNumber, setVehicleNumber] = useState('')

  const [token, setToken] = useState(() => localStorage.getItem('token') || '')
  const [userRole, setUserRole] = useState(() =>
    localStorage.getItem('role') || '',
  )

  const [busy, setBusy] = useState(false)
  const [lastRide, setLastRide] = useState(null)

  const [isAvailable, setIsAvailable] = useState(false)
  const [availableRides, setAvailableRides] = useState([])
  const [activeRide, setActiveRide] = useState(null)

  // Ride history state (added)
  const [rideHistory, setRideHistory] = useState([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyError, setHistoryError] = useState('')

  const loggedIn = Boolean(token)
  const isDriver = userRole === 'DRIVER'

  const apiRequest = useCallback(
    async (path, options = {}) => {
      const response = await fetch(`${API_URL}${path}`, {
        ...options,
        headers: {
          ...(options.body ? { 'Content-Type': 'application/json' } : {}),
          Authorization: `Bearer ${token}`,
          ...options.headers,
        },
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error('Your session expired. Please log in again.')
        }
        throw new Error(
          data.error || data.message || `Request failed (${response.status}).`,
        )
      }

      return data
    },
    [token],
  )

  // Load the customer's ride history from the backend (added)
  const loadRideHistory = useCallback(async () => {
    if (!token || userRole !== 'CUSTOMER') return

    setHistoryLoading(true)
    setHistoryError('')

    try {
      const data = await apiRequest('/api/rides/history')

      const list = Array.isArray(data)
        ? data
        : data.rides || data.history || data.content || []

      setRideHistory(list)
    } catch (error) {
      setHistoryError(error.message || 'Could not load ride history.')
    } finally {
      setHistoryLoading(false)
    }
  }, [token, userRole, apiRequest])

  // Automatically load history when a customer logs in (added)
  useEffect(() => {
    if (loggedIn && userRole === 'CUSTOMER') {
      loadRideHistory()
    } else {
      setRideHistory([])
      setHistoryError('')
      setHistoryLoading(false)
    }
  }, [loggedIn, userRole, loadRideHistory])

  // Generate and download a text receipt for a ride (added)
  function downloadReceipt(ride) {
    const lines = [
      'CabGo - Ride Receipt',
      '====================',
      `Ride ID: #${ride.rideId}`,
      `Date: ${formatRideDate(getRideDate(ride))}`,
      `Pickup: ${ride.pickupLocation ?? 'N/A'}`,
      `Destination: ${ride.dropoffLocation ?? 'N/A'}`,
      `Distance: ${ride.distanceKm ?? 'N/A'} km`,
      `Status: ${ride.status ?? 'N/A'}`,
      `Total fare: ₹${getRideFare(ride)}`,
      '',
      'Thank you for riding with CabGo.',
    ]

    const blob = new Blob([lines.join('\n')], {
      type: 'text/plain;charset=utf-8',
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `cabgo-receipt-ride-${ride.rideId}.txt`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('role')

    setToken('')
    setUserRole('')
    setLastRide(null)
    setActiveRide(null)
    setAvailableRides([])
    setIsAvailable(false)
    setRideHistory([])
    setHistoryError('')
    setHistoryLoading(false)
    setNotice('You have been logged out.')
  }

  async function handleAuth(event) {
    event.preventDefault()
    setBusy(true)
    setNotice('Please wait...')

    try {
      const isRegister = authMode === 'register'

      const registerAsDriver = isRegister && registerRole === 'driver'
      const authEndpoint = isRegister
        ? registerAsDriver
          ? '/api/drivers/register'
          : '/api/auth/register'
        : '/api/auth/login'

      const response = await fetch(`${API_URL}${authEndpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(
          isRegister
            ? registerAsDriver
              ? {
                  fullName,
                  email,
                  password,
                  licenseNumber,
                  vehicleModel,
                  vehicleNumber,
                }
              : { fullName, email, password, role: 'CUSTOMER' }
            : { email, password },
        ),
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.error || data.message || 'Authentication failed.')
      }

      if (isRegister) {
        setAuthMode('login')
        setNotice('Registration successful. Please sign in.')
        setPassword('')
        return
      }

      if (!data.token) {
        throw new Error('The login response did not contain a token.')
      }

      const role = data.role || 'CUSTOMER'

      localStorage.setItem('token', data.token)
      localStorage.setItem('role', role)

      setToken(data.token)
      setUserRole(role)
      setShowAuth(false)
      setPassword('')
      setIsAvailable(false)
      setActiveRide(null)
      setAvailableRides([])
      setNotice(
        role === 'DRIVER'
          ? 'Welcome, driver! Go online to see ride requests.'
          : 'Welcome back! You can request a ride.',
      )
    } catch (error) {
      setNotice(error.message || 'Could not connect to the backend.')
    } finally {
      setBusy(false)
    }
  }

  async function bookRide() {
    if (!loggedIn) {
      setAuthMode('login')
      setShowAuth(true)
      setNotice('Log in with your customer account to book a ride.')
      return
    }

    if (userRole !== 'CUSTOMER') {
      setNotice('Only customer accounts can book rides.')
      return
    }

    setBusy(true)
    setNotice('Calculating your route and booking...')

    try {
      const data = await apiRequest('/api/rides/book', {
        method: 'POST',
        body: JSON.stringify({
          pickupLocation: 'Selected pickup',
          dropoffLocation: 'Selected destination',
          pickupLatitude: pickup[0],
          pickupLongitude: pickup[1],
          dropoffLatitude: dropoff[0],
          dropoffLongitude: dropoff[1],
        }),
      })

      setLastRide(data)
      setNotice(
        `Ride #${data.rideId} requested. Estimated fare: ₹${data.estimatedFare}`,
      )

      // Refresh history after a successful booking (added)
      loadRideHistory()
    } catch (error) {
      setNotice(error.message || 'Ride booking failed.')
    } finally {
      setBusy(false)
    }
  }

  const refreshAvailableRides = useCallback(async () => {
    if (!token || userRole !== 'DRIVER' || !isAvailable || activeRide) {
      return
    }

    try {
      const data = await apiRequest('/api/driver/rides/available')
      setAvailableRides(data.rides || [])
    } catch (error) {
      setNotice(error.message || 'Could not load available rides.')
    }
  }, [token, userRole, isAvailable, activeRide, apiRequest])

  useEffect(() => {
    if (!isDriver || !isAvailable || activeRide) return

    refreshAvailableRides()

    const intervalId = window.setInterval(refreshAvailableRides, 10000)

    return () => window.clearInterval(intervalId)
  }, [isDriver, isAvailable, activeRide, refreshAvailableRides])

  // Restore an active ride from the backend after login or page refresh.
  const restoreActiveRide = useCallback(async () => {
    if (!token || userRole !== 'DRIVER') return

    try {
      const response = await fetch(`${API_URL}/api/driver/rides/active`, {
        headers: { Authorization: `Bearer ${token}` },
      })

      if (response.status === 204) {
        setActiveRide(null)
        return
      }

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data.error || data.message || 'Could not restore active ride.')
      }

      setActiveRide(data)
      setAvailableRides([])
      setNotice(`Restored active ride #${data.rideId} (${data.status}).`)
    } catch (error) {
      setNotice(error.message || 'Could not restore active ride.')
    }
  }, [token, userRole])

  useEffect(() => {
    if (isDriver) {
      restoreActiveRide()
    } else {
      setActiveRide(null)
    }
  }, [isDriver, restoreActiveRide])

  async function toggleAvailability() {
    setBusy(true)

    try {
      const nextAvailability = !isAvailable

      const data = await apiRequest('/api/driver/availability', {
        method: 'PUT',
        body: JSON.stringify({ available: nextAvailability }),
      })

      setIsAvailable(data.available)
      setNotice(
        data.available
          ? 'You are online. Loading available ride requests...'
          : 'You are now offline.',
      )

      if (!data.available) {
        setAvailableRides([])
      }
    } catch (error) {
      setNotice(error.message || 'Could not update driver availability.')
    } finally {
      setBusy(false)
    }
  }

  async function acceptRide(ride) {
    setBusy(true)
    setNotice(`Accepting ride #${ride.rideId}...`)

    try {
      const data = await apiRequest(
        `/api/driver/rides/${ride.rideId}/accept`,
        { method: 'PUT' },
      )

      setActiveRide({
        ...ride,
        status: data.status || 'ACCEPTED',
      })
      setAvailableRides([])
      setNotice(`Ride #${ride.rideId} accepted successfully.`)
    } catch (error) {
      setNotice(error.message || 'Could not accept this ride.')
      await refreshAvailableRides()
    } finally {
      setBusy(false)
    }
  }

  async function updateRideStatus(action, nextStatus) {
    if (!activeRide) return

    setBusy(true)

    try {
      const data = await apiRequest(
        `/api/driver/rides/${activeRide.rideId}/${action}`,
        { method: 'PUT' },
      )

      setActiveRide((current) => ({
        ...current,
        status: data.status || nextStatus,
      }))

      setNotice(
        data.message || `Ride #${activeRide.rideId}: ${nextStatus}.`,
      )

      if (nextStatus === 'COMPLETED') {
        setActiveRide(null)
        setIsAvailable(false)
        setNotice(
          `Ride #${activeRide.rideId} completed! Go online to receive another request.`,
        )
      }
    } catch (error) {
      setNotice(error.message || `Could not ${action} this ride.`)
    } finally {
      setBusy(false)
    }
  }

  function chooseLocation(position) {
    if (selecting === 'pickup') {
      setPickup(position)
      setNotice(
        `Pickup selected: ${position[0].toFixed(4)}, ${position[1].toFixed(4)}`,
      )
    } else {
      setDropoff(position)
      setNotice(
        `Destination selected: ${position[0].toFixed(4)}, ${position[1].toFixed(4)}`,
      )
    }

    setLastRide(null)
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <a className="brand" href="#" aria-label="CabGo home">
          <span className="brand-icon">C</span>
          CabGo
        </a>

        <span className="topbar-label">
          {isDriver ? 'Your next ride starts here' : 'Ride anywhere, anytime'}
        </span>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          {loggedIn ? (
            <>
              <span className="demo-badge">{userRole}</span>
              <button className="choice" onClick={logout}>
                Log out
              </button>
            </>
          ) : (
            <button
              className="book-button"
              style={{ width: 'auto', margin: 0, padding: '12px 16px' }}
              onClick={() => {
                setAuthMode('login')
                setShowAuth(true)
              }}
            >
              Sign in
            </button>
          )}
        </div>
      </header>

      {showAuth && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Account access"
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 2000,
            background: '#10182899',
            display: 'grid',
            placeItems: 'center',
            padding: 16,
          }}
        >
          <form
            onSubmit={handleAuth}
            style={{
              background: 'white',
              padding: 28,
              borderRadius: 16,
              width: 'min(100%, 420px)',
              display: 'grid',
              gap: 14,
              boxShadow: '0 20px 70px #0003',
            }}
          >
            <h2 style={{ color: '#14213d' }}>
              {authMode === 'login' ? 'Welcome back' : 'Create your account'}
            </h2>

            <p style={{ color: '#6b778c', fontSize: 13 }}>
  {authMode === 'login'
    ? 'Sign in to continue.'
    : `Register as a ${registerRole === 'driver' ? 'driver' : 'customer'} to get started.`}
</p>

            {authMode === 'register' && (
              <>
                <div style={{ display: 'flex', gap: 10 }}>
                  <button
                    type="button"
                    className="choice"
                    onClick={() => setRegisterRole('customer')}
                    style={{
                      flex: 1,
                      border: registerRole === 'customer'
                        ? '2px solid #2563eb'
                        : '1px solid #dbe2ec',
                    }}
                  >
                    Sign up to ride
                  </button>
                  <button
                    type="button"
                    className="choice"
                    onClick={() => setRegisterRole('driver')}
                    style={{
                      flex: 1,
                      border: registerRole === 'driver'
                        ? '2px solid #2563eb'
                        : '1px solid #dbe2ec',
                    }}
                  >
                    Sign up to drive
                  </button>
                </div>
                <input
                  required
                  minLength={2}
                  maxLength={100}
                  placeholder="Full name"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                  autoComplete="name"
                  style={inputStyle}
                />
                {registerRole === 'driver' && (
                  <>
                    <input
                      required
                      maxLength={100}
                      placeholder="Driving licence number"
                      value={licenseNumber}
                      onChange={(event) => setLicenseNumber(event.target.value)}
                      autoComplete="off"
                      style={inputStyle}
                    />
                    <input
                      required
                      maxLength={100}
                      placeholder="Vehicle model (e.g. Maruti Swift)"
                      value={vehicleModel}
                      onChange={(event) => setVehicleModel(event.target.value)}
                      autoComplete="off"
                      style={inputStyle}
                    />
                    <input
                      required
                      maxLength={30}
                      placeholder="Vehicle number (e.g. UK07AB1234)"
                      value={vehicleNumber}
                      onChange={(event) => setVehicleNumber(event.target.value)}
                      autoComplete="off"
                      style={inputStyle}
                    />
                  </>
                )}
              </>
            )}

            <input
              required
              type="email"
              maxLength={254}
              placeholder="Email address"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              style={inputStyle}
            />

            <input
              required
              type="password"
              minLength={authMode === 'register' ? 8 : undefined}
              placeholder="Password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete={
                authMode === 'login' ? 'current-password' : 'new-password'
              }
              style={inputStyle}
            />

            <button className="book-button" type="submit" disabled={busy}>
              {busy
                ? 'Please wait...'
                : authMode === 'login'
                  ? 'Sign in'
                  : 'Create account'}
              <span>→</span>
            </button>

            <button
              type="button"
              className="choice"
              onClick={() =>
                setAuthMode(authMode === 'login' ? 'register' : 'login')
              }
            >
              {authMode === 'login'
                ? 'New here? Create an account'
                : 'Already registered? Sign in'}
            </button>

            <button
              type="button"
              className="choice"
              onClick={() => setShowAuth(false)}
            >
              Close
            </button>
          </form>
        </div>
      )}

      <section className="booking-layout">
        <aside className="booking-panel">
          {isDriver ? (
            <>
              <p className="eyebrow">DRIVER PARTNER</p>
              <h1>
                Your ride.
                <br />
                <span>Your journey.</span>
              </h1>

              <p className="intro">
                Go online to receive ride requests from customers.
              </p>

              <div className="location-card">
                <div className="location-line">
                  <span
                    className="location-dot"
                    style={{
                      background: isAvailable ? '#16a34a' : '#9ca3af',
                      borderColor: isAvailable ? '#d9fbe7' : '#e5e7eb',
                    }}
                  />
                  <div>
                    <small>DRIVER STATUS</small>
                    <strong>{isAvailable ? 'ONLINE — READY FOR RIDES' : 'OFFLINE'}</strong>
                  </div>
                </div>

                {activeRide && (
                  <>
                    <div className="location-connector" />
                    <div className="location-line">
                      <span className="location-dot dropoff-dot" />
                      <div>
                        <small>YOUR CURRENT RIDE</small>
                        <strong>Ride #{activeRide.rideId}</strong>
                        <strong>Status: {activeRide.status}</strong>
                      </div>
                    </div>
                  </>
                )}
              </div>

              <button
                className="book-button"
                onClick={toggleAvailability}
                disabled={busy || Boolean(activeRide)}
                style={{
                  background: isAvailable ? '#475569' : '#16a34a',
                  marginTop: 18,
                }}
              >
                {busy
                  ? 'Please wait...'
                  : isAvailable
                    ? 'Go offline'
                    : 'Go online'}
                <span>→</span>
              </button>

              {activeRide ? (
                <div className="location-card" style={{ marginTop: 18 }}>
                  <p className="field-label" style={{ marginTop: 0 }}>
                    CURRENT RIDE
                  </p>

                  <strong>Ride #{activeRide.rideId}</strong>

                  <p style={{ marginTop: 12, fontSize: 13 }}>
                    Pickup: {activeRide.pickupLocation}
                  </p>
                  <p style={{ marginTop: 8, fontSize: 13 }}>
                    Destination: {activeRide.dropoffLocation}
                  </p>
                  <p style={{ marginTop: 8, fontSize: 13 }}>
                    Distance: {activeRide.distanceKm} km
                  </p>
                  <p style={{ marginTop: 8, fontSize: 13 }}>
                    Estimated fare: ₹{activeRide.estimatedFare}
                  </p>
                  <p style={{ marginTop: 8, fontSize: 13 }}>
                    Status: {activeRide.status}
                  </p>

                  {activeRide.status === 'ACCEPTED' && (
                    <button
                      className="book-button"
                      style={{ marginTop: 16 }}
                      disabled={busy}
                      onClick={() =>
                        updateRideStatus('start', 'IN_PROGRESS')
                      }
                    >
                      Start ride <span>→</span>
                    </button>
                  )}

                  {activeRide.status === 'IN_PROGRESS' && (
                    <button
                      className="book-button"
                      style={{ marginTop: 16, background: '#16a34a' }}
                      disabled={busy}
                      onClick={() =>
                        updateRideStatus('complete', 'COMPLETED')
                      }
                    >
                      Complete ride <span>✓</span>
                    </button>
                  )}
                </div>
              ) : (
                <>
                  <div className="map-tip" style={{ marginTop: 18 }}>
                    <span className="tip-icon">↗</span>
                    <p>
                      {isAvailable
                        ? 'You are online. Available ride requests refresh automatically.'
                        : 'Go online to see customer requests and accept a ride.'}
                    </p>
                  </div>

                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 8,
                      marginTop: 22,
                    }}
                  >
                    <p className="field-label" style={{ margin: 0 }}>
                      AVAILABLE REQUESTS ({availableRides.length})
                    </p>
                    <button
                      className="choice"
                      disabled={busy || !isAvailable}
                      onClick={refreshAvailableRides}
                    >
                      Refresh
                    </button>
                  </div>

                  {!isAvailable && (
                    <p className="intro" style={{ marginTop: 12 }}>
                      Go online to load available rides.
                    </p>
                  )}

                  {isAvailable && availableRides.length === 0 && (
                    <div className="location-card" style={{ marginTop: 14 }}>
                      <p style={{ fontSize: 13, color: '#6b778c' }}>
                        No ride requests right now. We will check again
                        automatically.
                      </p>
                    </div>
                  )}

                  {availableRides.map((ride) => (
                    <div
                      className="location-card"
                      key={ride.rideId}
                      style={{ marginTop: 14 }}
                    >
                      <strong>Ride #{ride.rideId}</strong>
                      <p style={{ marginTop: 12, fontSize: 13 }}>
                        Pickup: {ride.pickupLocation}
                      </p>
                      <p style={{ marginTop: 8, fontSize: 13 }}>
                        Destination: {ride.dropoffLocation}
                      </p>
                      <p style={{ marginTop: 8, fontSize: 13 }}>
                        Distance: {ride.distanceKm} km
                      </p>
                      <p style={{ marginTop: 8, fontSize: 13 }}>
                        Estimated fare: ₹{ride.estimatedFare}
                      </p>

                      <button
                        className="book-button"
                        style={{ marginTop: 16 }}
                        disabled={busy}
                        onClick={() => acceptRide(ride)}
                      >
                        Accept ride <span>→</span>
                      </button>
                    </div>
                  ))}
                </>
              )}

              <div className="map-tip" style={{ marginTop: 18 }}>
                <span className="tip-icon">i</span>
                <p>{notice}</p>
              </div>

              <p className="disclaimer">
                Map © OpenStreetMap contributors · Routing via OSRM
              </p>
            </>
          ) : (
            <>
              <p className="eyebrow">YOUR CITY. YOUR RIDE.</p>
              <h1>
                Go places.
                <br />
                <span>Feel free.</span>
              </h1>

              <p className="intro">
                Choose your pickup and destination on the map, then request a
                ride.
              </p>

              <div className="location-card">
                <div className="location-line">
                  <span className="location-dot pickup-dot" />
                  <div>
                    <small>YOUR PICKUP</small>
                    <strong>
                      {pickup[0].toFixed(5)}, {pickup[1].toFixed(5)}
                    </strong>
                  </div>
                </div>

                <div className="location-connector" />

                <div className="location-line">
                  <span className="location-dot dropoff-dot" />
                  <div>
                    <small>YOUR DESTINATION</small>
                    <strong>
                      {dropoff[0].toFixed(5)}, {dropoff[1].toFixed(5)}
                    </strong>
                  </div>
                </div>
              </div>

              <p className="field-label">SELECT A LOCATION TO CHANGE</p>

              <div className="location-actions">
                <button
                  className={
                    selecting === 'pickup' ? 'choice active' : 'choice'
                  }
                  onClick={() => {
                    setSelecting('pickup')
                    setNotice('Click the map to select your pickup.')
                  }}
                >
                  <span>＋</span> Set pickup
                </button>

                <button
                  className={
                    selecting === 'dropoff' ? 'choice active' : 'choice'
                  }
                  onClick={() => {
                    setSelecting('dropoff')
                    setNotice('Click the map to select your destination.')
                  }}
                >
                  <span>⌖</span> Set destination
                </button>
              </div>

              <p className="field-label">CHOOSE YOUR RIDE</p>

              <div className="ride-options">
                {[
                  { name: 'Go', icon: '🚕', description: 'Everyday ride' },
                  { name: 'Comfort', icon: '🚘', description: 'Extra comfort' },
                  { name: 'XL', icon: '🚙', description: 'Room for more' },
                ].map((ride) => (
                  <button
                    key={ride.name}
                    className={
                      rideType === ride.name
                        ? 'ride-option selected'
                        : 'ride-option'
                    }
                    onClick={() => setRideType(ride.name)}
                  >
                    <span className="ride-emoji">{ride.icon}</span>
                    <strong>{ride.name}</strong>
                    <small>{ride.description}</small>
                  </button>
                ))}
              </div>

              <div className="map-tip">
                <span className="tip-icon">↗</span>
                <p>{notice}</p>
              </div>

              {lastRide && (
                <div className="location-card" style={{ marginTop: 14 }}>
                  <strong>Ride #{lastRide.rideId}</strong>
                  <p style={{ marginTop: 8, fontSize: 13 }}>
                    Status: {lastRide.status}
                  </p>
                  <p style={{ marginTop: 5, fontSize: 13 }}>
                    Distance: {lastRide.distanceKm} km
                  </p>
                  <p style={{ marginTop: 5, fontSize: 13 }}>
                    Estimated time:{' '}
                    {lastRide.estimatedDurationMinutes ?? 'N/A'} minutes
                  </p>
                  <p style={{ marginTop: 5, fontSize: 13 }}>
                    Estimated fare: ₹{lastRide.estimatedFare}
                  </p>
                </div>
              )}

              <button
                className="book-button"
                onClick={bookRide}
                disabled={busy}
              >
                {busy ? 'Please wait...' : 'Request a ride'} <span>→</span>
              </button>

              {/* Ride History section (added) */}
              {loggedIn && userRole === 'CUSTOMER' && (
                <>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 8,
                      marginTop: 22,
                    }}
                  >
                    <p className="field-label" style={{ margin: 0 }}>
                      RIDE HISTORY ({rideHistory.length})
                    </p>
                    <button
                      className="choice"
                      disabled={historyLoading}
                      onClick={loadRideHistory}
                    >
                      {historyLoading ? 'Loading...' : 'Refresh'}
                    </button>
                  </div>

                  {historyLoading && rideHistory.length === 0 && (
                    <div className="location-card" style={{ marginTop: 14 }}>
                      <p style={{ fontSize: 13, color: '#6b778c' }}>
                        Loading your ride history...
                      </p>
                    </div>
                  )}

                  {historyError && (
                    <div className="location-card" style={{ marginTop: 14 }}>
                      <p style={{ fontSize: 13, color: '#dc2626' }}>
                        {historyError}
                      </p>
                    </div>
                  )}

                  {!historyLoading &&
                    !historyError &&
                    rideHistory.length === 0 && (
                      <div className="location-card" style={{ marginTop: 14 }}>
                        <p style={{ fontSize: 13, color: '#6b778c' }}>
                          No rides yet. Your booked rides will appear here.
                        </p>
                      </div>
                    )}

                  {rideHistory.map((ride, index) => (
                    <div
                      className="location-card"
                      key={ride.rideId ?? index}
                      style={{ marginTop: 14 }}
                    >
                      <strong>Ride #{ride.rideId}</strong>
                      <p style={{ marginTop: 8, fontSize: 13 }}>
                        Date: {formatRideDate(getRideDate(ride))}
                      </p>
                      <p style={{ marginTop: 5, fontSize: 13 }}>
                        Pickup: {ride.pickupLocation ?? 'N/A'}
                      </p>
                      <p style={{ marginTop: 5, fontSize: 13 }}>
                        Destination: {ride.dropoffLocation ?? 'N/A'}
                      </p>
                      <p style={{ marginTop: 5, fontSize: 13 }}>
                        Status: {ride.status ?? 'N/A'}
                      </p>
                      <p style={{ marginTop: 5, fontSize: 13 }}>
                        Distance: {ride.distanceKm ?? 'N/A'} km
                      </p>
                      <p style={{ marginTop: 5, fontSize: 13 }}>
                        Fare: ₹{getRideFare(ride)}
                      </p>

                      {ride.status === 'COMPLETED' && (
                        <button
                          className="choice"
                          style={{ marginTop: 12 }}
                          onClick={() => downloadReceipt(ride)}
                        >
                          Download Receipt
                        </button>
                      )}
                    </div>
                  ))}
                </>
              )}

              <p className="disclaimer">
                Map © OpenStreetMap contributors · Routing via OSRM
              </p>
            </>
          )}
        </aside>

        <section className="map-panel">
          <div className="map-heading">
            <div>
              <span className="live-dot" />
              <strong>
                {isDriver ? 'Driver map' : 'Plan your journey'}
              </strong>
            </div>
            <span className="map-region">DEHRADUN, INDIA</span>
          </div>

          <div className="map-area">
            <MapContainer
              center={[30.373, 78.18]}
              zoom={11}
              scrollWheelZoom
              style={{ height: '100%', width: '100%' }}
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />

              {!isDriver && <LocationPicker onPick={chooseLocation} />}

              {(!isDriver || activeRide) && (
                <>
                  <Marker position={pickup} icon={pickupIcon}>
                    <Popup>Pickup location</Popup>
                  </Marker>
                  <Marker position={dropoff} icon={dropoffIcon}>
                    <Popup>Destination</Popup>
                  </Marker>
                </>
              )}
            </MapContainer>

            {!isDriver && (
              <div className="map-legend">
                <span>
                  <i className="legend-pickup" /> Pickup
                </span>
                <span>
                  <i className="legend-dropoff" /> Destination
                </span>
              </div>
            )}
          </div>

          <div className="map-footer">
            <div>
              <span className="footer-icon">⌖</span>
              <span>
                <strong>
                  {isDriver ? 'Driver partner dashboard' : 'Pick your points'}
                </strong>
                <small>
                  {isDriver
                    ? 'Manage availability and ride requests'
                    : 'Click the map to change the selected location'}
                </small>
              </span>
            </div>
            <span className="map-status">OPENSTREETMAP</span>
          </div>
        </section>
      </section>

      <footer className="page-footer">
        <span>CabGo © 2026</span>
        <span>Made for smoother journeys.</span>
      </footer>
    </main>
  )
}
