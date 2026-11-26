import React, {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {api} from './api';
import './styles.css';

function ErrorBox({error}) {
  if (!error) return null;
  const message = error.message || 'The request could not be completed.';
  return (
    <div className="notice notice-error" role="alert">
      <span className="notice-icon" aria-hidden="true">!</span>
      <div>
        <strong>{message}</strong>
        {error.status && <span className="notice-detail">HTTP {error.status}</span>}
        {error.fieldErrors?.length > 0 && (
          <ul className="field-errors">
            {error.fieldErrors.map(({field, message: detail}) => <li key={`${field}-${detail}`}>{field}: {detail}</li>)}
          </ul>
        )}
      </div>
    </div>
  );
}

function SuccessNotice({children}) {
  if (!children) return null;
  return <div className="notice notice-success" role="status"><span className="notice-icon" aria-hidden="true">✓</span><strong>{children}</strong></div>;
}

function StatusBadge({open}) {
  return <span className={`status-badge ${open ? 'is-open' : 'is-closed'}`}><span className="status-dot"/> {open ? 'OPEN' : 'CLOSED'}</span>;
}

function TokenStatusBadge({status}) {
  return <span className={`token-status token-${status.toLowerCase()}`}>{status}</span>;
}

function Loading({children = 'Loading queue data…'}) {
  return <div className="loading-state" role="status"><span className="spinner" aria-hidden="true"/>{children}</div>;
}

export function App() {
  const [route, setRoute] = useState(location.hash === '#/admin' ? 'admin' : 'customer');
  useEffect(() => {
    const handleHashChange = () => setRoute(location.hash === '#/admin' ? 'admin' : 'customer');
    addEventListener('hashchange', handleHashChange);
    return () => removeEventListener('hashchange', handleHashChange);
  }, []);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#/customer" aria-label="QueueLess home">
          <span className="brand-mark">Q</span>
          <span>Queue<span className="brand-light">Less</span></span>
        </a>
        <div className="sidebar-caption">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          <a className={route === 'customer' ? 'active' : ''} href="#/customer"><span className="nav-icon">⌂</span> Customer</a>
          <a className={route === 'admin' ? 'active' : ''} href="#/admin"><span className="nav-icon">▦</span> Admin dashboard</a>
        </nav>
        <div className="sidebar-footer">
          <span className="online-dot"/>
          <span><strong>Local demo</strong><small>Service centre workspace</small></span>
        </div>
      </aside>
      <div className="app-content">
        <header className="topbar">
          <div className="breadcrumb">QueueLess <span>/</span> {route === 'admin' ? 'Administration' : 'Customer portal'}</div>
          <div className="topbar-status"><span className="online-dot"/> Demo environment</div>
        </header>
        {route === 'admin' ? <Admin/> : <Customer/>}
      </div>
    </div>
  );
}

