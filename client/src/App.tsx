import { BrowserRouter, Routes, Route, Outlet, Navigate } from "react-router-dom";
import { Sidebar } from "./components/Sidebar";
import { EligibilityCheckPage } from "./pages/EligibilityCheckPage";
import { DashboardPage } from "./pages/DashboardPage";
import { TranscriptPage } from "./pages/TranscriptPage";
import { CourseSearchPage } from "./pages/CourseSearchPage";
import { CourseDetailPage } from "./pages/CourseDetailPage";
import { AuthPage } from "./pages/AuthPage";
import { useAuth } from "./context/AuthContext";
import "./App.css";

function AppLayout() {
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}

function FullPageMessage({ message }: { message: string }) {
  return (
    <div className="full-page-message">
      <p>{message}</p>
    </div>
  );
}

function RequireAuth() {
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) {
    return <FullPageMessage message="Restoring your session..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace />;
  }

  return <Outlet />;
}

function PublicOnlyAuthRoute() {
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) {
    return <FullPageMessage message="Restoring your session..." />;
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <AuthPage />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/auth" element={<PublicOnlyAuthRoute />} />
        <Route element={<AppLayout />}>
          <Route path="/search" element={<CourseSearchPage />} />
          <Route path="/course/:courseCode" element={<CourseDetailPage />} />
          <Route element={<RequireAuth />}>
            <Route index element={<DashboardPage />} />
            <Route path="/eligibility" element={<EligibilityCheckPage />} />
            <Route path="/transcript" element={<TranscriptPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
