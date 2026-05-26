import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/notifications.css'
import './styles/accessibility.css'
import 'primereact/resources/themes/lara-light-blue/theme.css'
import 'primereact/resources/primereact.min.css'
import 'primeicons/primeicons.css'
import 'primeflex/primeflex.css'
import App from './App.jsx'
import { initAppearance } from './utils/appearance.js'
import { initAccessibility } from './utils/accessibility.js'
import { initializeAuthSession } from './utils/auth.js'

initAppearance()
initAccessibility()
initializeAuthSession()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
