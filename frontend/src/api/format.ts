const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const brl = (v: number) => moeda.format(v);

export const dataBr = (iso: string | null) => {
  if (!iso) return '—';
  // datas "YYYY-MM-DD" sem horário: evita deslocamento de fuso
  const d = iso.length === 10 ? new Date(iso + 'T12:00:00') : new Date(iso);
  return d.toLocaleDateString('pt-BR');
};

export const horaBr = (iso: string) => new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
export const dataHoraBr = (iso: string | null) => (iso ? `${dataBr(iso)} ${horaBr(iso)}` : '—');
