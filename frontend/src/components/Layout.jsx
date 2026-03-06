import Sidebar from './Sidebar'
import TopBar from './TopBar'

export default function Layout({ children }) {
  return (
    <div className="flex h-screen bg-primary text-body">
      <Sidebar />
      <TopBar />

      {/* Main content area */}
      <main className="ml-12 mt-14 flex-1 overflow-auto">
        <div className="px-8 py-6">
          {children}
        </div>
      </main>
    </div>
  )
}
