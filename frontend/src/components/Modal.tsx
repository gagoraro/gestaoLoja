import { ReactNode, useEffect } from 'react';

export default function Modal({ titulo, onClose, children }: { titulo: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', esc);
    return () => window.removeEventListener('keydown', esc);
  }, [onClose]);

  return (
    <div className="modal-fundo" onMouseDown={onClose}>
      <div className="modal" role="dialog" aria-label={titulo} onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-topo">
          <h2>{titulo}</h2>
          <button className="btn-link" onClick={onClose} aria-label="Fechar">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}
