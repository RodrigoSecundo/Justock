import React, { useEffect, useState } from "react";
import DialogoReutilizavel from "../common/DialogoReutilizavel";
import { InputText } from "primereact/inputtext";
import { Button } from "primereact/button";
import { InputIcon } from "primereact/inputicon";
import { getCurrentProfile, updateCurrentProfile } from "../../utils/api";
import { notifySuccess, notifyError } from "../../utils/notify";
import "../../styles/pages/dashboard/perfil.css";

function PerfilDialog({ open, onClose }) {
  const [loading, setLoading] = useState(false);
  const [user, setUser] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    async function loadUser() {
      try {
        setLoading(true);
        const data = await getCurrentProfile();
        setUser({
          id: data?.id ?? null,
          nome: data?.name ?? "",
          email: data?.email ?? "",
          numero: data?.numero ?? "",
          senha: "",
        });
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
    if (!user) {
      return;
    }

    setSaving(true);
    updateCurrentProfile(user)
      .then((data) => {
        setUser((prev) => ({
          ...prev,
          nome: data?.name ?? prev?.nome ?? "",
          email: data?.email ?? prev?.email ?? "",
          numero: data?.numero ?? prev?.numero ?? "",
          senha: "",
        }));
        notifySuccess("Informações de perfil atualizadas.");
        if (onClose) onClose();
      })
      .catch((err) => {
        notifyError(err?.message || "Não foi possível atualizar o perfil.");
      })
      .finally(() => {
        setSaving(false);
      });
  };

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
                  <div className="perfil-campo-wrapper perfil-campo-wrapper--clicavel">
                    <InputText
                      id="perfil-nome"
                      value={user?.nome || ""}
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
                  <span className="perfil-hint-opcional" />
                </div>

                <div className="perfil-campo-linha">
                  <label className="perfil-rotulo" htmlFor="perfil-numero">
                    Número:
                  </label>
                  <div className="perfil-campo-wrapper perfil-campo-wrapper--clicavel">
                    <InputText
                      id="perfil-numero"
                      value={user?.numero ?? ""}
                      onChange={(e) => setUser((prev) => ({ ...prev, numero: e.target.value }))}
                      className="perfil-campo"
                      placeholder="(opcional)"
                    />
                  </div>
                  <span className="perfil-hint-opcional">opcional</span>
                </div>

                <div className="perfil-campo-linha">
                  <label className="perfil-rotulo" htmlFor="perfil-senha">
                    Senha:
                  </label>
                  <div className="perfil-campo-wrapper perfil-campo-senha">
                    <InputText
                      id="perfil-senha"
                      type="password"
                      value={user?.senha ?? ""}
                      onChange={(e) => setUser((prev) => ({ ...prev, senha: e.target.value }))}
                      className="perfil-campo"
                      placeholder="Nova senha"
                    />
                  </div>
                  <span className="perfil-hint-opcional">opcional</span>
                </div>
              </div>
            </div>

            <div className="perfil-rodape">
              <Button type="submit" label="SALVAR" className="perfil-botao-salvar" loading={saving} disabled={saving} />
            </div>
          </form>
        )}
      </div>
    </DialogoReutilizavel>
  );
}

export default PerfilDialog;
