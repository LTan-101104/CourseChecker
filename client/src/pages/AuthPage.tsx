import { useMemo, useState } from "react";
import { GraduationCap } from "lucide-react";
import { ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import "./AuthPage.css";

type AuthMode = "login" | "register";

function extractFieldError(error: unknown, field: string): string | null {
  if (error instanceof ApiError) {
    return error.details[field] ?? null;
  }
  return null;
}

export function AuthPage() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<AuthMode>("login");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [loginForm, setLoginForm] = useState({
    email: "",
    password: "",
  });

  const [registerForm, setRegisterForm] = useState({
    studentId: "",
    displayName: "",
    email: "",
    password: "",
  });

  const activeTitle = useMemo(
    () => (mode === "login" ? "Welcome Back" : "Create Your Account"),
    [mode],
  );

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    setFormError(null);

    try {
      if (mode === "login") {
        await login(loginForm);
      } else {
        await register(registerForm);
      }
    } catch (error) {
      if (error instanceof ApiError) {
        const primaryField =
          extractFieldError(error, "email") ??
          extractFieldError(error, "password") ??
          extractFieldError(error, "studentId") ??
          extractFieldError(error, "displayName");
        setFormError(primaryField ?? error.message);
      } else {
        setFormError("Unable to complete authentication right now");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="auth-brand-icon">
            <GraduationCap size={16} color="#fff" />
          </div>
          <div className="auth-brand-text">
            <span className="auth-brand-title">CourseChecker</span>
            <span className="auth-brand-subtitle">UMass Amherst</span>
          </div>
        </div>

        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${mode === "login" ? "active" : ""}`}
            onClick={() => {
              setMode("login");
              setFormError(null);
            }}
          >
            Login
          </button>
          <button
            type="button"
            className={`auth-tab ${mode === "register" ? "active" : ""}`}
            onClick={() => {
              setMode("register");
              setFormError(null);
            }}
          >
            Register
          </button>
        </div>

        <h1 className="auth-title">{activeTitle}</h1>

        <form className="auth-form" onSubmit={handleSubmit}>
          {mode === "register" && (
            <>
              <label className="auth-label" htmlFor="studentId">
                Student ID
              </label>
              <input
                id="studentId"
                className="auth-input"
                placeholder="A12345678"
                value={registerForm.studentId}
                onChange={(event) =>
                  setRegisterForm((prev) => ({
                    ...prev,
                    studentId: event.target.value,
                  }))
                }
                required
              />

              <label className="auth-label" htmlFor="displayName">
                Display Name
              </label>
              <input
                id="displayName"
                className="auth-input"
                placeholder="Jane Doe"
                value={registerForm.displayName}
                onChange={(event) =>
                  setRegisterForm((prev) => ({
                    ...prev,
                    displayName: event.target.value,
                  }))
                }
                required
              />
            </>
          )}

          <label className="auth-label" htmlFor="email">
            Email
          </label>
          <input
            id="email"
            type="email"
            className="auth-input"
            placeholder="name@umass.edu"
            value={mode === "login" ? loginForm.email : registerForm.email}
            onChange={(event) => {
              const value = event.target.value;
              if (mode === "login") {
                setLoginForm((prev) => ({ ...prev, email: value }));
              } else {
                setRegisterForm((prev) => ({ ...prev, email: value }));
              }
            }}
            required
          />

          <label className="auth-label" htmlFor="password">
            Password
          </label>
          <input
            id="password"
            type="password"
            className="auth-input"
            placeholder="At least 8 characters"
            minLength={8}
            value={mode === "login" ? loginForm.password : registerForm.password}
            onChange={(event) => {
              const value = event.target.value;
              if (mode === "login") {
                setLoginForm((prev) => ({ ...prev, password: value }));
              } else {
                setRegisterForm((prev) => ({ ...prev, password: value }));
              }
            }}
            required
          />

          {formError && <p className="auth-error">{formError}</p>}

          <button type="submit" className="auth-submit" disabled={isSubmitting}>
            {isSubmitting
              ? "Submitting..."
              : mode === "login"
                ? "Sign In"
                : "Create Account"}
          </button>
        </form>
      </div>
    </div>
  );
}
