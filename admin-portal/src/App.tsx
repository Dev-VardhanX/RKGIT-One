import { Navigate, Route, Routes } from "react-router-dom"

import { ProtectedRoute } from "@/auth/ProtectedRoute"
import { AppLayout } from "@/components/AppLayout"

import LoginPage from "@/pages/LoginPage"
import DashboardPage from "@/pages/DashboardPage"
import MaterialsPage from "@/pages/MaterialsPage"
import MaterialFormPage from "@/pages/MaterialFormPage"

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={<LoginPage />}
      />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route
            path="/"
            element={<DashboardPage />}
          />

          <Route
            path="/materials"
            element={<MaterialsPage />}
          />

          <Route
            path="/materials/new"
            element={<MaterialFormPage />}
          />

          <Route
            path="/materials/:id/edit"
            element={<MaterialFormPage />}
          />
        </Route>
      </Route>

      <Route
        path="*"
        element={<Navigate to="/" replace />}
      />
    </Routes>
  )
}