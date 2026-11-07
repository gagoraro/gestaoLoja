import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { brl } from '../api/format';
import { Produto, ProdutoInput } from '../api/types';
import EstoqueModal from '../components/EstoqueModal';
import Modal from '../components/Modal';
import ProdutoForm from '../components/ProdutoForm';

export default function ProdutosPage() {
  const [produtos, setProdutos] = useState<Produto[]>([]);
  const [busca, setBusca] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [editando, setEditando] = useState<Produto | 'novo' | null>(null);
  const [estoque, setEstoque] = useState<Produto | null>(null);

  const carregar = useCallback(async () => {
    try {
      const q = busca.trim() ? `?busca=${encodeURIComponent(busca.trim())}` : '';
      setProdutos(await api.get<Produto[]>(`/api/produtos${q}`));
      setErro('');
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }, [busca]);

  useEffect(() => {
    const t = setTimeout(carregar, 250); // debounce da busca
    return () => clearTimeout(t);
  }, [carregar]);

  async function salvar(dados: ProdutoInput) {
    if (editando === 'novo') await api.post('/api/produtos', dados);
    else if (editando) await api.put(`/api/produtos/${editando.id}`, dados);
    setEditando(null);
    carregar();
  }

  async function remover(p: Produto) {
    if (!confirm(`Remover o produto "${p.nome}"?`)) return;
    try {
      await api.del(`/api/produtos/${p.id}`);
      carregar();
    } catch (e) {
      alert((e as Error).message);
    }
  }

  return (
    <section>
      <div className="cabecalho">
        <h1>Produtos & Estoque</h1>
        <button onClick={() => setEditando('novo')}>+ Novo produto</button>
      </div>
      <input className="busca" placeholder="Buscar por nome ou SKU…" value={busca} onChange={(e) => setBusca(e.target.value)} />

      {erro && <p className="erro">{erro}</p>}
      {carregando ? <p>Carregando…</p> : produtos.length === 0 ? (
        <p className="vazio">Nenhum produto cadastrado.</p>
      ) : (
        <table className="tabela">
          <thead>
            <tr><th>SKU</th><th>Produto</th><th className="num">Preço</th><th className="num">Estoque</th><th></th></tr>
          </thead>
          <tbody>
            {produtos.map((p) => (
              <tr key={p.id}>
                <td className="mono">{p.sku}</td>
                <td>{p.nome}{p.descricao && <div className="sub">{p.descricao}</div>}</td>
                <td className="num">{brl(p.precoUnitario)}</td>
                <td className={`num ${p.quantidadeEstoque === 0 ? 'zerado' : ''}`}>{p.quantidadeEstoque}</td>
                <td className="acoes-linha">
                  <button className="btn-sec" onClick={() => setEstoque(p)}>Estoque</button>
                  <button className="btn-sec" onClick={() => setEditando(p)}>Editar</button>
                  <button className="btn-perigo" onClick={() => remover(p)}>Remover</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {editando && (
        <Modal titulo={editando === 'novo' ? 'Novo produto' : 'Editar produto'} onClose={() => setEditando(null)}>
          <ProdutoForm produto={editando === 'novo' ? undefined : editando} onSalvar={salvar} onCancelar={() => setEditando(null)} />
        </Modal>
      )}
      {estoque && <EstoqueModal produto={estoque} onClose={() => setEstoque(null)} onAlterado={carregar} />}
    </section>
  );
}
