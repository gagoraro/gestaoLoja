import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { brl, dataBr } from '../api/format';
import { OrcamentoResumo } from '../api/types';

export default function OrcamentosPage() {
  const [orcamentos, setOrcamentos] = useState<OrcamentoResumo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');

  const carregar = () =>
    api.get<OrcamentoResumo[]>('/api/orcamentos')
      .then((r) => { setOrcamentos(r); setErro(''); })
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));

  useEffect(() => { carregar(); }, []);

  async function baixar(id: number) {
    try { await api.download(`/api/orcamentos/${id}/pdf`, `orcamento-${id}.pdf`); }
    catch (e) { alert((e as Error).message); }
  }

  async function remover(o: OrcamentoResumo) {
    if (!confirm(`Excluir o orçamento nº ${o.id} de ${o.clienteNome}?`)) return;
    try { await api.del(`/api/orcamentos/${o.id}`); carregar(); }
    catch (e) { alert((e as Error).message); }
  }

  return (
    <section>
      <div className="cabecalho">
        <h1>Orçamentos</h1>
        <Link to="/orcamentos/novo" className="botao">+ Novo orçamento</Link>
      </div>
      {erro && <p className="erro">{erro}</p>}
      {carregando ? <p>Carregando…</p> : orcamentos.length === 0 ? (
        <p className="vazio">Nenhum orçamento criado.</p>
      ) : (
        <table className="tabela">
          <thead><tr><th>Nº</th><th>Cliente</th><th>Emissão</th><th>Validade</th><th className="num">Total</th><th></th></tr></thead>
          <tbody>
            {orcamentos.map((o) => (
              <tr key={o.id}>
                <td className="mono">{o.id}</td>
                <td>{o.clienteNome}</td>
                <td>{dataBr(o.criadoEm)}</td>
                <td>{dataBr(o.validade)}</td>
                <td className="num">{brl(o.valorTotal)}</td>
                <td className="acoes-linha">
                  <button className="btn-sec" onClick={() => baixar(o.id)}>Baixar PDF</button>
                  <button className="btn-perigo" onClick={() => remover(o)}>Excluir</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
