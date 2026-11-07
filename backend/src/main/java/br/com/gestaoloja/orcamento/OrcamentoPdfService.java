package br.com.gestaoloja.orcamento;

import br.com.gestaoloja.config.AppProperties;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Gera o PDF do orçamento no servidor (layout consistente, independente do navegador). */
@Service
public class OrcamentoPdfService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Color CINZA = new Color(240, 240, 240);

    private final AppProperties props;

    public OrcamentoPdfService(AppProperties props) {
        this.props = props;
    }

    public byte[] gerar(Orcamento o) {
        NumberFormat moeda = NumberFormat.getCurrencyInstance(PT_BR);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font negrito = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

        doc.add(new Paragraph(props.loja().nome(), titulo));
        doc.add(new Paragraph("Orçamento nº " + o.getId(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)));
        doc.add(new Paragraph("Emitido em " + o.getCriadoEm().atZoneSameInstant(FUSO).format(DATA)
                + (o.getValidade() != null ? "  •  Válido até " + o.getValidade().format(DATA) : ""), normal));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Cliente: " + o.getClienteNome(), negrito));
        if (temTexto(o.getClienteContato())) {
            doc.add(new Paragraph("Contato: " + o.getClienteContato(), normal));
        }
        doc.add(Chunk.NEWLINE);

        PdfPTable tabela = new PdfPTable(new float[]{5f, 1.2f, 2f, 2f});
        tabela.setWidthPercentage(100);
        for (String h : new String[]{"Produto", "Qtd", "Preço unit.", "Subtotal"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, negrito));
            c.setBackgroundColor(CINZA);
            c.setPadding(6);
            c.setHorizontalAlignment(h.equals("Produto") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            tabela.addCell(c);
        }
        for (OrcamentoItem i : o.getItens()) {
            tabela.addCell(celula(i.getProdutoNome(), normal, Element.ALIGN_LEFT));
            tabela.addCell(celula(String.valueOf(i.getQuantidade()), normal, Element.ALIGN_RIGHT));
            tabela.addCell(celula(moeda.format(i.getPrecoUnitario()), normal, Element.ALIGN_RIGHT));
            tabela.addCell(celula(moeda.format(i.getSubtotal()), normal, Element.ALIGN_RIGHT));
        }
        PdfPCell rotuloTotal = celula("TOTAL", negrito, Element.ALIGN_RIGHT);
        rotuloTotal.setColspan(3);
        rotuloTotal.setBackgroundColor(CINZA);
        tabela.addCell(rotuloTotal);
        PdfPCell valorTotal = celula(moeda.format(nz(o.getValorTotal())), negrito, Element.ALIGN_RIGHT);
        valorTotal.setBackgroundColor(CINZA);
        tabela.addCell(valorTotal);
        doc.add(tabela);

        if (temTexto(o.getObservacoes())) {
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Observações", negrito));
            doc.add(new Paragraph(o.getObservacoes(), normal));
        }

        doc.close();
        return out.toByteArray();
    }

    private static PdfPCell celula(String texto, Font fonte, int alinhamento) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setPadding(6);
        c.setHorizontalAlignment(alinhamento);
        return c;
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
