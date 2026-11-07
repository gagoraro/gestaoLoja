import { FormEvent, useState } from 'react';
import { Produto, ProdutoInput } from '../api/types';

interface Props {
  produto?: Produto;
  onSalvar: (dados: ProdutoInput) => Promise<void>;
  onCancelar: () => void;
}

export default function ProdutoForm({ produto, onSalvar, onCancelar }: Props) {
  const [sku, setSku] = useState(produto?.sku ?? '');
  const [nome, setNome] = useState(produto?.nome ?? '');
  const [descricao, setDescricao] = useState(produto?.descricao ?? '');
  const [preco, setPreco] = useState(produto ? String(produto.precoUnitario) : '');
  const [estoqueInicial, setEstoqueInicial] = useState('0');
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setErro('');
    const precoNum = Number(preco.replace(',', '.'));
    if (Number.isNaN(precoNum) || precoNum < 0) return setErro('Preço inválido');
    setSalvando(true);
    try {
      await onSalvar({
        sku: sku.trim() || undefined, nome, descricao: descricao || undefined, precoUnitario: precoNum,
        estoqueInicial: produto ? undefined : Number(estoqueInicial) || 0,
      });
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form onSubmit={submit} className="form">
      <label>Código (SKU) — opcional
        <input value={sku} onChange={(e) => setSku(e.target.value)} maxLength={50}
          placeholder={produto ? 'Deixe em branco para manter o atual' : 'Deixe em branco para gerar automaticamente'} />
      </label>
      <label>Nome<input value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={150} /></label>
      <label>Descrição<textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} maxLength={1000} rows={2} /></label>
      <div className="linha">
        <label>Preço unitário (R$)
          <input value={preco} onChange={(e) => setPreco(e.target.value)} required inputMode="decimal" placeholder="0,00" />
        </label>
        {!produto && (
          <label>Estoque inicial
            <input type="number" min={0} value={estoqueInicial} onChange={(e) => setEstoqueInicial(e.target.value)} />
          </label>
        )}
      </div>
      {erro && <p className="erro">{erro}</p>}
      <div className="acoes">
        <button type="button" className="btn-sec" onClick={onCancelar}>Cancelar</button>
        <button type="submit" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</button>
      </div>
    </form>
  );
}
