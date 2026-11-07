const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message: string, public status: number, public campos?: Record<string, string>) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
  });
  if (!res.ok) {
    let msg = `Erro ${res.status}`;
    let campos: Record<string, string> | undefined;
    try {
      const body = await res.json();
      msg = body.detail ?? msg;
      campos = body.campos;
    } catch { /* corpo vazio */ }
    if (campos) msg += ': ' + Object.values(campos).join('; ');
    throw new ApiError(msg, res.status, campos);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export const api = {
  get: <T>(p: string) => request<T>(p),
  post: <T>(p: string, body: unknown) => request<T>(p, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(p: string, body: unknown) => request<T>(p, { method: 'PUT', body: JSON.stringify(body) }),
  del: (p: string) => request<void>(p, { method: 'DELETE' }),

  /** Baixa um arquivo binário (ex.: PDF) e dispara o download no navegador. */
  async download(p: string, nomePadrao: string) {
    const res = await fetch(`${BASE}${p}`);
    if (!res.ok) throw new ApiError(`Erro ${res.status} ao baixar arquivo`, res.status);
    const nome = /filename="?([^";]+)"?/.exec(res.headers.get('Content-Disposition') ?? '')?.[1] ?? nomePadrao;
    const url = URL.createObjectURL(await res.blob());
    const a = Object.assign(document.createElement('a'), { href: url, download: nome });
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },
};
