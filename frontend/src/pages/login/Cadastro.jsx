import "../../styles/pages/login/cadastro.css";
import logo from "../../assets/logo_preto_baix.png";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useState } from "react";
import { isAuthenticated, register } from "../../utils/auth";

function Cadastro() {
  const navigate = useNavigate();
  const location = useLocation();
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(false);

  if (isAuthenticated()) {
    return <Navigate to="/dashboard" replace />;
  }

  const planoSelecionado = location.state?.plano;

  const handleBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }

    navigate("/");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErro("");
    setCarregando(true);

    try {
      await register({ nome, email, senha });
      navigate("/login", {
        replace: true,
        state: {
          email,
          registrationSuccess: "Conta criada com sucesso. Faça seu login para continuar.",
        },
      });
    } catch (err) {
      setErro(err?.message || "Não foi possível criar a conta.");
    } finally {
      setCarregando(false);
    }
  };

  return (
    <div className="container-login">
      <button
        className="seta-voltar"
        aria-label="Voltar para a página anterior"
        onClick={handleBack}
      >
        <svg width="28" height="28" viewBox="0 0 24 24" aria-hidden="true" focusable="false" xmlns="http://www.w3.org/2000/svg">
          <path d="M15 6 L9 12 L15 18" stroke="currentColor" strokeWidth="2.25" strokeLinecap="round" strokeLinejoin="round" fill="none" />
        </svg>
      </button>

      <div className="cartao-login">
        <div className="logo-login">
          <img src={logo} alt="Logo JusTock" />
        </div>

        <form className="formulario-login" onSubmit={handleSubmit}>
          <div className="grupo-formulario">
            <input
              type="text"
              id="nome"
              placeholder={planoSelecionado ? `Nome para o plano ${planoSelecionado}` : "Nome completo"}
              value={nome}
              onChange={(event) => setNome(event.target.value)}
              required
            />
          </div>

          <div className="grupo-formulario">
            <input
              type="email"
              id="email"
              placeholder="E-mail"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>

          <div className="grupo-formulario">
            <input
              type="password"
              id="senha"
              placeholder="Senha"
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
              required
            />
          </div>

          {erro && (
            <div role="alert" className="mensagem-erro-login">
              {erro}
            </div>
          )}

          <button type="submit" className="botao-login" disabled={carregando}>
            {carregando ? "Criando..." : "Criar conta"}
          </button>
        </form>

        <div className="ajuda-login">
          <Link to="/login">Já tem conta? Entrar</Link>
        </div>
      </div>
    </div>
  );
}

export default Cadastro;