import React, { useEffect, useState } from "react";
import "../../styles/pages/dashboard/conexoes.css";
import { notifyError } from "../../utils/notify";
import { useSrOptimized, srProps } from "../../utils/useA11y";
import { getConexoes, getMercadoLivreStatus, getMercadoLivreAuthUrl, disconnectMercadoLivre } from "../../utils/api";


const Conexoes = () => {
  const [marketplaces, setMarketplaces] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const srOpt = useSrOptimized();

  const handleConnect = async (mktName) => {
    if (mktName.toLowerCase() === 'mercado livre') {
      try {
        const data = await getMercadoLivreAuthUrl();
        if (data.url) {
          window.location.href = data.url;
        } else {
          notifyError("Não foi possível obter a URL de autorização.");
        }
      } catch (error) {
        notifyError("Erro ao conectar com servidor do Mercado Livre");
      }
    } else {
      notifyError('Integração com ' + mktName + ' em desenvolvimento.');
    }
  };

  const handleDisconnect = async (mktName) => {
    if (mktName.toLowerCase() === 'mercado livre') {
      try {
        await disconnectMercadoLivre();
        notifySuccess("Desconectado de " + mktName);
        const mkts = marketplaces.map(m => m.name === mktName ? { ...m, connected: false } : m);
        setMarketplaces(mkts);
      } catch (e) {
        notifyError(e?.message || "Erro ao desconectar do Mercado Livre");
      }
    }
  };

  useEffect(() => {
    setCarregando(true);
    getConexoes()
      .then(async (data) => {
        let mkts = [];
        if (data && Array.isArray(data.marketplaces)) {
          mkts = data.marketplaces;
        }

        const realStatus = await getMercadoLivreStatus();
        mkts = mkts.map(m => {
          if (m.name.toLowerCase() === 'mercado livre') {
            return { ...m, connected: realStatus?.connected || false };
          }
          return m;
        });

        setMarketplaces(mkts);
      })
      .catch((err) => {
        console.error(err);
        setMarketplaces([]);
      })
      .finally(() => setCarregando(false));
  }, []);

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
                  <button className="conexoes_status conexoes_status_conectar" style={{ border: 'none', cursor: 'pointer' }} onClick={() => handleConnect(mkt.name)} {...srProps(srOpt, { 'aria-label': `Conectar ${mkt.name}` })}>Conectar</button>
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
              {mkt.connected && <button className="conexoes_btn_gerenciar" onClick={() => handleDisconnect(mkt.name)} {...srProps(srOpt, { 'aria-label': `Desconectar de ${mkt.name}` })}>Desconectar</button>}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default Conexoes;
