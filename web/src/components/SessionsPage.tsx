import { useState, useEffect, useCallback } from 'react'
import type { Settings, Session } from '../lib/types'
import * as api from '../lib/api'

interface Props {
  settings: Settings
  onOpenChat: (sessionId: string) => void
  onOpenSettings: () => void
}

export default function SessionsPage({ settings, onOpenChat, onOpenSettings }: Props) {
  const [sessions, setSessions] = useState<Session[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)

  const refresh = useCallback(async () => {
    try {
      setError('')
      const list = await api.listSessions(settings)
      list.sort((a, b) => b.last_active_at - a.last_active_at)
      setSessions(list)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load sessions')
    } finally {
      setLoading(false)
    }
  }, [settings])

  useEffect(() => { refresh() }, [refresh])

  const handleDelete = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation()
    if (!confirm('Delete this session?')) return
    try {
      await api.deleteSession(settings, id)
      setSessions((s) => s.filter((x) => x.id !== id))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed')
    }
  }

  const statusColor = (s: string) => `status-${s}`

  return (
    <div className="sessions-page">
      <div className="header">
        <h1>Claude Code</h1>
        <div className="spacer" />
        <button onClick={refresh} disabled={loading}>Refresh</button>
        <button onClick={onOpenSettings}>Settings</button>
        <button className="primary" onClick={() => setShowCreate(true)}>New Session</button>
      </div>

      {error && <div style={{ padding: '8px 16px', color: 'var(--red)', fontSize: 13 }}>{error}</div>}

      <div className="sessions-list">
        {loading && <div className="empty-state">Loading...</div>}
        {!loading && sessions.length === 0 && (
          <div className="empty-state">
            <div>No sessions</div>
            <div style={{ fontSize: 12 }}>Create a new session to get started</div>
          </div>
        )}
        {sessions.map((s) => (
          <div key={s.id} className="session-card" onClick={() => onOpenChat(s.id)}>
            <div className={`status-dot ${statusColor(s.status)}`} />
            <div className="info">
              <div className="dir">{s.working_directory || '~'}</div>
              <div className="id">{s.id}</div>
            </div>
            <div className="meta">
              <div>{s.status}</div>
              {(s.total_cost_usd ?? 0) > 0 && <div>${s.total_cost_usd!.toFixed(4)}</div>}
            </div>
            <button className="danger" onClick={(e) => handleDelete(e, s.id)} style={{ padding: '4px 10px', fontSize: 12 }}>
              Delete
            </button>
          </div>
        ))}
      </div>

      {showCreate && (
        <CreateSessionDialog
          settings={settings}
          onCreated={(s) => { setShowCreate(false); onOpenChat(s.id) }}
          onClose={() => setShowCreate(false)}
        />
      )}
    </div>
  )
}

function CreateSessionDialog({ settings, onCreated, onClose }: {
  settings: Settings
  onCreated: (s: Session) => void
  onClose: () => void
}) {
  const [workDir, setWorkDir] = useState(settings.defaultWorkingDirectory)
  const [model, setModel] = useState(settings.defaultModel)
  const [skipPerms, setSkipPerms] = useState(false)
  const [flags, setFlags] = useState('')
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')

  const handleCreate = async () => {
    setCreating(true)
    setError('')
    try {
      const session = await api.createSession(settings, {
        working_directory: workDir || undefined,
        model: model || undefined,
        dangerously_skip_permissions: skipPerms,
        additional_flags: flags ? flags.split(/\s+/).filter(Boolean) : undefined,
      })
      onCreated(session)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed')
      setCreating(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>New Session</h2>
        <div className="field">
          <label>Working Directory</label>
          <input value={workDir} onChange={(e) => setWorkDir(e.target.value)} placeholder="/path/to/project" />
        </div>
        <div className="field">
          <label>Model</label>
          <input value={model} onChange={(e) => setModel(e.target.value)} placeholder="Default" />
        </div>
        <div className="field">
          <label>Extra Flags</label>
          <input value={flags} onChange={(e) => setFlags(e.target.value)} placeholder="--flag1 --flag2" />
        </div>
        <div className="field">
          <div className="checkbox-row">
            <input type="checkbox" checked={skipPerms} onChange={(e) => setSkipPerms(e.target.checked)} id="skip-perms" />
            <label htmlFor="skip-perms" style={{ color: 'var(--text)' }}>Skip permissions</label>
          </div>
        </div>
        {error && <div style={{ color: 'var(--red)', fontSize: 13, marginBottom: 8 }}>{error}</div>}
        <div className="actions">
          <button onClick={onClose}>Cancel</button>
          <button className="primary" onClick={handleCreate} disabled={creating}>
            {creating ? 'Creating...' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  )
}
