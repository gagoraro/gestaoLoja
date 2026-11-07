export interface Produto {
  id: number;
  sku: string;
  nome: string;
  descricao: string | null;
  precoUnitario: number;
  quantidadeEstoque: number;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ProdutoInput {
  sku: string;
  nome: string;
  descricao?: string;
  precoUnitario: number;
  estoqueInicial?: number;
}

export type TipoMovimentacao = 'ENTRADA' | 'SAIDA';

export interface Movimentacao {
  id: number;
  produtoId: number;
  tipo: TipoMovimentacao;
  quantidade: number;
  saldoApos: number;
  motivo: string | null;
  criadoEm: string;
}

export interface OrcamentoResumo {
  id: number;
  clienteNome: string;
  valorTotal: number;
  validade: string | null;
  criadoEm: string;
}

export interface OrcamentoItem {
  produtoId: number;
  produtoNome: string;
  precoUnitario: number;
  quantidade: number;
  subtotal: number;
}

export interface Orcamento extends OrcamentoResumo {
  clienteContato: string | null;
  observacoes: string | null;
  itens: OrcamentoItem[];
}

export interface OrcamentoInput {
  clienteNome: string;
  clienteContato?: string;
  observacoes?: string;
  itens: { produtoId: number; quantidade: number }[];
}
