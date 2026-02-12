import { useState } from 'react'
import type { Settings } from '../lib/types'

interface Props {
  settings: Settings
  onSave: (s: Settings) => void
  onBack: () => void
}

export default function SettingsPage({ settings, onSave, onBack }: Props) {
  const [apiUrl, setApiUrl] = useState(settings.apiUrl)
  const [authToken, setAuthToken] = useState(settings.authToken)
  const [defaultModel, setDefaultModel] = useState(settings.defaultModel)
  const [defaultWorkingDirectory, setDefaultWorkingDirectory] = useState(settings.defaultWorkingDirectory)

  const handleSave = () => {
    onSave({ apiUrl, authToken, defaultModel, defaultWorkingDirectory })
  }

  return (
    <div className="settings-page">
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
        <button onClick={onBack}>Back</button>
        <h2 style={{ margin: 0 }}>Settings</h2>
      </div>

      <div className="field">
        <label>API URL</label>
        <input value={apiUrl} onChange={(e) => setApiUrl(e.target.value)} placeholder="http://127.0.0.1:8080" />
      </div>
      <div className="field">
        <label>Auth Token</label>
        <input type="password" value={authToken} onChange={(e) => setAuthToken(e.target.value)} placeholder="Leave empty if auth disabled" />
      </div>
      <div className="field">
        <label>Default Model</label>
        <input value={defaultModel} onChange={(e) => setDefaultModel(e.target.value)} placeholder="e.g. claude-opus-4-6" />
      </div>
      <div className="field">
        <label>Default Working Directory</label>
        <input value={defaultWorkingDirectory} onChange={(e) => setDefaultWorkingDirectory(e.target.value)} placeholder="/path/to/project" />
      </div>

      <div className="actions">
        <button onClick={onBack}>Cancel</button>
        <button className="primary" onClick={handleSave}>Save</button>
      </div>
    </div>
  )
}
