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

export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'CARTAO_DEBITO' | 'CARTAO_CREDITO';

export const FORMAS: { valor: FormaPagamento; rotulo: string }[] = [
  { valor: 'DINHEIRO', rotulo: 'Dinheiro' },
  { valor: 'PIX', rotulo: 'Pix' },
  { valor: 'CARTAO_DEBITO', rotulo: 'Débito' },
  { valor: 'CARTAO_CREDITO', rotulo: 'Crédito' },
];

export interface Venda {
  id: number;
  formaPagamento: FormaPagamento;
  valorTotal: number;
  observacao: string | null;
  cancelada: boolean;
  criadoPor: string | null;
  criadoEm: string;
  itens: OrcamentoItem[];
}

export interface Caixa {
  id: number;
  aberto: boolean;
  valorAbertura: number;
  valorContado: number | null;
  observacao: string | null;
  abertoPor: string | null;
  fechadoPor: string | null;
  abertoEm: string;
  fechadoEm: string | null;
  resumo: {
    quantidadeVendas: number;
    totalVendas: number;
    totalPorForma: Record<FormaPagamento, number>;
    dinheiroEsperadoNaGaveta: number;
    diferenca: number | null;
  };
  vendas: Venda[];
}

export interface CaixaResumo {
  id: number;
  aberto: boolean;
  abertoEm: string;
  fechadoEm: string | null;
  abertoPor: string | null;
  quantidadeVendas: number;
  totalVendas: number;
}
