import { useEffect, useState } from 'react'

export default function TopBar() {
  const [username, setUsername] = useState('User')

  useEffect(() => {
    // Get system username from environment variables
    const user = import.meta.env.VITE_USER || 'User'
    setUsername(user.charAt(0).toUpperCase() + user.slice(1))
  }, [])

  return (
    <header className="fixed top-0 left-12 right-0 h-14 bg-bg-primary border-b border-border-default flex items-center justify-between px-8 z-10">
      {/* Left: Logo/Title */}
      <div className="text-text-light font-medium text-sm">NoteHunt</div>

      {/* Right: Username and Avatar */}
      <div className="flex items-center gap-4">
        <span className="text-text-body text-sm">{username}</span>
        <div className="w-8 h-8 rounded-full bg-border-default flex items-center justify-center text-text-light text-xs font-bold">
          {username.charAt(0).toUpperCase()}
        </div>
      </div>
    </header>
  )
}
