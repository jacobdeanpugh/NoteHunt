import { useEffect, useState } from 'react'

export default function TopBar() {
  const [username, setUsername] = useState('User')

  useEffect(() => {
    // Get system username from environment variables
    const user = import.meta.env.VITE_USER || 'User'
    setUsername(user.charAt(0).toUpperCase() + user.slice(1))
  }, [])

  return (
    <header className="fixed top-0 left-12 right-0 h-14 bg-[#0e0e0e] border-b border-[#1e1e1e] flex items-center justify-between px-8 z-10">
      {/* Left: Logo/Title */}
      <div className="text-[#f5f5f5] font-medium text-sm">NoteHunt</div>

      {/* Right: Username and Avatar */}
      <div className="flex items-center gap-4">
        <span className="text-[#aaaaaa] text-sm">{username}</span>
        <div className="w-8 h-8 rounded-full bg-[#141414] flex items-center justify-center text-[#f5f5f5] text-xs font-bold">
          {username.charAt(0).toUpperCase()}
        </div>
      </div>
    </header>
  )
}
