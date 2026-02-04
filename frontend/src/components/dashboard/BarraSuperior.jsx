import React, { useState, useEffect, useRef } from "react";
import "./barra_superior.css";
import { FiHelpCircle, FiBell, FiSearch } from "react-icons/fi";
import { useNavigate } from "react-router-dom";
import SuporteModal from "../suporte/SuporteModal";
import { getAccessibilityPrefs } from "../../utils/accessibility";
import PerfilDialog from "./PerfilDialog";

const BarraSuperior = () => {
  const navigate = useNavigate();
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [openSuporte, setOpenSuporte] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const notificationsCount = notifications.length;
  const [openPerfil, setOpenPerfil] = useState(false);

  const notificationsRef = useRef(null);
  const profileRef = useRef(null);
  const [srOpt, setSrOpt] = useState(() => getAccessibilityPrefs().toggleLeitor === true);

  const toggleNotifications = () => {
    setShowNotifications(prev => !prev);
    setShowProfile(false);
  };
  const toggleProfile = () => {
    setShowProfile(prev => !prev);
    setShowNotifications(false);
  };

  const handleLogout = (e) => {
    e.preventDefault();
    setShowLogoutModal(true);
    setShowProfile(false);
  };

  const handleConfirmLogout = () => {
    window.location.href = '/login';
  };

  const handleCancelLogout = () => {
    setShowLogoutModal(false);
  };

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (notificationsRef.current && !notificationsRef.current.contains(e.target)) {
        setShowNotifications(false);
      }
      if (profileRef.current && !profileRef.current.contains(e.target)) {
        setShowProfile(false);
      }
    };
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        setShowNotifications(false);
        setShowProfile(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  useEffect(() => {
    const onAcc = (e) => {
      const v = e?.detail?.toggleLeitor;
      if (typeof v === 'boolean') setSrOpt(v);
      else setSrOpt(getAccessibilityPrefs().toggleLeitor === true);
    };
    window.addEventListener('jt:accessibility-updated', onAcc);
    onAcc({ detail: getAccessibilityPrefs() });
    return () => window.removeEventListener('jt:accessibility-updated', onAcc);
  }, []);

  return (
    <div className="barra-superior" role="banner" aria-label="Barra superior">
      <div className="secao-pesquisa" role="search" aria-label="Pesquisar">
        <FiSearch aria-hidden className="icone-pesquisa" />
        <input
          type="text"
          id="campo-pesquisa"
          placeholder="Pesquisar"
          aria-label="Pesquisar"
          className="entrada-pesquisa"
        />
      </div>
      <div className="secao-direita">
        <FiHelpCircle
          className="icone-suporte"
          title="Suporte"
          aria-label="Suporte"
          data-srlabel="Suporte"
          onClick={() => setOpenSuporte(true)}
        />
        <div ref={notificationsRef} style={{ position: 'relative', display: 'inline-flex' }}>
          <button
            type="button"
            className={`icone-sino ${notificationsCount > 0 ? 'has-unread' : ''}`}
            aria-label={`Notificações${srOpt ? `, ${notificationsCount} novas` : ''}`}
            data-srlabel="Notificações"
            title="Notificações"
            data-count={notificationsCount}
            aria-haspopup="menu"
            aria-expanded={showNotifications}
            aria-controls="dropdown-notificacoes"
            onClick={toggleNotifications}
          >
            <FiBell />
          </button>
          {showNotifications && (
            <div id="dropdown-notificacoes" className={`dropdown-notificacoes ${showNotifications ? 'show' : ''}`} role="menu" aria-label="Lista de notificações" aria-live={srOpt ? 'polite' : undefined}>
              {notificationsCount > 0 ? (
                <div>
                  <ul style={{ listStyle: 'none', padding: 0, margin: 0 }} role="none">
                    {notifications.map((n, idx) => (
                      <li key={idx} role="menuitem" style={{ padding: '6px 4px', borderBottom: '1px solid #eee' }}>{n}</li>
                    ))}
                  </ul>
                  <button
                    type="button"
                    style={{ marginTop: '8px', width: '100%', background: '#f5f5f5', border: '1px solid #ddd', padding: '6px', borderRadius: '4px', cursor: 'pointer' }}
                    onClick={() => setNotifications([])}
                  >
                    Limpar notificações
                  </button>
                </div>
              ) : (
                <p>Sem notificações no momento</p>
              )}
            </div>
          )}
        </div>

        <div ref={profileRef} style={{ position: 'relative', display: 'inline-flex' }}>
          <button
            type="button"
            className="avatar-usuario"
            onClick={toggleProfile}
            aria-haspopup="menu"
            aria-expanded={showProfile}
            aria-controls="dropdown-perfil"
            aria-label="Abrir menu do usuário"
          >
            U
          </button>
          {showProfile && (
            <div
              id="dropdown-perfil"
              className={`dropdown-perfil ${showProfile ? 'show' : ''}`}
              role="menu"
              aria-label="Menu do usuário"
            >
              <a
                href="#perfil"
                className="dropdown-perfil-link"
                role="menuitem"
                onClick={(e) => {
                  e.preventDefault();
                  setShowProfile(false);
                  setOpenPerfil(true);
                }}
              >
                Perfil
              </a>
              <a
                href="#assinatura"
                className="dropdown-perfil-link"
                role="menuitem"
                onClick={(e) => {
                  e.preventDefault();
                  setShowProfile(false);
                  navigate("/assinatura");
                }}
              >
                Assinatura
              </a>
              <a
                href="#sair"
                className="dropdown-perfil-link"
                role="menuitem"
                onClick={handleLogout}
              >
                Sair
              </a>
            </div>
          )}
        </div>
      </div>
      <SuporteModal open={openSuporte} onClose={() => setOpenSuporte(false)} />
      <PerfilDialog open={openPerfil} onClose={() => setOpenPerfil(false)} />
      {showLogoutModal && (
        <div className="modal-sobreposicao">
          <div className="modal-conteudo">
            <p>Tem certeza que quer sair da conta?</p>
            <div className="modal-botoes">
              <button className="btn-nao" onClick={handleCancelLogout}>NÃO</button>
              <button className="btn-sim" onClick={handleConfirmLogout}>SIM</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BarraSuperior;
