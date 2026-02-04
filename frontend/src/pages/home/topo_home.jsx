import { useEffect, useMemo, useState } from "react";
import "../../styles/pages/home/topo_home.css";
import homemsepara from "../../assets/homemsepara.png";
import homemleva from "../../assets/homemleva.png";
import homementrega from "../../assets/homementrega.png";

function TopoHome({ onOpenPlanos }) {
  // Ordem: 1- homemsepara, 2- homemleva, 3- homementrega
  const slides = useMemo(() => [homemsepara, homemleva, homementrega], []);
  const extendedSlides = useMemo(() => [...slides, slides[0]], [slides]);
  const [index, setIndex] = useState(0);
  const [isTransitioning, setIsTransitioning] = useState(true);
  const TRANSITION_MS = 600;

  // Avança a cada 15s e reinicia após o último
  useEffect(() => {
    const id = setInterval(() => {
      setIsTransitioning(true);
      setIndex((prev) => {
        const next = prev + 1;
        return next > slides.length ? slides.length : next;
      });
    }, 15000);
    return () => clearInterval(id);
  }, [slides.length]);

  // Quando a transição para o clone termina, desliga transição, volta ao 0 e religa transição no próximo frame
  useEffect(() => {
    if (!isTransitioning) {
      const id = requestAnimationFrame(() => setIsTransitioning(true));
      return () => cancelAnimationFrame(id);
    }
  }, [isTransitioning]);

  return (
    <section id="home" className="topo">
      <div className="topo-conteudo">
        <h1>Gestão de estoque rápida e eficaz</h1>
        <p>
          Integre seu inventário com as maiores marketplaces do mercado
          <br />e automatize suas operações!
        </p>
        <button onClick={onOpenPlanos}>Comece agora o TESTE GRÁTIS!</button>
      </div>
      <div className="topo-direita">
        <div className="topo-banner">
          <div
            className="topo-slider-track"
            style={{
              // Segurança extra: impede translate além do último slide (clone)
              transform: `translate3d(-${Math.min(index, slides.length) * 100}%, 0, 0)`,
              transition: isTransitioning ? `transform ${TRANSITION_MS}ms ease` : "none",
            }}
            onTransitionEnd={() => {
              if (index === slides.length) {
                // chegou ao clone do primeiro
                setIsTransitioning(false);
                setIndex(0);
              }
            }}
          >
            {extendedSlides.map((src, i) => (
              <div className="topo-slide" key={i}>
                <img
                  src={src}
                  alt={`Banner Justock ${((i % slides.length) + 1).toString()}`}
                  className="topo-banner-img"
                  draggable={false}
                />
              </div>
            ))}
          </div>
        </div>

        <div className="topo-carrossel">
          {slides.map((_, i) => (
            <div
              key={i}
              className={`indicador-carrossel ${i === index % slides.length ? "ativo" : ""}`}
              onClick={() => {
                setIsTransitioning(true);
                setIndex(i);
              }}
              role="button"
              aria-label={`Ir para slide ${i + 1}`}
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  setIsTransitioning(true);
                  setIndex(i);
                }
              }}
            />
          ))}
        </div>
      </div>
    </section>
  );
}

export default TopoHome;
