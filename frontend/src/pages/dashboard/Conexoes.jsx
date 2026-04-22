import React, { useEffect, useState } from "react";
import "../../styles/pages/dashboard/conexoes.css";
import { notifyError, notifySuccess } from "../../utils/notify";
import { useSrOptimized, srProps } from "../../utils/useA11y";
import { getConexoes, getMercadoLivreAuthUrl, disconnectMercadoLivre, syncMercadoLivre } from "../../utils/api";


const Conexoes = () => {
  const [marketplaces, setMarketplaces] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [syncingMarketplace, setSyncingMarketplace] = useState(null);
  const [disconnectingMarketplace, setDisconnectingMarketplace] = useState(null);
  const srOpt = useSrOptimized();

  const loadMarketplaces = async ({ silent = false } = {}) => {
    if (!silent) {
      setCarregando(true);
    }

    try {
      const data = await getConexoes();
      setMarketplaces(Array.isArray(data?.marketplaces) ? data.marketplaces : []);
    } catch (err) {
      console.error(err);
      setMarketplaces([]);
    } finally {
      if (!silent) {
        setCarregando(false);
      }
    }
  };

  const handleConnect = async (mktName) => {
    if (mktName.toLowerCase() === 'mercado livre') {
      try {
        const data = await getMercadoLivreAuthUrl();
        if (data.url) {
          window.location.href = data.url;
        } else {
          notifyError("Não foi possível obter a URL de autorização.");
        }
      } catch {
        notifyError("Erro ao conectar com servidor do Mercado Livre");
      }
    } else {
      notifyError('Integração com ' + mktName + ' em desenvolvimento.');
    }
  };

  const handleDisconnect = async (mktName) => {
    if (mktName.toLowerCase() === 'mercado livre') {
      try {
        setDisconnectingMarketplace(mktName);
        await disconnectMercadoLivre();
        notifySuccess("Desconectado de " + mktName);
        await loadMarketplaces();
      } catch (e) {
        notifyError(e?.message || "Erro ao desconectar do Mercado Livre");
      } finally {
        setDisconnectingMarketplace(null);
      }
    }
  };

  const handleSync = async (mktName) => {
    if (mktName.toLowerCase() !== 'mercado livre') {
      return;
    }

    try {
      setSyncingMarketplace(mktName);
      await syncMercadoLivre();
      notifySuccess("Pedidos e produtos do Mercado Livre sincronizados.");
      await loadMarketplaces();
    } catch (error) {
      notifyError(error?.message || "Erro ao sincronizar com o Mercado Livre");
    } finally {
      setSyncingMarketplace(null);
    }
  };

  useEffect(() => {
    loadMarketplaces();

    const intervalId = window.setInterval(() => {
      loadMarketplaces({ silent: true });
    }, 30000);

    return () => window.clearInterval(intervalId);
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
                    {mkt.sellerId && <p {...srProps(srOpt, { 'aria-label': `Conta conectada ${mkt.sellerId}` })}><strong>Conta:</strong> {mkt.sellerId}</p>}
                  </div>
                ) : (
                  <div className="conexoes_dados_vazio">Sem dados</div>
                )}
              </div>
              {mkt.connected && (
                <div className="conexoes_acoes">
                  {mkt.name.toLowerCase() === 'mercado livre' && (
                    <button
                      className="conexoes_btn_gerenciar conexoes_btn_sync"
                      onClick={() => handleSync(mkt.name)}
                      disabled={syncingMarketplace === mkt.name}
                      {...srProps(srOpt, { 'aria-label': `Sincronizar pedidos e produtos de ${mkt.name}` })}
                    >
                      {syncingMarketplace === mkt.name ? 'Sincronizando...' : 'Sincronizar pedidos e produtos'}
                    </button>
                  )}
                  <button
                    className="conexoes_btn_gerenciar"
                    onClick={() => handleDisconnect(mkt.name)}
                    disabled={disconnectingMarketplace === mkt.name}
                    {...srProps(srOpt, { 'aria-label': `Desconectar de ${mkt.name}` })}
                  >
                    {disconnectingMarketplace === mkt.name ? 'Desconectando...' : 'Desconectar'}
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default Conexoes;
