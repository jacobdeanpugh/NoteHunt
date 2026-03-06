import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import SearchScreen from './screens/SearchScreen'
import IndexStatusScreen from './screens/IndexStatusScreen'
import SettingsScreen from './screens/SettingsScreen'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <Layout>
              <SearchScreen />
            </Layout>
          }
        />
        <Route
          path="/status"
          element={
            <Layout>
              <IndexStatusScreen />
            </Layout>
          }
        />
        <Route
          path="/settings"
          element={
            <Layout>
              <SettingsScreen />
            </Layout>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
