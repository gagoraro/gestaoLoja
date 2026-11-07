import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Orcamento, Produto } from '../api/types';
import ItensEditor, { Linha } from '../components/ItensEditor';

export default function NovoOrcamentoPage() {
  const navigate = useNavigate();
  const [produtos, setProdutos] = useState<Produto[]>([]);
  const [clienteNome, setClienteNome] = useState('');
  const [clienteContato, setClienteContato] = useState('');
  const [observacoes, setObservacoes] = useState('');
  const [linhas, setLinhas] = useState<Linha[]>([]);
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    api.get<Produto[]>('/api/produtos').then(setProdutos).catch((e) => setErro(e.message));
  }, []);

  async function salvar(e: FormEvent, gerarPdf: boolean) {
    e.preventDefault();
    setErro('');
    if (!clienteNome.trim()) return setErro('Informe o nome do cliente.');
    if (linhas.length === 0) return setErro('Adicione ao menos um produto.');
    setSalvando(true);
    try {
      const o = await api.post<Orcamento>('/api/orcamentos', {
        clienteNome,
        clienteContato: clienteContato || undefined,
        observacoes: observacoes || undefined,
        itens: linhas.map((l) => ({ produtoId: l.produto.id, quantidade: l.quantidade })),
      });
      if (gerarPdf) await api.download(`/api/orcamentos/${o.id}/pdf`, `orcamento-${o.id}.pdf`);
      navigate('/orcamentos');
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section>
      <div className="cabecalho">
        <div className="cabecalho-marca">
          <img src="/logo.png" alt="Logo A Vitoriosa de Angra" />
          <div>
            <p className="eyebrow">A Vitoriosa de Angra</p>
            <h1>Novo orçamento</h1>
          </div>
        </div>
      </div>
      <form className="form" onSubmit={(e) => salvar(e, true)}>
        <div className="cartao">
          <h3>Cliente</h3>
          <div className="linha">
            <label>Nome<input value={clienteNome} onChange={(e) => setClienteNome(e.target.value)} required maxLength={150} /></label>
            <label>Contato (telefone/e-mail)<input value={clienteContato} onChange={(e) => setClienteContato(e.target.value)} maxLength={150} /></label>
          </div>
        </div>
        <div className="cartao">
          <h3>Itens</h3>
          <ItensEditor produtos={produtos} linhas={linhas} onChange={setLinhas} />
        </div>
        <div className="cartao">
          <label>Observações<textarea rows={3} value={observacoes} onChange={(e) => setObservacoes(e.target.value)} maxLength={1000}
            placeholder="Condições de pagamento, prazo de entrega…" /></label>
        </div>
        {erro && <p className="erro">{erro}</p>}
        <div className="acoes">
          <button type="button" className="btn-sec" onClick={() => navigate('/orcamentos')}>Cancelar</button>
          <button type="button" className="btn-sec" disabled={salvando} onClick={(e) => salvar(e, false)}>Salvar sem PDF</button>
          <button type="submit" disabled={salvando}>{salvando ? 'Gerando…' : 'Salvar e gerar PDF'}</button>
        </div>
      </form>
    </section>
  );
}
