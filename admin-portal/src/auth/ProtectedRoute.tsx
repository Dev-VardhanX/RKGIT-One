import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/auth/AuthProvider";

export function ProtectedRoute() {
  const { session, loading, isAdmin } = useAuth();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-sm text-muted-foreground">
          Checking your session...
        </p>
      </div>
    );
  }

  if (!session) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3">
        <h1 className="text-xl font-semibold">Access denied</h1>
        <p className="text-sm text-muted-foreground">
          Your account is not authorized to access this portal.
        </p>
      </div>
    );
  }

  return <Outlet />;
}