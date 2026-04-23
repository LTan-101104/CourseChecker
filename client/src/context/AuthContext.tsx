/* eslint-disable react-refresh/only-export-components */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  fetchCurrentUser,
  login as loginApi,
  register as registerApi,
  type CurrentUser,
  type LoginPayload,
  type RegisterPayload,
} from "../api/auth";
import { ApiError, setAuthTokenProvider } from "../api/client";

const AUTH_TOKEN_STORAGE_KEY = "coursechecker.authToken";

interface AuthContextValue {
  token: string | null;
  user: CurrentUser | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  authError: string | null;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
  bootstrapSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function getStoredToken(): string | null {
  return localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredToken());
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);

  useEffect(() => {
    setAuthTokenProvider(() => token);
    return () => setAuthTokenProvider(null);
  }, [token]);

  const persistToken = useCallback((nextToken: string | null) => {
    setToken(nextToken);
    if (nextToken) {
      localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, nextToken);
    } else {
      localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    }
  }, []);

  const logout = useCallback(() => {
    persistToken(null);
    setUser(null);
    setAuthError(null);
  }, [persistToken]);

  const bootstrapSession = useCallback(async () => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      persistToken(null);
      setUser(null);
      setIsInitializing(false);
      return;
    }

    setIsInitializing(true);
    persistToken(storedToken);

    try {
      const currentUser = await fetchCurrentUser();
      setUser(currentUser);
      setAuthError(null);
    } catch (error) {
      persistToken(null);
      setUser(null);
      if (error instanceof ApiError) {
        setAuthError(error.message);
      } else {
        setAuthError("Unable to restore session");
      }
    } finally {
      setIsInitializing(false);
    }
  }, [persistToken]);

  useEffect(() => {
    void bootstrapSession();
  }, [bootstrapSession]);

  const login = useCallback(
    async (payload: LoginPayload) => {
      const response = await loginApi(payload);
      persistToken(response.token);
      setUser(response.user);
      setAuthError(null);
    },
    [persistToken],
  );

  const register = useCallback(
    async (payload: RegisterPayload) => {
      const response = await registerApi(payload);
      persistToken(response.token);
      setUser(response.user);
      setAuthError(null);
    },
    [persistToken],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token && user),
      isInitializing,
      authError,
      login,
      register,
      logout,
      bootstrapSession,
    }),
    [
      authError,
      bootstrapSession,
      isInitializing,
      login,
      logout,
      register,
      token,
      user,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
