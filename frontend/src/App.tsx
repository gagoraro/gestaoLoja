import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import Layout from './components/Layout';
import CaixaPage from './pages/CaixaPage';
import EstoquePage from './pages/EstoquePage';
import LoginPage from './pages/LoginPage';
import NovoOrcamentoPage from './pages/NovoOrcamentoPage';
import OrcamentosPage from './pages/OrcamentosPage';
import ProdutosPage from './pages/ProdutosPage';

export default function App() {
  const { carregando, logado } = useAuth();

  if (carregando) return <div className="carregando">Carregando…</div>;
  if (!logado) return <LoginPage />;

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/caixa" replace />} />
        <Route path="/caixa" element={<CaixaPage />} />
        <Route path="/produtos" element={<ProdutosPage />} />
        <Route path="/estoque" element={<EstoquePage />} />
        <Route path="/orcamentos" element={<OrcamentosPage />} />
        <Route path="/orcamentos/novo" element={<NovoOrcamentoPage />} />
        <Route path="*" element={<p className="vazio">Página não encontrada.</p>} />
      </Routes>
    </Layout>
  );
}
