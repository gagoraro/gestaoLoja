import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { brl } from '../api/format';
import { Orcamento, Produto } from '../api/types';

interface Linha { produto: Produto; quantidade: number }

export default function NovoOrcamentoPage() {
  const navigate = useNavigate();
  const [produtos, setProdutos] = useState<Produto[]>([]);
  const [clienteNome, setClienteNome] = useState('');
  const [clienteContato, setClienteContato] = useState('');
  const [observacoes, setObservacoes] = useState('');
  const [linhas, setLinhas] = useState<Linha[]>([]);
  const [produtoSel, setProdutoSel] = useState('');
  const [qtdSel, setQtdSel] = useState('1');
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    api.get<Produto[]>('/api/produtos').then(setProdutos).catch((e) => setErro(e.message));
  }, []);

  // prévia do total no front; o valor oficial é recalculado no backend
  const total = useMemo(() => linhas.reduce((s, l) => s + l.produto.precoUnitario * l.quantidade, 0), [linhas]);

  function adicionar() {
    const produto = produtos.find((p) => p.id === Number(produtoSel));
    const qtd = Number(qtdSel);
    if (!produto || !Number.isInteger(qtd) || qtd <= 0) return;
    setLinhas((ls) => {
      const existente = ls.find((l) => l.produto.id === produto.id);
      return existente
        ? ls.map((l) => (l === existente ? { ...l, quantidade: l.quantidade + qtd } : l))
        : [...ls, { produto, quantidade: qtd }];
    });
    setProdutoSel('');
    setQtdSel('1');
  }

  const alterarQtd = (id: number, qtd: number) =>
    setLinhas((ls) => ls.map((l) => (l.produto.id === id ? { ...l, quantidade: Math.max(1, qtd || 1) } : l)));
  const removerLinha = (id: number) => setLinhas((ls) => ls.filter((l) => l.produto.id !== id));

  async function salvar(e: FormEvent, gerarPdf: boolean) {
    e.preventDefault();
    setErro('');
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
      <div className="cabecalho"><h1>Novo orçamento</h1></div>
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
          <div className="linha adicionar">
            <label className="cresce">Produto
              <select value={produtoSel} onChange={(e) => setProdutoSel(e.target.value)}>
                <option value="">Selecione…</option>
                {produtos.map((p) => (
                  <option key={p.id} value={p.id}>{p.sku} — {p.nome} ({brl(p.precoUnitario)}) · estoque {p.quantidadeEstoque}</option>
                ))}
              </select>
            </label>
            <label className="estreito">Qtd<input type="number" min={1} value={qtdSel} onChange={(e) => setQtdSel(e.target.value)} /></label>
            <button type="button" onClick={adicionar} disabled={!produtoSel}>Adicionar</button>
          </div>

          {linhas.length === 0 ? <p className="vazio">Nenhum item adicionado.</p> : (
            <table className="tabela">
              <thead><tr><th>Produto</th><th className="num">Preço unit.</th><th className="num">Qtd</th><th className="num">Subtotal</th><th></th></tr></thead>
              <tbody>
                {linhas.map((l) => (
                  <tr key={l.produto.id}>
                    <td>
                      {l.produto.nome}
                      {l.quantidade > l.produto.quantidadeEstoque && (
                        <div className="aviso">Acima do estoque atual ({l.produto.quantidadeEstoque})</div>
                      )}
                    </td>
                    <td className="num">{brl(l.produto.precoUnitario)}</td>
                    <td className="num">
                      <input className="qtd" type="number" min={1} value={l.quantidade}
                        onChange={(e) => alterarQtd(l.produto.id, Number(e.target.value))} />
                    </td>
                    <td className="num">{brl(l.produto.precoUnitario * l.quantidade)}</td>
                    <td><button type="button" className="btn-link" onClick={() => removerLinha(l.produto.id)}>Remover</button></td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr><td colSpan={3} className="num"><strong>Total</strong></td><td className="num total">{brl(total)}</td><td></td></tr>
              </tfoot>
            </table>
          )}
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
