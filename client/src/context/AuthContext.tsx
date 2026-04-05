import { createContext, useContext, type ReactNode } from "react";

interface AuthContextValue {
  studentId: string;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({
  studentId,
  children,
}: {
  studentId: string;
  children: ReactNode;
}) {
  return (
    <AuthContext.Provider value={{ studentId }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
