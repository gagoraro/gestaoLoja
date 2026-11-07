import { useState } from 'react';
import { brl } from '../api/format';
import { Produto } from '../api/types';

export interface Linha { produto: Produto; quantidade: number }

interface Props {
  produtos: Produto[];
  linhas: Linha[];
  onChange: (linhas: Linha[]) => void;
  /** venda: bloqueia passar do estoque; orçamento: só avisa */
  limitarAoEstoque?: boolean;
}

export const totalDe = (linhas: Linha[]) => linhas.reduce((s, l) => s + l.produto.precoUnitario * l.quantidade, 0);

/** Seleção de produtos + quantidades com total ao vivo (o valor oficial é recalculado no backend). */
export default function ItensEditor({ produtos, linhas, onChange, limitarAoEstoque }: Props) {
  const [produtoSel, setProdutoSel] = useState('');
  const [qtdSel, setQtdSel] = useState('1');

  function adicionar() {
    const produto = produtos.find((p) => p.id === Number(produtoSel));
    const qtd = Number(qtdSel);
    if (!produto || !Number.isInteger(qtd) || qtd <= 0) return;
    const existente = linhas.find((l) => l.produto.id === produto.id);
    onChange(existente
      ? linhas.map((l) => (l === existente ? { ...l, quantidade: l.quantidade + qtd } : l))
      : [...linhas, { produto, quantidade: qtd }]);
    setProdutoSel('');
    setQtdSel('1');
  }

  const alterar = (id: number, qtd: number) =>
    onChange(linhas.map((l) => (l.produto.id === id ? { ...l, quantidade: Math.max(1, qtd || 1) } : l)));
  const remover = (id: number) => onChange(linhas.filter((l) => l.produto.id !== id));
  const disponiveis = limitarAoEstoque ? produtos.filter((p) => p.quantidadeEstoque > 0) : produtos;

  return (
    <>
      <div className="linha">
        <label className="cresce">Produto
          <select value={produtoSel} onChange={(e) => setProdutoSel(e.target.value)}>
            <option value="">Selecione…</option>
            {disponiveis.map((p) => (
              <option key={p.id} value={p.id}>{p.nome} — {brl(p.precoUnitario)} · estoque {p.quantidadeEstoque}</option>
            ))}
          </select>
        </label>
        <label className="estreito">Qtd
          <input type="number" min={1} value={qtdSel} onChange={(e) => setQtdSel(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); adicionar(); } }} />
        </label>
        <button type="button" onClick={adicionar} disabled={!produtoSel}>Adicionar</button>
      </div>

      {linhas.length === 0 ? <p className="vazio">Nenhum item adicionado.</p> : (
        <div className="tabela-wrap">
          <table className="tabela">
            <thead><tr><th>Produto</th><th className="num">Preço</th><th className="num">Qtd</th><th className="num">Subtotal</th><th /></tr></thead>
            <tbody>
              {linhas.map((l) => (
                <tr key={l.produto.id}>
                  <td>
                    {l.produto.nome}
                    {l.quantidade > l.produto.quantidadeEstoque && (
                      <div className={limitarAoEstoque ? 'sub negativo' : 'aviso'}>
                        {limitarAoEstoque ? 'Estoque insuficiente' : 'Acima do estoque atual'} ({l.produto.quantidadeEstoque} un.)
                      </div>
                    )}
                  </td>
                  <td className="num">{brl(l.produto.precoUnitario)}</td>
                  <td className="num">
                    <input className="qtd" type="number" min={1} value={l.quantidade} aria-label={`Quantidade de ${l.produto.nome}`}
                      onChange={(e) => alterar(l.produto.id, Number(e.target.value))} />
                  </td>
                  <td className="num">{brl(l.produto.precoUnitario * l.quantidade)}</td>
                  <td className="num"><button type="button" className="btn-link" onClick={() => remover(l.produto.id)}>Remover</button></td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr><td colSpan={3} className="num"><strong>Total</strong></td><td className="num total">{brl(totalDe(linhas))}</td><td /></tr>
            </tfoot>
          </table>
        </div>
      )}
    </>
  );
}
