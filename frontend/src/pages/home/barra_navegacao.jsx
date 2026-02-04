import "../../styles/pages/home/barra_navegacao.css";
import logo from "../../assets/logo.png";
import { Link, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import SuporteModal from "../../components/suporte/SuporteModal";

function BarraNavegacao({ onOpenPlanos, onOpenSuporte }) {
  const location = useLocation();
  const [activeLink, setActiveLink] = useState("#home");
  const [openSuporte, setOpenSuporte] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      const sections = ["#recursos-home", "#sobre-nos"];
      const scrollPosition = window.scrollY;
      const windowHeight = window.innerHeight;
      const docHeight = document.documentElement.scrollHeight;

      if (scrollPosition + windowHeight >= docHeight - 5) {
        setActiveLink("#sobre-nos");
        return;
      }

      for (let i = sections.length - 1; i >= 0; i--) {
        const sectionId = sections[i];
        const section = document.querySelector(`#${sectionId}`);

        if (section) {
          const sectionTop = section.offsetTop - 100;
          if (scrollPosition >= sectionTop) {
            setActiveLink(`#${sectionId}`);
            return;
          }
        }
      }

    if (scrollPosition < 500) {
      setActiveLink("#home");
    }
    };

    window.addEventListener("scroll", handleScroll);
    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  const handleScroll = (event, targetId) => {
    event.preventDefault();
    if (targetId === "#") {
      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    } else {
      const targetElement = document.querySelector(targetId);
      if (targetElement) {
        targetElement.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      }
    }
  };

  const createHandler = (callback) => (event) => {
    event.preventDefault();
    if (callback) {
      callback();
    }
  };

  const handleLogoClick = (e) => {
    if (location.pathname === "/") {
      e.preventDefault();
      window.location.reload();
    }
  };

  return (
    <nav className="navegacao">
      <div className="container-navegacao">
        <div className="logo-navegacao">
          <Link to="/" onClick={handleLogoClick}>
            <img src={logo} alt="Logo JusTock" />
          </Link>
        </div>
        <ul className="links_navegacao">
          <li>
            <a
              href="#home"
              onClick={(e) => handleScroll(e, "#home")}
              className={activeLink === "#home" ? "active" : ""}
            >
              Home
            </a>
          </li>
          <li>
            <a
              href="#recursos-home"
              onClick={(e) => handleScroll(e, "#recursos-home")}
              className={activeLink === "#recursos-home" ? "active" : ""}
            >
              Funcionalidades
            </a>
          </li>
          <li>
            <a href="#planos" onClick={createHandler(onOpenPlanos)}>
              Planos
            </a>
          </li>
          <li>
            <a
              href="#sobre-nos"
              onClick={(e) => handleScroll(e, "#sobre-nos")}
              className={activeLink === "#sobre-nos" ? "active" : ""}
            >
              Sobre
            </a>
          </li>
          
          <li>
            <a
              href="#suporte"
              onClick={(e) => {
                e.preventDefault();
                setOpenSuporte(true);
                if (onOpenSuporte) onOpenSuporte();
              }}
            >
              Suporte
            </a>
          </li>
        </ul>
        <Link to="/login">
          <button className="login_navegacao">Login</button>
        </Link>
      </div>
      <SuporteModal open={openSuporte} onClose={() => setOpenSuporte(false)} />
    </nav>
  );
}

export default BarraNavegacao;
