import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import { brl } from '../api/format';
import { Produto } from '../api/types';
import EstoqueModal from '../components/EstoqueModal';

const ESTOQUE_BAIXO = 5;

export default function EstoquePage() {
  const [produtos, setProdutos] = useState<Produto[]>([]);
  const [busca, setBusca] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [selecionado, setSelecionado] = useState<Produto | null>(null);

  const carregar = useCallback(() =>
    api.get<Produto[]>('/api/produtos')
      .then((r) => { setProdutos(r); setErro(''); })
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false)), []);

  useEffect(() => { carregar(); }, [carregar]);

  const filtrados = useMemo(() => {
    const q = busca.trim().toLowerCase();
    return q ? produtos.filter((p) => p.nome.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)) : produtos;
  }, [produtos, busca]);

  const unidades = produtos.reduce((s, p) => s + p.quantidadeEstoque, 0);
  const valorEstoque = produtos.reduce((s, p) => s + p.quantidadeEstoque * p.precoUnitario, 0);
  const baixos = produtos.filter((p) => p.quantidadeEstoque <= ESTOQUE_BAIXO).length;

  return (
    <section>
      <div className="cabecalho">
        <div>
          <p className="eyebrow">Controle</p>
          <h1>Estoque</h1>
          <p className="sub">Adicione ou remova quantidades e veja o histórico de cada produto.</p>
        </div>
      </div>

      <div className="metricas">
        <div className="metrica"><div className="rotulo">Unidades em estoque</div><div className="valor">{unidades}</div></div>
        <div className="metrica"><div className="rotulo">Valor em estoque (preço de venda)</div><div className="valor">{brl(valorEstoque)}</div></div>
        <div className="metrica"><div className="rotulo">Produtos com estoque baixo (≤ {ESTOQUE_BAIXO})</div><div className="valor">{baixos}</div></div>
      </div>

      <input className="busca" type="search" placeholder="Buscar produto…" value={busca} onChange={(e) => setBusca(e.target.value)} />
      {erro && <p className="erro">{erro}</p>}
      {carregando ? <p className="vazio">Carregando…</p> : filtrados.length === 0 ? (
        <div className="cartao vazio">Nenhum produto. Cadastre produtos na seção Produtos.</div>
      ) : (
        <div className="tabela-wrap">
          <table className="tabela">
            <thead><tr><th>Produto</th><th>Situação</th><th className="num">Quantidade</th><th /></tr></thead>
            <tbody>
              {filtrados.map((p) => (
                <tr key={p.id}>
                  <td>{p.nome}<div className="sub mono">{p.sku}</div></td>
                  <td>
                    {p.quantidadeEstoque === 0 ? <span className="tag perigo">Sem estoque</span>
                      : p.quantidadeEstoque <= ESTOQUE_BAIXO ? <span className="tag alerta">Baixo</span>
                      : <span className="tag ok">Normal</span>}
                  </td>
                  <td className="num total">{p.quantidadeEstoque}</td>
                  <td className="acoes-linha"><button className="btn-sec btn-peq" onClick={() => setSelecionado(p)}>Movimentar</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {selecionado && <EstoqueModal produto={selecionado} onClose={() => setSelecionado(null)} onAlterado={carregar} />}
    </section>
  );
}
