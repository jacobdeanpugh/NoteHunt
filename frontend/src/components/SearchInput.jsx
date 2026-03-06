import { useState } from 'react'
import Input from './ui/Input'

const PROMPT_STARTERS = [
  'notes about project deadlines',
  'everything tagged #ideas',
  'files I edited last week',
]

export default function SearchInput({ onSearch, loading = false }) {
  const [query, setQuery] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    if (query.trim()) {
      onSearch(query.trim())
    }
  }

  const handlePromptClick = (starter) => {
    setQuery(starter)
  }

  return (
    <div className="mb-12">
      {/* Heading */}
      <h2 className="text-base text-text-body mb-4">I want to find...</h2>

      {/* Prompt starters */}
      <div className="space-y-2 mb-6">
        {PROMPT_STARTERS.map((starter, idx) => (
          <button
            key={idx}
            onClick={() => handlePromptClick(starter)}
            className="text-sm text-text-muted hover:text-text-body transition-colors block"
          >
            I want to find... <span className="text-text-body">{starter}</span>
          </button>
        ))}
      </div>

      {/* Search input form */}
      <form onSubmit={handleSubmit} className="relative">
        <Input
          type="text"
          placeholder="I want to find..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="w-full pr-12"
        />
        <button
          type="submit"
          disabled={loading || !query.trim()}
          className="absolute right-3 top-1/2 transform -translate-y-1/2 text-text-muted hover:text-text-body disabled:opacity-50 text-lg"
        >
          →
        </button>
      </form>
    </div>
  )
}
