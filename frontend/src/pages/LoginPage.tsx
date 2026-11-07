import { FormEvent, useState } from 'react';
import { useAuth } from '../auth/AuthContext';

export default function LoginPage() {
  const { entrar } = useAuth();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      await entrar(email.trim(), senha);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="login">
      <div className="cartao">
        <img src="/logo.png" alt="Logo A Vitoriosa de Angra" />
        <p className="eyebrow">Gestão da loja</p>
        <h1>A Vitoriosa <em>de Angra</em></h1>
        <form className="form" onSubmit={submit}>
          <label>E-mail
            <input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </label>
          <label>Senha
            <input type="password" autoComplete="current-password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
          </label>
          {erro && <p className="erro">{erro}</p>}
          <button type="submit" className="btn-grande" disabled={enviando}>{enviando ? 'Entrando…' : 'Entrar'}</button>
        </form>
      </div>
    </div>
  );
}
