import { FormEvent, useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { brl, dataBr, dataHoraBr, horaBr } from '../api/format';
import { Caixa, CaixaResumo, FORMAS, FormaPagamento, Produto, Venda } from '../api/types';
import ItensEditor, { Linha, totalDe } from '../components/ItensEditor';
import Modal from '../components/Modal';

const rotuloForma = (f: FormaPagamento) => FORMAS.find((x) => x.valor === f)?.rotulo ?? f;
const paraNumero = (v: string) => Number(v.replace(/\./g, '').replace(',', '.'));

export default function CaixaPage() {
  const [caixa, setCaixa] = useState<Caixa | null>(null);
  const [produtos, setProdutos] = useState<Produto[]>([]);
  const [historico, setHistorico] = useState<CaixaResumo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [fechando, setFechando] = useState(false);
  const [detalhe, setDetalhe] = useState<Caixa | null>(null);

  const carregar = useCallback(async () => {
    try {
      const [atual, prods, hist] = await Promise.all([
        api.get<Caixa | undefined>('/api/caixa/atual'),
        api.get<Produto[]>('/api/produtos'),
        api.get<CaixaResumo[]>('/api/caixa'),
      ]);
      setCaixa(atual ?? null);
      setProdutos(prods);
      setHistorico(hist);
      setErro('');
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  async function verDetalhe(id: number) {
    try { setDetalhe(await api.get<Caixa>(`/api/caixa/${id}`)); }
    catch (e) { alert((e as Error).message); }
  }

  if (carregando) return <p className="vazio">Carregando…</p>;

  return (
    <section>
      {erro && <p className="erro" style={{ marginBottom: '1rem' }}>{erro}</p>}
      {caixa
        ? <CaixaAberto caixa={caixa} produtos={produtos} onAlterado={carregar} onFechar={() => setFechando(true)} />
        : <AbrirCaixa onAberto={carregar} />}

      <div className="cartao" style={{ marginTop: '1.5rem' }}>
        <h3>Caixas anteriores</h3>
        {historico.filter((h) => !h.aberto).length === 0 ? <p className="sub">Nenhum caixa fechado ainda.</p> : (
          <div className="tabela-wrap">
            <table className="tabela">
              <thead><tr><th>Dia</th><th>Aberto</th><th>Fechado</th><th className="num">Vendas</th><th className="num">Total</th><th /></tr></thead>
              <tbody>
                {historico.filter((h) => !h.aberto).map((h) => (
                  <tr key={h.id}>
                    <td>{dataBr(h.abertoEm)}</td>
                    <td>{horaBr(h.abertoEm)}</td>
                    <td>{h.fechadoEm ? dataHoraBr(h.fechadoEm) : '—'}</td>
                    <td className="num">{h.quantidadeVendas}</td>
                    <td className="num">{brl(h.totalVendas)}</td>
                    <td className="acoes-linha"><button className="btn-sec btn-peq" onClick={() => verDetalhe(h.id)}>Detalhes</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {fechando && caixa && (
        <FecharCaixa caixa={caixa} onClose={() => setFechando(false)}
          onFechado={(c) => { setFechando(false); setDetalhe(c); carregar(); }} />
      )}
      {detalhe && (
        <Modal titulo={`Caixa de ${dataBr(detalhe.abertoEm)}`} onClose={() => setDetalhe(null)}>
          <ResumoCaixa caixa={detalhe} />
          <ListaVendas vendas={detalhe.vendas} />
        </Modal>
      )}
    </section>
  );
}

function AbrirCaixa({ onAberto }: { onAberto: () => void }) {
  const [troco, setTroco] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function abrir(e: FormEvent) {
    e.preventDefault();
    const valor = troco.trim() ? paraNumero(troco) : 0;
    if (Number.isNaN(valor) || valor < 0) return setErro('Valor inválido.');
    setEnviando(true);
    try { await api.post('/api/caixa/abrir', { valorAbertura: valor }); onAberto(); }
    catch (e) { setErro((e as Error).message); }
    finally { setEnviando(false); }
  }

  return (
    <>
      <div className="cabecalho">
        <div>
          <p className="eyebrow">Caixa do dia</p>
          <h1>Caixa <em>fechado</em></h1>
          <p className="sub">Abra o caixa para começar a registrar as vendas de hoje.</p>
        </div>
      </div>
      <form className="cartao form" onSubmit={abrir} style={{ maxWidth: 480 }}>
        <label>Troco inicial na gaveta (R$)
          <input inputMode="decimal" placeholder="0,00" value={troco} onChange={(e) => setTroco(e.target.value)} autoFocus />
        </label>
        {erro && <p className="erro">{erro}</p>}
        <button type="submit" className="btn-grande" disabled={enviando}>{enviando ? 'Abrindo…' : 'Abrir caixa'}</button>
      </form>
    </>
  );
}

function CaixaAberto({ caixa, produtos, onAlterado, onFechar }:
  { caixa: Caixa; produtos: Produto[]; onAlterado: () => void; onFechar: () => void }) {
  const [linhas, setLinhas] = useState<Linha[]>([]);
  const [forma, setForma] = useState<FormaPagamento>('DINHEIRO');
  const [observacao, setObservacao] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);
  const total = totalDe(linhas);

  async function finalizar() {
    setErro('');
    if (linhas.length === 0) return setErro('Adicione ao menos um produto.');
    setEnviando(true);
    try {
      await api.post('/api/caixa/vendas', {
        formaPagamento: forma,
        observacao: observacao || undefined,
        itens: linhas.map((l) => ({ produtoId: l.produto.id, quantidade: l.quantidade })),
      });
      setLinhas([]);
      setObservacao('');
      setForma('DINHEIRO');
      onAlterado();
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setEnviando(false);
    }
  }

  async function cancelar(v: Venda) {
    if (!confirm(`Cancelar a venda #${v.id} de ${brl(v.valorTotal)}? Os itens voltam para o estoque.`)) return;
    try { await api.post(`/api/caixa/vendas/${v.id}/cancelar`); onAlterado(); }
    catch (e) { alert((e as Error).message); }
  }

  return (
    <>
      <div className="cabecalho">
        <div>
          <p className="eyebrow">Caixa do dia · {dataBr(caixa.abertoEm)}</p>
          <h1>Caixa <em>aberto</em></h1>
          <p className="sub">Desde {horaBr(caixa.abertoEm)}{caixa.abertoPor && caixa.abertoPor !== 'local' ? ` · por ${caixa.abertoPor}` : ''} · troco inicial {brl(caixa.valorAbertura)}</p>
        </div>
        <button className="btn-sec" onClick={onFechar}>Fechar caixa</button>
      </div>

      <ResumoCaixa caixa={caixa} />

      <div className="grade grade-2">
        <div className="cartao">
          <h3>Nova venda</h3>
          <ItensEditor produtos={produtos} linhas={linhas} onChange={setLinhas} limitarAoEstoque />
          <div className="form" style={{ marginTop: '1rem' }}>
            <label>Forma de pagamento
              <div className="segmentado" role="radiogroup">
                {FORMAS.map((f) => (
                  <button key={f.valor} type="button" role="radio" aria-checked={forma === f.valor}
                    className={forma === f.valor ? 'ativo' : ''} onClick={() => setForma(f.valor)}>{f.rotulo}</button>
                ))}
              </div>
            </label>
            <label>Observação (opcional)
              <input value={observacao} onChange={(e) => setObservacao(e.target.value)} maxLength={500} placeholder="Cliente, troca, desconto combinado…" />
            </label>
            {erro && <p className="erro">{erro}</p>}
            <button className="btn-grande" style={{ justifyContent: 'center' }} disabled={enviando || linhas.length === 0} onClick={finalizar}>
              {enviando ? 'Registrando…' : `Finalizar venda · ${brl(total)}`}
            </button>
          </div>
        </div>

        <div className="cartao">
          <h3>Vendas de hoje</h3>
          <ListaVendas vendas={caixa.vendas} onCancelar={cancelar} />
        </div>
      </div>
    </>
  );
}

function ResumoCaixa({ caixa }: { caixa: Caixa }) {
  const r = caixa.resumo;
  return (
    <>
      <div className="metricas">
        <div className="metrica destaque"><div className="rotulo">Total do dia</div><div className="valor">{brl(r.totalVendas)}</div></div>
        <div className="metrica"><div className="rotulo">Vendas</div><div className="valor">{r.quantidadeVendas}</div></div>
        <div className="metrica"><div className="rotulo">Dinheiro esperado na gaveta</div><div className="valor">{brl(r.dinheiroEsperadoNaGaveta)}</div></div>
        {r.diferenca !== null && (
          <div className="metrica"><div className="rotulo">Diferença no fechamento</div>
            <div className={`valor ${r.diferenca < 0 ? 'negativo' : r.diferenca > 0 ? 'positivo' : ''}`}>{brl(r.diferenca)}</div></div>
        )}
      </div>
      <div className="metricas">
        {FORMAS.map((f) => (
          <div key={f.valor} className="metrica"><div className="rotulo">{f.rotulo}</div>
            <div className="valor" style={{ fontSize: '1.3rem' }}>{brl(r.totalPorForma[f.valor] ?? 0)}</div></div>
        ))}
      </div>
    </>
  );
}

function ListaVendas({ vendas, onCancelar }: { vendas: Venda[]; onCancelar?: (v: Venda) => void }) {
  if (vendas.length === 0) return <p className="sub">Nenhuma venda registrada.</p>;
  return (
    <div className="tabela-wrap">
      <table className="tabela compacta">
        <thead><tr><th>Hora</th><th>Itens</th><th>Pagamento</th><th className="num">Valor</th>{onCancelar && <th />}</tr></thead>
        <tbody>
          {vendas.map((v) => (
            <tr key={v.id} className={v.cancelada ? 'cancelada' : ''}>
              <td>{horaBr(v.criadoEm)}</td>
              <td>{v.itens.map((i) => `${i.quantidade}× ${i.produtoNome}`).join(', ')}
                {v.observacao && <div className="sub">{v.observacao}</div>}</td>
              <td>{rotuloForma(v.formaPagamento)}</td>
              <td className="num">{brl(v.valorTotal)}</td>
              {onCancelar && (
                <td className="num">{v.cancelada ? <span className="tag perigo">Cancelada</span>
                  : <button className="btn-link" onClick={() => onCancelar(v)}>Cancelar</button>}</td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FecharCaixa({ caixa, onClose, onFechado }: { caixa: Caixa; onClose: () => void; onFechado: (c: Caixa) => void }) {
  const [contado, setContado] = useState('');
  const [observacao, setObservacao] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);
  const esperado = caixa.resumo.dinheiroEsperadoNaGaveta;
  const valorContado = contado.trim() ? paraNumero(contado) : null;
  const diferenca = valorContado !== null && !Number.isNaN(valorContado) ? valorContado - esperado : null;

  async function fechar(e: FormEvent) {
    e.preventDefault();
    if (valorContado !== null && (Number.isNaN(valorContado) || valorContado < 0)) return setErro('Valor inválido.');
    setEnviando(true);
    try {
      onFechado(await api.post<Caixa>(`/api/caixa/${caixa.id}/fechar`, { valorContado, observacao: observacao || undefined }));
    } catch (e) {
      setErro((e as Error).message);
      setEnviando(false);
    }
  }

  return (
    <Modal titulo="Fechar caixa" onClose={onClose}>
      <form className="form" onSubmit={fechar}>
        <div className="metricas" style={{ marginBottom: 0 }}>
          <div className="metrica destaque"><div className="rotulo">Total do dia</div><div className="valor">{brl(caixa.resumo.totalVendas)}</div></div>
          <div className="metrica"><div className="rotulo">Dinheiro esperado</div><div className="valor">{brl(esperado)}</div></div>
        </div>
        <label>Dinheiro contado na gaveta (R$) — opcional
          <input inputMode="decimal" placeholder="0,00" value={contado} onChange={(e) => setContado(e.target.value)} autoFocus />
        </label>
        {diferenca !== null && (
          <p className={diferenca < 0 ? 'negativo' : diferenca > 0 ? 'positivo' : 'sub'} style={{ margin: 0 }}>
            {diferenca === 0 ? 'Caixa confere.' : diferenca < 0 ? `Faltando ${brl(-diferenca)}` : `Sobrando ${brl(diferenca)}`}
          </p>
        )}
        <label>Observação<input value={observacao} onChange={(e) => setObservacao(e.target.value)} maxLength={500} /></label>
        {erro && <p className="erro">{erro}</p>}
        <div className="acoes">
          <button type="button" className="btn-sec" onClick={onClose}>Voltar</button>
          <button type="submit" disabled={enviando}>{enviando ? 'Fechando…' : 'Confirmar fechamento'}</button>
        </div>
      </form>
    </Modal>
  );
}
