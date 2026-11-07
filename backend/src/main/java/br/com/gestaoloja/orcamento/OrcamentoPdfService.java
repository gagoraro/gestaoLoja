package br.com.gestaoloja.orcamento;

import br.com.gestaoloja.config.AppProperties;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    // cores da identidade visual do sistema
    private static final Color CINZA = new Color(240, 239, 231);
    private static final Color OLIVA = new Color(67, 82, 38);

    private final AppProperties props;
    private final byte[] logo;

    public OrcamentoPdfService(AppProperties props) {
        this.props = props;
        this.logo = carregarLogo();
    }

    private static byte[] carregarLogo() {
        try (InputStream in = new ClassPathResource("logo-empresa.png").getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            return null; // sem logo, o PDF sai só com o nome da loja
        }
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

        // Cabeçalho: logo à esquerda, dados do orçamento à direita
        PdfPTable cabecalho = new PdfPTable(new float[]{1.3f, 4f});
        cabecalho.setWidthPercentage(100);
        PdfPCell celLogo = new PdfPCell();
        celLogo.setBorder(Rectangle.NO_BORDER);
        if (logo != null) {
            try {
                Image img = Image.getInstance(logo);
                img.scaleToFit(95, 95);
                celLogo.addElement(img);
            } catch (Exception e) {
                celLogo.addElement(new Phrase(""));
            }
        }
        cabecalho.addCell(celLogo);

        PdfPCell celInfo = new PdfPCell();
        celInfo.setBorder(Rectangle.NO_BORDER);
        celInfo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celInfo.addElement(new Paragraph(props.loja().nome(), titulo));
        celInfo.addElement(new Paragraph("Orçamento nº " + o.getId(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, OLIVA)));
        celInfo.addElement(new Paragraph("Emitido em " + o.getCriadoEm().atZoneSameInstant(FUSO).format(DATA)
                + (o.getValidade() != null ? "  •  Válido até " + o.getValidade().format(DATA) : ""), normal));
        cabecalho.addCell(celInfo);
        doc.add(cabecalho);
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
        Font negritoBranco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell rotuloTotal = celula("TOTAL", negritoBranco, Element.ALIGN_RIGHT);
        rotuloTotal.setColspan(3);
        rotuloTotal.setBackgroundColor(OLIVA);
        tabela.addCell(rotuloTotal);
        PdfPCell valorTotal = celula(moeda.format(nz(o.getValorTotal())), negritoBranco, Element.ALIGN_RIGHT);
        valorTotal.setBackgroundColor(OLIVA);
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
