import { ReactNode, useEffect, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { IconeCaixa, IconeEstoque, IconeInicio, IconeOrcamento, IconeProdutos } from './Icones';

const SECOES = [
  { para: '/', rotulo: 'Início', icone: <IconeInicio /> },
  { para: '/caixa', rotulo: 'Caixa', icone: <IconeCaixa /> },
  { para: '/produtos', rotulo: 'Produtos', icone: <IconeProdutos /> },
  { para: '/estoque', rotulo: 'Estoque', icone: <IconeEstoque /> },
  { para: '/orcamentos', rotulo: 'Orçamentos', icone: <IconeOrcamento /> },
];

export default function Layout({ children }: { children: ReactNode }) {
  const [aberto, setAberto] = useState(false);
  const { pathname } = useLocation();
  const { email, loginAtivo, sair } = useAuth();
  const atual = SECOES.find((s) => (s.para === '/' ? pathname === '/' : pathname.startsWith(s.para)));

  useEffect(() => setAberto(false), [pathname]); // fecha o menu ao navegar
  useEffect(() => {
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && setAberto(false);
    window.addEventListener('keydown', esc);
    return () => window.removeEventListener('keydown', esc);
  }, []);

  return (
    <>
      <header className="topo">
        <button className="hamburguer" aria-label="Abrir menu" aria-expanded={aberto} onClick={() => setAberto(true)}>
          <span />
        </button>
        <Link to="/" className="marca">
          <img src="/logo.png" alt="" />
          <strong>A Vitoriosa de Angra</strong>
        </Link>
        {atual && <span className="secao-atual">{atual.rotulo}</span>}
      </header>

      <div className={`gaveta-fundo ${aberto ? 'aberta' : ''}`} onClick={() => setAberto(false)} />
      <aside className={`gaveta ${aberto ? 'aberta' : ''}`} aria-hidden={!aberto}>
        <Link to="/" className="marca">
          <img src="/logo.png" alt="Logo A Vitoriosa de Angra" />
          <div>
            <strong>A Vitoriosa de Angra</strong>
            <div className="sub">Gestão da loja</div>
          </div>
        </Link>
        <nav>
          {SECOES.map((s) => (
            <NavLink key={s.para} to={s.para} end={s.para === '/'} tabIndex={aberto ? 0 : -1}>
              {s.icone}
              {s.rotulo}
            </NavLink>
          ))}
        </nav>
        <div className="rodape">
          {loginAtivo ? (
            <>
              <span>Conectado como<br /><strong style={{ color: 'var(--tinta)' }}>{email}</strong></span>
              <button className="btn-sec btn-peq" tabIndex={aberto ? 0 : -1} onClick={sair}>Sair</button>
            </>
          ) : (
            <span>Modo local (sem login)</span>
          )}
        </div>
      </aside>

      <main className="conteudo">{children}</main>
    </>
  );
}
