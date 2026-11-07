import { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { IconeCaixa, IconeEstoque, IconeOrcamento, IconeProdutos } from '../components/Icones';

const ATALHOS: { para: string; rotulo: string; descricao: string; icone: ReactNode }[] = [
  { para: '/caixa', rotulo: 'Caixa', descricao: 'Abrir, vender e fechar o dia', icone: <IconeCaixa /> },
  { para: '/produtos', rotulo: 'Produtos', descricao: 'Cadastro e preços', icone: <IconeProdutos /> },
  { para: '/estoque', rotulo: 'Estoque', descricao: 'Entradas e saídas', icone: <IconeEstoque /> },
  { para: '/orcamentos', rotulo: 'Orçamentos', descricao: 'Criar e gerar PDF', icone: <IconeOrcamento /> },
];

function saudacao() {
  const h = new Date().getHours();
  return h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite';
}

export default function InicioPage() {
  const { email } = useAuth();
  return (
    <section>
      <div className="cabecalho">
        <div className="cabecalho-marca">
          <img src="/logo.png" alt="Logo A Vitoriosa de Angra" />
          <div>
            <p className="eyebrow">A Vitoriosa de Angra</p>
            <h1>{saudacao()}<em>{email ? `, ${email.split('@')[0]}` : ''}.</em></h1>
            <p className="sub">O que você quer fazer agora?</p>
          </div>
        </div>
      </div>

      <nav className="atalhos" aria-label="Atalhos">
        {ATALHOS.map((a) => (
          <Link key={a.para} to={a.para} className="atalho">
            <span className="atalho-icone">{a.icone}</span>
            <span className="atalho-rotulo">{a.rotulo}</span>
            <span className="atalho-desc">{a.descricao}</span>
          </Link>
        ))}
      </nav>
    </section>
  );
}
