import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { AuthProvider } from './context/AuthContext.tsx'
import { CompletedCoursesProvider } from './context/CompletedCoursesContext.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <CompletedCoursesProvider>
        <App />
      </CompletedCoursesProvider>
    </AuthProvider>
  </StrictMode>,
)
