import { createClient, SupabaseClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL as string | undefined;
const chave = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined;

/** null = login desligado (modo local, backend no perfil dev). */
export const supabase: SupabaseClient | null =
  url && chave && !chave.startsWith('<') ? createClient(url, chave) : null;

export async function tokenAtual(): Promise<string | null> {
  if (!supabase) return null;
  const { data } = await supabase.auth.getSession();
  return data.session?.access_token ?? null;
}
