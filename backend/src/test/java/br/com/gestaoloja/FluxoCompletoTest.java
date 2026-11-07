package br.com.gestaoloja;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FluxoCompletoTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode enviar(String url, String body, int status) throws Exception {
        String r = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return json.readTree(r);
    }

    @Test
    void produtoEstoqueOrcamentoPdf() throws Exception {
        JsonNode p = enviar("/api/produtos",
                "{\"sku\":\"CAM-01\",\"nome\":\"Camiseta\",\"precoUnitario\":49.90,\"estoqueInicial\":10}", 201);
        long id = p.get("id").asLong();
        assertThat(p.get("quantidadeEstoque").asInt()).isEqualTo(10);

        // SKU duplicado
        enviar("/api/produtos", "{\"sku\":\"cam-01\",\"nome\":\"X\",\"precoUnitario\":1}", 422);

        // entrada e saída
        enviar("/api/produtos/" + id + "/movimentacoes", "{\"tipo\":\"ENTRADA\",\"quantidade\":5}", 201);
        JsonNode saida = enviar("/api/produtos/" + id + "/movimentacoes", "{\"tipo\":\"SAIDA\",\"quantidade\":3}", 201);
        assertThat(saida.get("saldoApos").asInt()).isEqualTo(12);
        // não deixa negativo
        enviar("/api/produtos/" + id + "/movimentacoes", "{\"tipo\":\"SAIDA\",\"quantidade\":13}", 422);
        // quantidade inválida
        enviar("/api/produtos/" + id + "/movimentacoes", "{\"tipo\":\"SAIDA\",\"quantidade\":0}", 400);

        mvc.perform(get("/api/produtos/" + id + "/movimentacoes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));

        // orçamento: preço vem do servidor, total = 3 * 49.90
        JsonNode o = enviar("/api/orcamentos",
                "{\"clienteNome\":\"Maria\",\"itens\":[{\"produtoId\":" + id + ",\"quantidade\":2},{\"produtoId\":" + id + ",\"quantidade\":1}]}", 201);
        assertThat(o.get("valorTotal").decimalValue()).isEqualByComparingTo("149.70");
        assertThat(o.get("itens").size()).isEqualTo(1);

        byte[] pdf = mvc.perform(get("/api/orcamentos/" + o.get("id").asLong() + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("orcamento-")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        // orçamento sem itens
        enviar("/api/orcamentos", "{\"clienteNome\":\"Maria\",\"itens\":[]}", 400);

        // exclusão lógica não quebra o orçamento existente
        mvc.perform(delete("/api/produtos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/orcamentos/" + o.get("id").asLong())).andExpect(status().isOk());
    }
}
