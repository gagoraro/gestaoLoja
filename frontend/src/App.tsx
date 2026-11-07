import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import ProdutosPage from './pages/ProdutosPage';
import OrcamentosPage from './pages/OrcamentosPage';
import NovoOrcamentoPage from './pages/NovoOrcamentoPage';

export default function App() {
  return (
    <div className="layout">
      <header className="topbar">
        <strong className="logo">Gestão Loja</strong>
        <nav>
          <NavLink to="/produtos">Produtos & Estoque</NavLink>
          <NavLink to="/orcamentos">Orçamentos</NavLink>
        </nav>
      </header>
      <main className="conteudo">
        <Routes>
          <Route path="/" element={<Navigate to="/produtos" replace />} />
          <Route path="/produtos" element={<ProdutosPage />} />
          <Route path="/orcamentos" element={<OrcamentosPage />} />
          <Route path="/orcamentos/novo" element={<NovoOrcamentoPage />} />
          <Route path="*" element={<p>Página não encontrada.</p>} />
        </Routes>
      </main>
    </div>
  );
}
