import React from "react";

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, info: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    // Guarda info para exibição em dev e para envio a serviços de logging
    this.setState({ info });
    if (typeof console !== 'undefined' && typeof console.error === 'function') {
      console.error("ErrorBoundary caught:", error, info);
    }
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null, info: null }, () => {
      if (typeof this.props.onReset === 'function') {
        try {
          this.props.onReset();
        } catch {
          window.location.reload();
        }
      }
    });
  };

  handleReload = () => {
    window.location.reload();
  };

  render() {
    const { hasError, error, info } = this.state;
    if (hasError) {
      return (
        <div style={{ padding: 24 }}>
          <h2>Ocorreu um erro nesta seção</h2>
          <p>Tente recarregar ou tentar novamente. Se persistir, entre em contato com o suporte.</p>
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button onClick={this.handleReload} style={{ padding: '8px 12px' }}>Recarregar</button>
            <button onClick={this.handleReset} style={{ padding: '8px 12px' }}>Tentar novamente</button>
          </div>
          {import.meta.env && import.meta.env.DEV && (
            <details style={{ marginTop: 12 }}>
              <summary>Detalhes do erro (dev)</summary>
              <pre style={{ whiteSpace: 'pre-wrap' }}>{String(error && error.toString()) + '\n' + (info && info.componentStack)}</pre>
            </details>
          )}
        </div>
      );
    }
    return this.props.children;
  }
}

export default ErrorBoundary;
