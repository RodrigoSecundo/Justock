import "../../styles/pages/home/recursos_home.css";
import { FiRefreshCw, FiShoppingCart, FiClock, FiSliders, FiHeadphones } from "react-icons/fi";

const features = [
  {
    title: "Sincronização Automática",
    text: "Sincronize o inventário entre plataformas sem esforço!",
    icon: FiRefreshCw,
  },
  {
    title: "Marketplace Conectada",
    text: "Conecte suas contas e gerencie tudo em apenas um lugar!",
    icon: FiShoppingCart,
  },
  {
    title: "Atualização em Tempo Real",
    text: "Sistema atualiza sozinho o status de pedidos!",
    icon: FiClock,
  },
  {
    title: "Controle sem Limites",
    text: "Atualizações e adições podem ser feitas também de forma manual!",
    icon: FiSliders,
  },
  {
    title: "Suporte de Qualidade",
    text: "Nossa equipe está sempre pronta para melhor atendê-lo.",
    icon: FiHeadphones,
  },
];

function RecursosDisponiveis() {
  return (
    <section id="recursos-home" className="recursos">
      {features.map((f, index) => (
        <div key={index} className="cartao-recurso">
          <div className="icone-recurso">
            {(() => {
              const Icon = f.icon;
              return <Icon className="icone-recurso-svg" aria-hidden="true" />;
            })()}
          </div>
          <div className="texto-recurso">
            <div className="container-titulo-recurso">
              <h3>{f.title}</h3>
              <div className="divisor-recurso"></div>
            </div>
            <p>{f.text}</p>
          </div>
        </div>
      ))}
    </section>
  );
}

export default RecursosDisponiveis;
