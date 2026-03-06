import { useState } from 'react'
import Input from '../components/ui/Input'
import Button from '../components/ui/Button'

export default function SettingsScreen() {
  const [formData, setFormData] = useState({
    directoryPath: '/Users/jacob/Notes',
    indexPath: '%APPDATA%/NoteQuest/index',
    batchSize: 50,
    extensions: {
      txt: true,
      md: false,
      rst: false,
      org: false,
    },
  })
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
    setSuccess(false)
  }

  const handleExtensionChange = (ext) => {
    setFormData((prev) => ({
      ...prev,
      extensions: {
        ...prev.extensions,
        [ext]: !prev.extensions[ext],
      },
    }))
  }

  const handleSave = async () => {
    try {
      setError(null)
      // TODO: POST to /config endpoint (Phase 5)
      // await saveConfig(formData)
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
    } catch (err) {
      setError('Failed to save settings. Please try again.')
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-3xl font-light mb-8">Settings</h1>

      {/* Directory Settings Section */}
      <div className="mb-8">
        <h2 className="text-xs uppercase text-muted font-medium tracking-wide border-b border-dark pb-2 mb-6">
          Directory Settings
        </h2>

        <div className="space-y-6">
          {/* Directory Path */}
          <div>
            <label className="block text-sm text-body mb-2">
              Directory Path
            </label>
            <Input
              value={formData.directoryPath}
              onChange={(e) =>
                handleInputChange('directoryPath', e.target.value)
              }
            />
          </div>

          {/* Index Path */}
          <div>
            <label className="block text-sm text-body mb-2">
              Index Path
            </label>
            <Input
              value={formData.indexPath}
              onChange={(e) => handleInputChange('indexPath', e.target.value)}
            />
          </div>
        </div>
      </div>

      {/* File Extensions Section */}
      <div className="mb-8">
        <h2 className="text-xs uppercase text-muted font-medium tracking-wide border-b border-dark pb-2 mb-6">
          File Extensions
        </h2>

        <div className="space-y-3">
          {Object.entries(formData.extensions).map(([ext, checked]) => (
            <label
              key={ext}
              className="flex items-center gap-3 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={checked}
                onChange={() => handleExtensionChange(ext)}
                className="w-4 h-4 bg-surface border border-dark rounded cursor-pointer"
              />
              <span className="font-mono text-sm text-body">.{ext}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Indexing Section */}
      <div className="mb-8">
        <h2 className="text-xs uppercase text-muted font-medium tracking-wide border-b border-dark pb-2 mb-6">
          Indexing
        </h2>

        <div>
          <label className="block text-sm text-body mb-2">
            Batch Size (files per batch)
          </label>
          <Input
            type="number"
            value={formData.batchSize}
            onChange={(e) =>
              handleInputChange('batchSize', parseInt(e.target.value))
            }
          />
        </div>
      </div>

      {/* Messages */}
      {error && (
        <div className="bg-status-error bg-opacity-10 border border-status-error text-status-error p-3 rounded mb-6">
          {error}
        </div>
      )}
      {success && (
        <div className="bg-status-complete bg-opacity-10 border border-status-complete text-status-complete p-3 rounded mb-6">
          Settings saved successfully (Phase 5 feature)
        </div>
      )}

      {/* Save Button */}
      <div className="flex justify-end">
        <Button onClick={handleSave} variant="primary">
          Save Settings
        </Button>
      </div>

      {/* Note */}
      <p className="text-xs text-muted mt-6 italic">
        Note: Settings are read-only in this version. Write functionality will
        be added in Phase 5.
      </p>
    </div>
  )
}
