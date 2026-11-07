import { Session } from '@supabase/supabase-js';
import { createContext, ReactNode, useContext, useEffect, useState } from 'react';
import { supabase } from './supabase';

interface Auth {
  carregando: boolean;
  loginAtivo: boolean;
  email: string | null;
  logado: boolean;
  entrar: (email: string, senha: string) => Promise<void>;
  sair: () => Promise<void>;
}

const Ctx = createContext<Auth | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Session | null>(null);
  const [carregando, setCarregando] = useState(!!supabase);

  useEffect(() => {
    if (!supabase) return;
    supabase.auth.getSession().then(({ data }) => {
      setSessao(data.session);
      setCarregando(false);
    });
    const { data } = supabase.auth.onAuthStateChange((_evento, s) => setSessao(s));
    return () => data.subscription.unsubscribe();
  }, []);

  const valor: Auth = {
    carregando,
    loginAtivo: !!supabase,
    email: sessao?.user.email ?? null,
    logado: !supabase || !!sessao,
    async entrar(email, senha) {
      if (!supabase) return;
      const { error } = await supabase.auth.signInWithPassword({ email, password: senha });
      if (error) {
        throw new Error(error.message === 'Invalid login credentials' ? 'E-mail ou senha incorretos.' : error.message);
      }
    },
    async sair() {
      await supabase?.auth.signOut();
    },
  };

  return <Ctx.Provider value={valor}>{children}</Ctx.Provider>;
}

export function useAuth() {
  const a = useContext(Ctx);
  if (!a) throw new Error('useAuth fora do AuthProvider');
  return a;
}