function Customer() {
  const [queues, setQueues] = useState([]);
  const [queuesLoading, setQueuesLoading] = useState(true);
  const [selected, setSelected] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [token, setToken] = useState(null);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    api('/api/queues')
      .then(data => { if (active) setQueues(data); })
      .catch(err => { if (active) setError(err); })
      .finally(() => { if (active) setQueuesLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!token) return undefined;
    const timer = setInterval(() => {
      api(`/api/tokens/${token.tokenId}`).then(setToken).catch(setError);
    }, 3000);
    return () => clearInterval(timer);
  }, [token?.tokenId]);

  async function join(event) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const joined = await api(`/api/queues/${selected}/join`, {
        method: 'POST',
        body: JSON.stringify({customerName: name.trim(), phone: phone.trim() || null}),
      });
      setToken(joined);
    } catch (requestError) {
      setError(requestError);
    } finally {
      setSubmitting(false);
    }
  }

  async function cancel() {
    setError(null);
    setSubmitting(true);
    try {
      setToken(await api(`/api/tokens/${token.tokenId}/cancel`, {method: 'POST'}));
    } catch (requestError) {
      setError(requestError);
    } finally {
      setSubmitting(false);
    }
  }

  const openQueues = queues.filter(queue => queue.open);

  return (
    <main className="page customer-page">
      <section className="customer-hero">
        <div className="hero-copy">
          <span className="eyebrow"><span className="online-dot"/> YOUR TIME, BETTER SPENT</span>
          <h1>Skip the line.<br/><span>Keep your day.</span></h1>
          <p>Join a queue from wherever you are and follow your place in real time. We’ll let you know when it’s your turn.</p>
          <div className="hero-points"><span>Easy to join</span><span>Live position</span><span>No app needed</span></div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="hero-orbit orbit-one"/><div className="hero-orbit orbit-two"/>
          <div className="ticket-card"><span className="ticket-label">YOUR PLACE IN LINE</span><strong>Q<span>–</span>–</strong><span className="ticket-dots"/><span className="ticket-wait">A little more time for you.</span></div>
          <span className="art-spark spark-one">✦</span><span className="art-spark spark-two">✦</span>
        </div>
      </section>

      <div className="customer-layout">
        <section className="panel join-panel">
          <div className="section-heading">
            <div><span className="step-label">STEP 01</span><h2>Join a queue</h2><p>Choose a service and tell us who’s arriving.</p></div>
            <span className="heading-icon" aria-hidden="true">↗</span>
          </div>
          <ErrorBox error={error}/>
          {token ? (
            <section className="token-ticket" aria-label="Your queue ticket">
              <span className="ticket-label">YOUR DIGITAL TICKET</span>
              <h3>{token.displayIdentifier}</h3>
              <TokenStatusBadge status={token.status}/>
              {token.status === 'WAITING' ? (
                <>
                  <div className="token-metrics">
                    <div><span>YOUR POSITION</span><strong>{token.position}</strong><small>in line</small></div>
                    <div><span>EST. WAIT</span><strong>{token.estimatedWaitMinutes}<small> min</small></strong><small>based on current queue</small></div>
                  </div>
                  <button className="button button-quiet button-full" type="button" onClick={cancel} disabled={submitting}>{submitting ? 'Cancelling…' : 'Leave this queue'}</button>
                </>
              ) : (
                <p className="token-complete-message">{token.status === 'CALLED' ? 'It’s your turn. Please see a team member.' : `Your queue visit is ${token.status.toLowerCase()}.`}</p>
              )}
            </section>
          ) : queuesLoading ? <Loading/> : (
            <form className="form-stack" onSubmit={join}>
              <label htmlFor="queue-select">Service queue
                <select id="queue-select" value={selected} onChange={event => setSelected(event.target.value)} required>
                  <option value="">Select a queue</option>
                  {openQueues.map(queue => <option key={queue.queueId} value={queue.queueId}>{queue.name}</option>)}
                </select>
              </label>
              {openQueues.length === 0 && <p className="inline-hint">There are no open queues right now. Please check back soon.</p>}
              <label htmlFor="customer-name">Your name
                <input id="customer-name" value={name} onChange={event => setName(event.target.value)} placeholder="e.g. Alex Morgan" autoComplete="name" maxLength="100" required/>
              </label>
              <label htmlFor="customer-phone">Phone <span className="optional-label">OPTIONAL</span>
                <input id="customer-phone" value={phone} onChange={event => setPhone(event.target.value)} placeholder="For queue identification" autoComplete="tel" maxLength="40"/>
              </label>
              <button className="button button-primary button-full" disabled={submitting || queuesLoading || openQueues.length === 0}>
                {submitting ? <><span className="button-spinner"/> Joining queue…</> : <>Join queue <span aria-hidden="true">→</span></>}
              </button>
              <p className="form-footnote">By joining, you agree to wait nearby and respond when your number is called.</p>
            </form>
          )}
        </section>

        <aside className="customer-aside">
          <div className="aside-card aside-tip">
            <span className="aside-symbol" aria-hidden="true">✦</span>
            <h3>A calmer way to wait</h3>
            <p>Keep doing what you love. Your place updates automatically while you wait.</p>
            <div className="tip-rule"/><div className="tip-meta"><span className="pulse-dot"/> Queue status refreshes live</div>
          </div>
          <div className="aside-card privacy-note"><span className="privacy-icon" aria-hidden="true">◈</span><p>Your details are only used to manage your place in this queue.</p></div>
        </aside>
      </div>
    </main>
  );
}

