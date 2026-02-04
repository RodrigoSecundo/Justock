import React from "react";
import DialogoReutilizavel from "../../components/common/DialogoReutilizavel";
import { Card } from "primereact/card";
import { Button } from "primereact/button";
import { Tag } from "primereact/tag";
import "../../styles/pages/home/tema_modal_planos.css";

const planos = [
  {
    nome: "Básico",
    preco: "R$49,99",
    usuarios: "1 usuário",
    integracoes: "Integrações ilimitadas",
    produtos: "Produtos ilimitados",
    suporte: "Suporte via chat/email (horário comercial)",
    recomendado: false,
  },
  {
    nome: "Profissional",
    preco: "R$79,99",
    usuarios: "10 usuários",
    integracoes: "Integrações ilimitadas",
    produtos: "Produtos ilimitados",
    suporte: "Suporte via chat/email (prioritário)",
    recomendado: true,
  },
  {
    nome: "Empresarial",
    preco: "R$109,99",
    usuarios: "Usuários ilimitados",
    integracoes: "Integrações ilimitadas",
    produtos: "Produtos ilimitados",
    suporte: "Suporte prioritário 24/7 (SLA reduzido)",
    recomendado: false,
  },
];

const PlanosModal = ({ open, onClose }) => {
  return (
  <DialogoReutilizavel
      visible={open}
      onHide={onClose}
      header="Nossos Planos"
      position="top"
      width="min(1040px, 92vw)"
  maskClassName="mascara-dialogo-planos"
  className="dialogo-planos"
      dismissableMask
    >
      <div className="grid grade-planos">
        {planos.map((plano) => (
          <div key={plano.nome} className="col-12 md:col-4">
            <Card className={`cartao-plano ${plano.recomendado ? 'recomendado' : ''}`}>
              {plano.recomendado && (
                <Tag value="RECOMENDADO" className="badge-recomendado" />
              )}
              <div className="cabecalho-plano">
                <h2 className="nome-plano">{plano.nome}</h2>
              </div>
              <div className="preco-plano">
                {plano.preco}
                <span className="periodo-preco"> /mês</span>
              </div>
              <div className="texto-teste-gratis">1 mês grátis</div>
              <ul className="beneficios-plano">
                <li><span className="icone-check">✓</span>{plano.usuarios}</li>
                <li><span className="icone-check">✓</span>{plano.integracoes}</li>
                <li><span className="icone-check">✓</span>{plano.produtos}</li>
                <li><span className="icone-check">✓</span>{plano.suporte}</li>
              </ul>
              <div className="acoes-plano">
                <Button label="Escolher Plano" className="botao-plano w-full" />
              </div>
            </Card>
          </div>
        ))}
      </div>
  </DialogoReutilizavel>
  );
};

export default PlanosModal;
