const base = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const, viewBox: '0 0 24 24' };

export const IconeProdutos = () => (
  <svg {...base}><path d="M21 8 12 3 3 8v8l9 5 9-5V8Z" /><path d="m3 8 9 5 9-5M12 13v8" /></svg>
);
export const IconeEstoque = () => (
  <svg {...base}><path d="M3 21V9l9-6 9 6v12" /><path d="M7 21v-8h10v8M7 17h10" /></svg>
);
export const IconeOrcamento = () => (
  <svg {...base}><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9l-6-6Z" /><path d="M14 3v6h6M8 13h8M8 17h5" /></svg>
);
export const IconeCaixa = () => (
  <svg {...base}><rect x="3" y="10" width="18" height="11" rx="2" /><path d="M7 10V5h10v5M7 15h.01M12 15h.01M17 15h.01M7 18h10" /></svg>
);
export const IconeInicio = () => (
  <svg {...base}><path d="m3 11 9-8 9 8" /><path d="M5 10v10h5v-6h4v6h5V10" /></svg>
);
