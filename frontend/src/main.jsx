import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/dashboard-theme.css'
import './styles/notifications.css'
import './styles/accessibility.css'
import './styles/components/tema_tabelas_primereact.css'
import 'primereact/resources/themes/lara-light-blue/theme.css'
import 'primereact/resources/primereact.min.css'
import 'primeicons/primeicons.css'
import 'primeflex/primeflex.css'
import App from './App.jsx'
// Backend agora vem do json-server (db.json)
import { initAppearance } from './utils/appearance.js'
import { initAccessibility } from './utils/accessibility.js'

initAppearance()
initAccessibility()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
