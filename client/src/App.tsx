import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";
import { Sidebar } from "./components/Sidebar";
import { EligibilityCheckPage } from "./pages/EligibilityCheckPage";
import { TranscriptPage } from "./pages/TranscriptPage";
import { CourseSearchPage } from "./pages/CourseSearchPage";
import { CourseDetailPage } from "./pages/CourseDetailPage";
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

function Placeholder({ title }: { title: string }) {
  return <h1>{title}</h1>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<Placeholder title="Dashboard" />} />
          <Route path="/search" element={<CourseSearchPage />} />
          <Route path="/course/:courseCode" element={<CourseDetailPage />} />
          <Route path="/eligibility" element={<EligibilityCheckPage />} />
          <Route path="/transcript" element={<TranscriptPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
