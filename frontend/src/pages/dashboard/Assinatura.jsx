import React, { useEffect, useState } from "react";
import { Card } from "primereact/card";
import { Button } from "primereact/button";
import { Tag } from "primereact/tag";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { getAssinatura } from "../../utils/api";
import { notifyError } from "../../utils/notify";
import "../../styles/pages/dashboard/assinatura.css";
import MastercardLogo from "../../assets/Mastercard-logo.png";

const Assinatura = () => {
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        const data = await getAssinatura();
        setDados(data);
      } catch (err) {
        console.error("Erro ao carregar assinatura", err);
        notifyError("Não foi possível carregar os dados de assinatura.");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  if (loading) {
    return <p>Carregando informações de assinatura...</p>;
  }

  if (!dados) {
    return <p>Nenhuma informação de assinatura encontrada.</p>;
  }

  const { planoAtual, metodoPagamento, historicoCobrancas } = dados;

  const validadeFormatada = (() => {
    if (!metodoPagamento?.validade) return "--/--";
    const d = new Date(metodoPagamento.validade);
    if (Number.isNaN(d.getTime())) return "--/--";
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const yy = String(d.getFullYear()).slice(-2);
    return `${mm}/${yy}`;
  })();

  const proximaCobrancaFormatada = (() => {
    if (!planoAtual?.proximaCobranca) return "--/--";
    const d = new Date(planoAtual.proximaCobranca);
    if (Number.isNaN(d.getTime())) return "--/--";
    const dia = String(d.getDate()).padStart(2, "0");
    const mes = String(d.getMonth() + 1).padStart(2, "0");
    return `${dia}/${mes}`;
  })();

  const valorTemplate = (row) =>
    row.valor != null ? row.valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" }) : "-";

  return (
    <div className="pagina-assinatura">
      <div className="linha-superior-assinatura">
        <Card className="card-plano-atual">
          <div className="card-plano-header">
            <Tag value="PLANO ATUAL" className="tag-plano-atual" />
          </div>
          <div className="card-plano-body">
            <h2 className="plano-nome">{planoAtual?.nome || "-"}</h2>
            <ul className="plano-beneficios">
              <li>Usuários ilimitados</li>
              <li>Integrações ilimitadas</li>
              <li>Produtos ilimitados</li>
              <li>Suporte 24/7</li>
            </ul>
          </div>
          <div className="card-plano-footer">
            <Button label="Alterar plano" className="btn-alterar-plano" />
          </div>
        </Card>

        <Card className="card-metodo-pagamento">
          <div className="metodo-header">
            <h3>Método de Pagamento</h3>
            <Button label="Alterar" className="btn-alterar-metodo" />
          </div>
          <div className="metodo-body">
            <div className="metodo-cartao-mock">
              <div className="cartao-topo">
                <span />
                <img src={MastercardLogo} alt="Bandeira do cartão" className="cartao-logo" />
              </div>
              <div className="cartao-numero">•••• •••• •••• {metodoPagamento?.final || "0000"}</div>
              <div className="cartao-rodape">
                <span className="cartao-label">VALIDADE</span>
                <span className="cartao-validade">{validadeFormatada}</span>
              </div>
            </div>
            <div className="metodo-info-lateral">
              <p className="proxima-cobranca">
                Próxima cobrança:
                <br />
                <strong>{proximaCobrancaFormatada}</strong>
              </p>
              <Button label="Adicionar método alternativo" className="btn-metodo-alternativo" />
              <button type="button" className="btn-cancelar-assinatura">Cancelar Assinatura</button>
            </div>
          </div>
        </Card>
      </div>

      <Card className="card-historico">
        <h3 className="titulo-historico">Histórico de cobranças</h3>
        <DataTable
          value={historicoCobrancas || []}
          size="small"
          stripedRows
          showGridlines
          emptyMessage="Nenhuma cobrança encontrada."
          className="tabela-historico"
        >
          <Column field="data" header="Data" style={{ width: "120px" }} />
          <Column field="descricao" header="Descrição" />
          <Column field="metodo" header="Método" style={{ width: "160px" }} />
          <Column header="Valor" body={valorTemplate} style={{ width: "140px", textAlign: "right" }} />
          <Column field="status" header="Status" style={{ width: "120px" }} />
        </DataTable>
      </Card>
    </div>
  );
};

export default Assinatura;