function Admin() {
  const [queues, setQueues] = useState([]);
  const [queuesLoading, setQueuesLoading] = useState(true);
  const [selected, setSelected] = useState('');
  const [queueConfig, setQueueConfig] = useState(null);
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState('');
  const [name, setName] = useState('');
  const [prefix, setPrefix] = useState('');
  const [duration, setDuration] = useState('5');
  const [creating, setCreating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [actionPending, setActionPending] = useState(false);

  async function refreshQueues() {
    const updated = await api('/api/queues');
    setQueues(updated);
    return updated;
  }

  useEffect(() => {
    let active = true;
    api('/api/queues')
      .then(data => { if (active) setQueues(data); })
      .catch(err => { if (active) setError(err); })
      .finally(() => { if (active) setQueuesLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!selected) return undefined;
    let active = true;
    const load = () => api(`/api/queues/${selected}/dashboard`)
      .then(data => { if (active) setDashboard(data); })
      .catch(err => { if (active) setError(err); });
    load();
    const timer = setInterval(load, 3000);
    return () => { active = false; clearInterval(timer); };
  }, [selected]);

  async function create(event) {
    event.preventDefault();
    setError(null);
    setSuccess('');
    setCreating(true);
    try {
      const created = await api('/api/queues', {
        method: 'POST',
        body: JSON.stringify({name: name.trim(), prefix: prefix.trim(), averageServiceDurationMinutes: Number(duration), open: true}),
      });
      setName('');
      setPrefix('');
      setDuration('5');
      setSuccess(`${created.name} was created successfully.`);
      try {
        await refreshQueues();
      } catch (refreshError) {
        setError({message: `${created.name} was created, but the queue list could not be refreshed.`, status: refreshError.status});
      }
    } catch (requestError) {
      setError(requestError);
    } finally {
      setCreating(false);
    }
  }

  async function selectQueue(queue) {
    setSelected(String(queue.queueId));
    setDashboard(null);
    setQueueConfig(null);
    setError(null);
    setSuccess('');
    try {
      setQueueConfig(await api(`/api/queues/${queue.queueId}`));
    } catch (requestError) {
      setError(requestError);
    }
  }

  async function updateQueue(event) {
    event.preventDefault();
    if (!queueConfig) return;
    setError(null);
    setSuccess('');
    setSaving(true);
    try {
      const updated = await api(`/api/queues/${queueConfig.queueId}`, {
        method: 'PUT',
        body: JSON.stringify({...queueConfig, averageServiceDurationMinutes: Number(queueConfig.averageServiceDurationMinutes)}),
      });
      setQueueConfig(updated);
      setSuccess(`${updated.name} settings saved.`);
      try {
        await refreshQueues();
      } catch (refreshError) {
        setError({message: `${updated.name} was saved, but the queue list could not be refreshed.`, status: refreshError.status});
      }
    } catch (requestError) {
      setError(requestError);
    } finally {
      setSaving(false);
    }
  }

  async function action(path, label) {
    setError(null);
    setSuccess('');
    setActionPending(true);
    try {
      await api(path, {method: 'POST'});
      setSuccess(label);
      setDashboard(await api(`/api/queues/${selected}/dashboard`));
    } catch (requestError) {
      setError(requestError);
    } finally {
      setActionPending(false);
    }
  }

  async function toggle(queue, open) {
    setError(null);
    setSuccess('');
    setActionPending(true);
    try {
      await api(`/api/queues/${queue.queueId}/status`, {method: 'PATCH', body: JSON.stringify({open})});
      if (queueConfig?.queueId === queue.queueId) setQueueConfig({...queueConfig, open});
      setSuccess(`${queue.name} is now ${open ? 'open' : 'closed'}.`);
      await refreshQueues();
    } catch (requestError) {
      setError(requestError);
    } finally {
      setActionPending(false);
    }
  }

  const openCount = queues.filter(queue => queue.open).length;
  const selectedQueue = queues.find(queue => String(queue.queueId) === selected);
  const counts = dashboard?.counts || {};
  const countItems = [
    {key: 'WAITING', label: 'Waiting', tone: 'waiting'},
    {key: 'CALLED', label: 'Called', tone: 'called'},
    {key: 'COMPLETED', label: 'Completed', tone: 'completed'},
    {key: 'SKIPPED', label: 'Skipped', tone: 'skipped'},
    {key: 'CANCELLED', label: 'Cancelled', tone: 'cancelled'},
  ];

  return (
    <main className="page admin-page">
      <section className="page-heading">
        <div>
          <span className="eyebrow">OVERVIEW <span className="heading-divider">/</span> QUEUE OPERATIONS</span>
          <h1>Administrator dashboard</h1>
          <p>Manage your service queues and keep every visit moving.</p>
        </div>
        <span className="demo-badge"><span className="online-dot"/> Local demo access — no production authentication</span>
      </section>
      <ErrorBox error={error}/>
      <SuccessNotice>{success}</SuccessNotice>

      <section className="stat-grid" aria-label="Queue overview">
        <div className="stat-card"><span className="stat-icon stat-violet">▦</span><div><span className="stat-label">TOTAL QUEUES</span><strong>{queuesLoading ? '—' : queues.length}</strong><small>Service lines</small></div></div>
        <div className="stat-card"><span className="stat-icon stat-green">↗</span><div><span className="stat-label">OPEN NOW</span><strong>{queuesLoading ? '—' : openCount}</strong><small>Accepting customers</small></div></div>
        <div className="stat-card"><span className="stat-icon stat-amber">◷</span><div><span className="stat-label">CLOSED</span><strong>{queuesLoading ? '—' : queues.length - openCount}</strong><small>Temporarily paused</small></div></div>
      </section>

      <div className="admin-layout">
        <div className="admin-primary">
          <section className="panel queue-panel">
            <div className="section-heading">
              <div><span className="step-label">YOUR WORKSPACE</span><h2>Service queues</h2><p>Select a queue to configure and monitor it.</p></div>
              <span className="count-chip">{queues.length} {queues.length === 1 ? 'queue' : 'queues'}</span>
            </div>
            {queuesLoading ? <Loading/> : queues.length === 0 ? (
              <div className="empty-state"><span className="empty-icon">▦</span><h3>No queues yet</h3><p>Create your first service queue to start welcoming customers.</p></div>
            ) : (
              <div className="queue-grid">
                {queues.map(queue => (
                  <article className={`queue-card ${selected === String(queue.queueId) ? 'queue-card-selected' : ''}`} key={queue.queueId}>
                    <button className="queue-select" type="button" onClick={() => selectQueue(queue)} aria-label={`${queue.name} ${queue.prefix} · ${queue.open ? 'OPEN' : 'CLOSED'}`}>
                      <span className="queue-card-top"><span className="queue-prefix">{queue.prefix}</span><StatusBadge open={queue.open}/></span>
                      <strong>{queue.name}</strong><span className="queue-subtitle">Service queue</span>
                      <span className="queue-card-footer">Manage queue <span aria-hidden="true">→</span></span>
                    </button>
                    <button className={`button button-small ${queue.open ? 'button-quiet' : 'button-outline'}`} type="button" onClick={() => toggle(queue, !queue.open)} disabled={actionPending} aria-label={`${queue.open ? 'Close' : 'Open'} ${queue.name}`}>
                      {queue.open ? 'Close queue' : 'Open queue'}
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>

          {selected && (
            <section className="panel dashboard-panel">
              <div className="section-heading">
                <div><span className="step-label">LIVE MONITORING</span><h2>Dashboard</h2><p>{selectedQueue ? `${selectedQueue.name} · ` : ''}Queue totals and current customer activity.</p></div>
                <span className="live-badge"><span className="pulse-dot"/> LIVE</span>
              </div>
              {!dashboard ? <Loading>Loading dashboard…</Loading> : (
                <>
                  <div className="totals-grid">
                    {countItems.map(item => (
                      <div className={`total-card total-${item.tone}`} key={item.key}>
                        <span className="total-value">{counts[item.key] ?? 0}</span><span className="total-label">{item.label}</span>
                      </div>
                    ))}
                  </div>
                  <div className="active-heading"><div><h3>Customers in queue</h3><span>Waiting and currently called</span></div><button className="button button-primary" type="button" onClick={() => action(`/api/queues/${selected}/call-next`, 'Next customer called.')} disabled={actionPending || !selectedQueue?.open}>{actionPending ? 'Updating…' : <>Call next <span aria-hidden="true">→</span></>}</button></div>
                  {dashboard.activeTokens.length === 0 ? (
                    <div className="empty-state empty-state-compact"><span className="empty-icon">◷</span><h3>All clear</h3><p>No customers are waiting in this queue.</p></div>
                  ) : (
                    <div className="token-list">
                      {dashboard.activeTokens.map(token => (
                        <article className="token-row" key={token.tokenId}>
                          <span className="token-avatar">{token.customerName?.slice(0, 1)?.toUpperCase() || '#'}</span>
                          <div className="token-info"><strong>{token.displayIdentifier}</strong><span>{token.customerName || `Token ${token.sequenceNumber}`}</span></div>
                          <span className={`token-status token-${token.status.toLowerCase()}`}>{token.status}</span>
                          {token.status === 'CALLED' && <div className="token-actions">
                            <button className="button button-small button-primary" type="button" onClick={() => action(`/api/tokens/${token.tokenId}/complete`, `${token.displayIdentifier} marked complete.`)} disabled={actionPending}>Complete</button>
                            <button className="button button-small button-quiet" type="button" onClick={() => action(`/api/tokens/${token.tokenId}/skip`, `${token.displayIdentifier} skipped.`)} disabled={actionPending}>Skip</button>
                          </div>}
                        </article>
                      ))}
                    </div>
                  )}
                  <p className="refresh-note"><span className="pulse-dot"/> Updates automatically every few seconds</p>
                </>
              )}
            </section>
          )}
        </div>

        <aside className="admin-secondary">
          <section className="panel create-panel">
            <div className="section-heading">
              <div><span className="step-label">GET STARTED</span><h2>Create queue</h2><p>Set up a new service line in seconds.</p></div>
            </div>
            <form className="form-stack" onSubmit={create}>
              <label htmlFor="admin-queue-name">New queue name
                <input id="admin-queue-name" value={name} onChange={event => setName(event.target.value)} placeholder="e.g. Customer support" maxLength="100" required/>
              </label>
              <label htmlFor="admin-queue-prefix">Ticket prefix
                <input id="admin-queue-prefix" value={prefix} onChange={event => setPrefix(event.target.value)} placeholder="e.g. CS" maxLength="20" required/>
              </label>
              <label htmlFor="admin-queue-duration">Average service time
                <div className="input-suffix"><input id="admin-queue-duration" aria-label="Average service time (minutes)" type="number" min="1" value={duration} onChange={event => setDuration(event.target.value)} required/><span>minutes</span></div>
              </label>
              <button className="button button-primary button-full" disabled={creating}>{creating ? <><span className="button-spinner"/> Creating…</> : <>Create queue <span aria-hidden="true">→</span></>}</button>
            </form>
          </section>

          {queueConfig && (
            <section className="panel configure-panel">
              <div className="section-heading">
                <div><span className="step-label">QUEUE SETTINGS</span><h2>Configure {queueConfig.name}</h2><p>Update queue details and service timing.</p></div>
              </div>
              <form className="form-stack" onSubmit={updateQueue}>
                <label htmlFor="config-name">Queue name
                  <input id="config-name" aria-label="Queue name" value={queueConfig.name} onChange={event => setQueueConfig({...queueConfig, name: event.target.value})} maxLength="100" required/>
                </label>
                <label htmlFor="config-prefix">Ticket prefix
                  <input id="config-prefix" aria-label="Queue prefix" value={queueConfig.prefix} onChange={event => setQueueConfig({...queueConfig, prefix: event.target.value})} maxLength="20" required/>
                </label>
                <label htmlFor="config-duration">Average service time
                  <div className="input-suffix"><input id="config-duration" aria-label="Average service duration" type="number" min="1" value={queueConfig.averageServiceDurationMinutes} onChange={event => setQueueConfig({...queueConfig, averageServiceDurationMinutes: event.target.value})} required/><span>minutes</span></div>
                </label>
                <button className="button button-outline button-full" disabled={saving}>{saving ? 'Saving…' : 'Save queue changes'}</button>
              </form>
            </section>
          )}

          <div className="demo-note"><span className="demo-note-icon">i</span><div><strong>Demo workspace</strong><p>Admin access is for local demonstration only. It is not protected by production authentication.</p></div></div>
        </aside>
      </div>
    </main>
  );
}

export default App;

const rootElement = document.getElementById('root');
const root = rootElement && (import.meta.hot?.data.root || createRoot(rootElement));
if (root) {
  if (import.meta.hot) import.meta.hot.data.root = root;
  root.render(<App/>);
}
