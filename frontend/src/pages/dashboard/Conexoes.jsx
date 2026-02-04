import React, { useEffect, useState } from "react";
import "../../styles/pages/dashboard/conexoes.css";
import { notifyError } from "../../utils/notify";
import { useSrOptimized, srProps } from "../../utils/useA11y";
import { getConexoes } from "../../utils/api";


const Conexoes = () => {
  const [marketplaces, setMarketplaces] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const srOpt = useSrOptimized();

  useEffect(() => {
    setCarregando(true);
    getConexoes()
      .then((data) => {
        if (data && Array.isArray(data.marketplaces)) {
          setMarketplaces(data.marketplaces);
        } else {
          setMarketplaces([]);
        }
      })
      .catch(() => {
        setMarketplaces([]);
      })
      .finally(() => setCarregando(false));
  }, []);

  // Exibiremos nomes em caixa ao invés de logos (restrições de uso de marca encontradas nas documentações oficiais)
  
  return (
    <div className="conexoes_pagina" {...srProps(srOpt, { role: 'main', 'aria-label': 'Conexões com marketplaces' })}>
          <div className="conexoes_cabecalho">
            <h2>Conexões com Marketplaces</h2>
          </div>

          <div className="conexoes_cards" {...srProps(srOpt, { role: 'region', 'aria-label': 'Lista de conexões' })}>
            {carregando ? (
              <div>Carregando...</div>
            ) : (
              marketplaces.map((mkt) => (
                <div className="conexoes_card" key={mkt.id} {...srProps(srOpt, { role: 'group', 'aria-label': `${mkt.name}. ${mkt.connected ? 'Conectado' : 'Desconectado'}` })}>
                  <div className="conexoes_logo_text" aria-hidden="true">{mkt.name}</div>
                  <div className="conexoes_card_corpo">
                    {mkt.connected ? (
                      <div className="conexoes_status conexoes_status_conectado" {...srProps(srOpt, { 'aria-label': 'Status: Conectado' })}>Conectado</div>
                    ) : (
                      <button className="conexoes_status conexoes_status_conectar" style={{border: 'none', cursor: 'pointer'}} onClick={() => notifyError('Funcionalidade de conectar ainda não implementada.')} {...srProps(srOpt, { 'aria-label': `Conectar ${mkt.name}` })}>Conectar</button>
                    )}
                    {mkt.connected ? (
                      <div className="conexoes_dados">
                        <p {...srProps(srOpt, { 'aria-label': `Total de vendas ${mkt.totalVendas}` })}><strong>Total de vendas:</strong> {mkt.totalVendas}</p>
                        <p {...srProps(srOpt, { 'aria-label': `Pedidos ativos ${mkt.pedidosAtivos}` })}><strong>Pedidos ativos:</strong> {mkt.pedidosAtivos}</p>
                        <p {...srProps(srOpt, { 'aria-label': `Quantidade em inventário ${mkt.totalInventario}` })}><strong>Quant. Inventário:</strong> {mkt.totalInventario}</p>
                      </div>
                    ) : (
                      <div className="conexoes_dados_vazio">Sem dados</div>
                    )}
                  </div>
                  {mkt.connected && <button className="conexoes_btn_gerenciar" {...srProps(srOpt, { 'aria-label': `Gerenciar conexão de ${mkt.name}` })}>Gerenciar</button>}
                </div>
              ))
            )}
          </div>
    </div>
  );
};

export default Conexoes;
