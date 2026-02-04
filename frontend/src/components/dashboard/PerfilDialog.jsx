import React, { useEffect, useState } from "react";
import DialogoReutilizavel from "../common/DialogoReutilizavel";
import { InputText } from "primereact/inputtext";
import { Button } from "primereact/button";
import { Password } from "primereact/password";
import { InputIcon } from "primereact/inputicon";
import { getUsuario } from "../../utils/api";
import { notifySuccess, notifyError } from "../../utils/notify";
import "../../styles/pages/dashboard/perfil.css";

const DEFAULT_USER_ID = 1;

function PerfilDialog({ open, onClose }) {
  const [loading, setLoading] = useState(false);
  const [user, setUser] = useState(null);
  const [editing, setEditing] = useState({ nome: false, numero: false });

  useEffect(() => {
    if (!open) return;
    async function loadUser() {
      try {
        setLoading(true);
        const stored = window.localStorage.getItem("jt:user");
        const parsed = stored ? JSON.parse(stored) : null;
        const id = parsed?.id ?? DEFAULT_USER_ID;
        const data = await getUsuario(id);
        setUser(data);
      } catch (err) {
        console.error("Erro ao carregar usuário:", err);
        notifyError("Não foi possível carregar os dados do perfil.");
      } finally {
        setLoading(false);
      }
    }
    loadUser();
  }, [open]);

  const handleSubmit = (e) => {
    e.preventDefault();
    // No momento não persiste no backend, apenas feedback visual
    notifySuccess("Informações de perfil atualizadas.");
    if (onClose) onClose();
  };

  const senhaMascarada = user?.senha ? "•".repeat(String(user.senha).length) : "";

  return (
    <DialogoReutilizavel
      visible={open}
      onHide={onClose}
      header="Perfil do usuário"
      position="top"
      width="min(900px, 96vw)"
      className="dialogo-perfil"
      contentClassName="conteudo-dialogo-perfil"
    >
      <div className="perfil-container">
        {loading ? (
          <p>Carregando dados do perfil...</p>
        ) : (
          <form className="perfil-formulario" onSubmit={handleSubmit}>
            <div className="perfil-topo">
              <div className="perfil-foto-wrapper">
                <div className="perfil-foto" aria-label="Foto de perfil atual">
                  <span className="perfil-foto-inicial" aria-hidden="true">U</span>
                </div>
                <Button
                  type="button"
                  label="ALTERAR FOTO"
                  className="p-button-sm perfil-botao-foto"
                  disabled
                />
                <small className="perfil-foto-hint">(opcional)</small>
              </div>

              <div className="perfil-dados-basicos">
                <div className="perfil-campo-linha">
                  <label className="perfil-rotulo" htmlFor="perfil-nome">
                    Nome Completo:
                  </label>
                  <div
                    className="perfil-campo-wrapper perfil-campo-wrapper--clicavel"
                    onClick={() => setEditing((prev) => ({ ...prev, nome: true }))}
                  >
                    <InputText
                      id="perfil-nome"
                      value={user?.nome || ""}
                      readOnly={!editing.nome}
                      className="perfil-campo"
                      onChange={(e) => setUser((prev) => ({ ...prev, nome: e.target.value }))}
                    />
                  </div>
                  <span className="perfil-obrigatorio">*</span>
                </div>

                <div className="perfil-campo-linha">
                  <label className="perfil-rotulo" htmlFor="perfil-email">
                    E-mail:
                  </label>
                  <div className="perfil-campo-wrapper perfil-campo-wrapper--bloqueado">
                    <InputText
                      id="perfil-email"
                      value={user?.email || ""}
                      readOnly
                      className="perfil-campo perfil-campo--bloqueado"
                    />
                    <InputIcon className="pi pi-lock perfil-icone-cadeado" aria-hidden="true" />
                  </div>
                </div>

                <div className="perfil-campo-linha">
                  <label className="perfil-rotulo" htmlFor="perfil-numero">
                    Número:
                  </label>
                  <div
                    className="perfil-campo-wrapper perfil-campo-wrapper--clicavel"
                    onClick={() => setEditing((prev) => ({ ...prev, numero: true }))}
                  >
                    <InputText
                      id="perfil-numero"
                      value={user?.numero ?? ""}
                      readOnly={!editing.numero}
                      onChange={(e) => setUser((prev) => ({ ...prev, numero: e.target.value }))}
                      className="perfil-campo"
                      placeholder="(opcional)"
                    />
                  </div>
                </div>

                <div className="perfil-campo-linha">
                  <label className="perfil-rotulo" htmlFor="perfil-senha">
                    Senha:
                  </label>
                  <div className="perfil-campo-wrapper">
                    <Password
                      id="perfil-senha"
                      value={senhaMascarada}
                      readOnly
                      toggleMask={false}
                      feedback={false}
                      className="perfil-campo-senha"
                      inputClassName="perfil-campo"
                    />
                  </div>
                  <span className="perfil-obrigatorio">*</span>
                </div>
              </div>
            </div>

            <div className="perfil-rodape">
              <Button type="submit" label="SALVAR" className="perfil-botao-salvar" />
            </div>
          </form>
        )}
      </div>
    </DialogoReutilizavel>
  );
}

export default PerfilDialog;
