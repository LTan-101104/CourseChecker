import { useLocation, Link } from "react-router-dom";
import {
  GraduationCap,
  LayoutDashboard,
  Search,
  CircleCheck,
  BookOpen,
  type LucideIcon,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import "./Sidebar.css";

interface NavItem {
  label: string;
  icon: LucideIcon;
  path: string;
}

const mainNav: NavItem[] = [
  { label: "Dashboard", icon: LayoutDashboard, path: "/" },
  { label: "Course Search", icon: Search, path: "/search" },
  { label: "Eligibility Check", icon: CircleCheck, path: "/eligibility" },
];

const recordsNav: NavItem[] = [
  { label: "My Transcript", icon: BookOpen, path: "/transcript" },
];

export function Sidebar() {
  const location = useLocation();
  const { studentId } = useAuth();

  return (
    <aside className="sidebar">
      {/* Header */}
      <div className="sidebar-header">
        <div className="brand-logo">
          <GraduationCap size={16} color="#fff" />
        </div>
        <div className="brand-name">
          <span className="brand-title">CourseChecker</span>
          <span className="brand-subtitle">UMass Amherst</span>
        </div>
      </div>

      {/* Navigation */}
      <nav className="sidebar-content">
        <div className="section-title">Main</div>
        {mainNav.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className={`nav-item ${location.pathname === item.path ? "active" : ""}`}
          >
            <div className="nav-item-left">
              <item.icon size={16} />
              <span>{item.label}</span>
            </div>
          </Link>
        ))}

        <div className="section-title">My Records</div>
        {recordsNav.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className={`nav-item ${location.pathname === item.path ? "active" : ""}`}
          >
            <div className="nav-item-left">
              <item.icon size={16} />
              <span>{item.label}</span>
            </div>
          </Link>
        ))}
      </nav>

      {/* Footer */}
      <div className="sidebar-footer">
        <div className="user-avatar">
          {studentId.charAt(0).toUpperCase()}
        </div>
        <div className="user-info">
          <span className="user-name">John Doe</span>
          <span className="user-email">jdoe@umass.edu</span>
        </div>
      </div>
    </aside>
  );
}
