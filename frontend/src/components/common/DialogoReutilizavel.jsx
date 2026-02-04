import React from 'react';
import { Dialog } from 'primereact/dialog';

/**
 * DialogoReutilizavel
 * Envolve o componente Dialog do PrimeReact padronizando comportamento de modais/abas laterais.
 * - visible: boolean -> controla visibilidade
 * - onHide: () => void -> callback ao fechar
 * - header: ReactNode | string -> cabeçalho opcional
 * - position: 'center' | 'top' | 'right' | 'bottom-right' -> posição
 * - width: string -> largura (ex: '28rem', '420px', 'min(1040px, 92vw)')
 * - footer: ReactNode -> rodapé opcional
 * - className / contentClassName / maskClassName -> classes extras
 */
const DialogoReutilizavel = ({
  visible,
  onHide,
  header,
  position = 'center',
  width,
  footer,
  className = '',
  contentClassName = '',
  style = {},
  maskClassName = '',
  children,
  closable = true,
  dismissableMask = true,
  closeIcon = <i className="pi pi-times" />,
}) => {
  const isSide = position === 'left' || position === 'right';
  const computedWidth = width || (isSide ? '420px' : undefined);

  const computedStyle = {
    ...(isSide ? { height: '100vh', maxHeight: '100vh' } : {}),
    width: computedWidth,
    ...style,
  };

  return (
    <Dialog
      visible={visible}
      onHide={onHide}
      header={header}
      position={position}
      modal
      dismissableMask={dismissableMask}
      closeOnEscape
      blockScroll
      draggable={false}
      resizable={false}
      className={`jt-dialog ${isSide ? 'jt-dialog-side' : 'jt-dialog-center'} ${className}`.trim()}
      contentClassName={contentClassName}
      maskClassName={maskClassName}
      style={computedStyle}
      footer={footer}
      closable={closable}
      closeIcon={closeIcon}
      breakpoints={{ '960px': '92vw', '640px': isSide ? '100vw' : '96vw' }}
    >
      {children}
    </Dialog>
  );
};

export default DialogoReutilizavel;