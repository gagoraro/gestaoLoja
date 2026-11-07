import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api/client';
import { dataBr } from '../api/format';
import { Movimentacao, Produto, TipoMovimentacao } from '../api/types';
import Modal from './Modal';

interface Props {
  produto: Produto;
  onClose: () => void;
  onAlterado: () => void;
}

export default function EstoqueModal({ produto, onClose, onAlterado }: Props) {
  const [tipo, setTipo] = useState<TipoMovimentacao>('ENTRADA');
  const [quantidade, setQuantidade] = useState('1');
  const [motivo, setMotivo] = useState('');
  const [saldo, setSaldo] = useState(produto.quantidadeEstoque);
  const [historico, setHistorico] = useState<Movimentacao[]>([]);
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  const carregar = () =>
    api.get<Movimentacao[]>(`/api/produtos/${produto.id}/movimentacoes`).then(setHistorico).catch((e) => setErro(e.message));

  useEffect(() => { carregar(); }, [produto.id]); // eslint-disable-line react-hooks/exhaustive-deps

  async function submit(e: FormEvent) {
    e.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      const m = await api.post<Movimentacao>(`/api/produtos/${produto.id}/movimentacoes`, {
        tipo, quantidade: Number(quantidade), motivo: motivo || undefined,
      });
      setSaldo(m.saldoApos);
      setQuantidade('1');
      setMotivo('');
      onAlterado();
      carregar();
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal titulo={`Estoque — ${produto.nome}`} onClose={onClose}>
      <p className="saldo">Saldo atual: <strong>{saldo}</strong> un.</p>
      <form onSubmit={submit} className="form">
        <div className="segmentado">
          <button type="button" className={tipo === 'ENTRADA' ? 'ativo entrada' : ''} onClick={() => setTipo('ENTRADA')}>+ Adicionar</button>
          <button type="button" className={tipo === 'SAIDA' ? 'ativo saida' : ''} onClick={() => setTipo('SAIDA')}>− Remover</button>
        </div>
        <div className="linha">
          <label>Quantidade<input type="number" min={1} value={quantidade} onChange={(e) => setQuantidade(e.target.value)} required /></label>
          <label>Motivo (opcional)<input value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Compra, venda, ajuste…" maxLength={255} /></label>
        </div>
        {erro && <p className="erro">{erro}</p>}
        <div className="acoes">
          <button type="submit" disabled={enviando}>{tipo === 'ENTRADA' ? 'Adicionar ao estoque' : 'Remover do estoque'}</button>
        </div>
      </form>

      <h3>Últimas movimentações</h3>
      {historico.length === 0 ? <p className="vazio">Nenhuma movimentação.</p> : (
        <div className="tabela-wrap"><table className="tabela compacta">
          <thead><tr><th>Data</th><th>Tipo</th><th className="num">Qtd</th><th className="num">Saldo</th><th>Motivo</th></tr></thead>
          <tbody>
            {historico.map((m) => (
              <tr key={m.id}>
                <td>{dataBr(m.criadoEm)}</td>
                <td><span className={`tag ${m.tipo === 'ENTRADA' ? 'ok' : 'perigo'}`}>{m.tipo === 'ENTRADA' ? 'Entrada' : 'Saída'}</span></td>
                <td className="num">{m.quantidade}</td>
                <td className="num">{m.saldoApos}</td>
                <td>{m.motivo ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table></div>
      )}
    </Modal>
  );
}
