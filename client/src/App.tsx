import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";
import { Sidebar } from "./components/Sidebar";
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
          <Route path="/search" element={<Placeholder title="Course Search" />} />
          <Route path="/eligibility" element={<Placeholder title="Eligibility Check" />} />
          <Route path="/transcript" element={<Placeholder title="My Transcript" />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
