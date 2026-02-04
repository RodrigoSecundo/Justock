import "../../styles/pages/login/login.css";
import logo from "../../assets/logo_preto_baix.png";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { login } from "../../utils/auth";

function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro("");
    try {
      const usuario = await login({ email, senha });
      window.localStorage.setItem("jt:user", JSON.stringify(usuario));
      navigate("/dashboard");
    } catch (err) {
      console.error("Login error:", err);
      const mensagem = err?.message || "Usuário ou senha inválidos.";
      setErro(mensagem);
    }
  };

  return (
    <div className="container-login">
      <button
        className="seta-voltar"
        aria-label="Voltar para home"
        onClick={() => navigate('/')}
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
              type="email"
              id="email"
              placeholder="E-mail"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="grupo-formulario">
            <input
              type="password"
              id="senha"
              placeholder="Senha"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
            />
          </div>

          {erro && (
            <div role="alert" data-testid="login-error" className="mensagem-erro-login">
              {erro}
            </div>
          )}

          <button type="submit" className="botao-login">
            Entrar
          </button>
        </form>

        <div className="ajuda-login">
          <a href="#">Precisa de ajuda para entrar?</a>
        </div>
      </div>
    </div>
  );
}

export default Login;
