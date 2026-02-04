import DialogoReutilizavel from "../common/DialogoReutilizavel";
import "../../styles/components/tema_modal_suporte.css";
import { InputTextarea } from "primereact/inputtextarea";
import { Button } from "primereact/button";
import { Divider } from "primereact/divider";
import React, { useState } from "react";

function SuporteModal({ open, onClose }) {
  const [mensagem, setMensagem] = useState("");

  // Enviar não deve fechar o modal no momento; manter sem ação.
  const handleSubmit = (event) => {
    event.preventDefault();
    // Futuro: enviar mensagem ao suporte
  };

  return (
    <DialogoReutilizavel
      visible={open}
      onHide={onClose}
      position="bottom-right"
      header={
        <div className="flex flex-column">
          <span className="font-bold">Suporte JusTock</span>
          <small className="text-600">Equipe disponível para ajudar</small>
        </div>
      }
      width="420px"
      className="dialogo-suporte"
      maskClassName="mascara-dialogo-suporte"
      contentClassName="conteudo-dialogo-suporte"
    >
      <div className="p-3">
        <div className="mb-3 text-center cabecalho-suporte">
          <div className="titulo-suporte text-lg font-semibold">Olá!</div>
          <div className="subtitulo-suporte">Como podemos ajudar hoje?</div>
        </div>
        <form onSubmit={handleSubmit} className="flex flex-column gap-2 corpo-chat-suporte">
          <div className="linha-anexar">
            <Button type="button" label="Anexar" icon="pi pi-paperclip" outlined className="botao-anexar" />
          </div>
          <InputTextarea
            className="textarea-suporte"
            placeholder="Digite sua mensagem aqui..."
            rows={4}
            value={mensagem}
            onChange={(e) => setMensagem(e.target.value)}
            /* Sem autoResize: mantém altura e ativa scroll ao exceder */
          />
          <div className="flex justify-content-end gap-1 acoes-chat">
            {/* Usar ícone X do Dialog */}
            <Button type="button" label="Enviar" icon="pi pi-send" />
          </div>
        </form>
      </div>
    </DialogoReutilizavel>
  );
}

export default SuporteModal;
