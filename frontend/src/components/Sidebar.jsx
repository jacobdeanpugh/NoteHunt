import { Link, useLocation } from 'react-router-dom'

export default function Sidebar() {
  const location = useLocation()

  const isActive = (path) => location.pathname === path

  return (
    <div className="fixed left-0 top-0 h-screen w-12 bg-bg-primary border-r border-border-default flex flex-col items-center py-6 gap-8">
      {/* Logo */}
      <div className="flex items-center justify-center w-8 h-8 text-text-light font-bold text-sm">
        N
      </div>

      {/* Navigation */}
      <nav className="flex flex-col gap-4">
        <SidebarIcon
          path="/"
          icon="🔍"
          active={isActive('/')}
          title="Search"
        />
        <SidebarIcon
          path="/status"
          icon="⚙️"
          active={isActive('/status')}
          title="Index Status"
        />
        <SidebarIcon
          path="/settings"
          icon="⚡"
          active={isActive('/settings')}
          title="Settings"
        />
      </nav>

      {/* Version/Avatar at bottom */}
      <div className="mt-auto mb-6 text-text-muted text-xs">v0.1</div>
    </div>
  )
}

function SidebarIcon({ path, icon, active, title }) {
  return (
    <Link
      to={path}
      title={title}
      className={`flex items-center justify-center w-8 h-8 rounded-full transition-colors ${
        active
          ? 'bg-border-default text-text-light'
          : 'text-text-muted hover:text-text-body'
      }`}
    >
      <span className="text-lg">{icon}</span>
    </Link>
  )
}
