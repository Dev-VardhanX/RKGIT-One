import { Navigate, Route, Routes } from "react-router-dom"

import { ProtectedRoute } from "@/auth/ProtectedRoute"
import { AppLayout } from "@/components/AppLayout"

import LoginPage from "@/pages/LoginPage"
import DashboardPage from "@/pages/DashboardPage"
import MaterialsPage from "@/pages/MaterialsPage"
import MaterialFormPage from "@/pages/MaterialFormPage"
import EventsPage from "@/pages/EventsPage";
import EventFormPage from "@/pages/EventFormPage";
import NoticesPage from "@/pages/NoticesPage"
import NoticeFormPage from "@/pages/NoticeFormPage"

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={<LoginPage />}
      />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/materials" element={<MaterialsPage />} />
          <Route path="/materials/new" element={<MaterialFormPage />} />
          <Route path="/materials/:id/edit" element={<MaterialFormPage />} />
          <Route path="events" element={<EventsPage />} />
          <Route path="events/new" element={<EventFormPage />} />
          <Route path="events/:id/edit" element={<EventFormPage />} />
          <Route path="/notices" element={<NoticesPage />} />
          <Route path="/notices/new" element={<NoticeFormPage />} />
          <Route path="/notices/:id/edit" element={<NoticeFormPage />} />
        </Route>
      </Route>

      <Route
        path="*"
        element={<Navigate to="/" replace />}
      />
    </Routes>
  )
}