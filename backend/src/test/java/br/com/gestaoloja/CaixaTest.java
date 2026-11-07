package br.com.gestaoloja;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.profiles.active=dev", "spring.datasource.url=jdbc:h2:mem:caixa;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
@DirtiesContext
class CaixaTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode enviar(String url, String body, int status) throws Exception {
        String r = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return r.isEmpty() ? null : json.readTree(r);
    }

    private JsonNode obter(String url) throws Exception {
        return json.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void abrirVenderCancelarFechar() throws Exception {
        long p = enviar("/api/produtos", "{\"sku\":\"REF-1\",\"nome\":\"Refrigerante\",\"precoUnitario\":6.50,\"estoqueInicial\":10}", 201)
                .get("id").asLong();

        // sem caixa aberto: 204 e venda bloqueada
        mvc.perform(get("/api/caixa/atual")).andExpect(status().isNoContent());
        enviar("/api/caixa/vendas", "{\"formaPagamento\":\"PIX\",\"itens\":[{\"produtoId\":" + p + ",\"quantidade\":1}]}", 422);

        long caixa = enviar("/api/caixa/abrir", "{\"valorAbertura\":100}", 201).get("id").asLong();
        enviar("/api/caixa/abrir", "{\"valorAbertura\":0}", 422); // só um aberto por vez

        enviar("/api/caixa/vendas", "{\"formaPagamento\":\"DINHEIRO\",\"itens\":[{\"produtoId\":" + p + ",\"quantidade\":2}]}", 201);
        long vendaPix = enviar("/api/caixa/vendas", "{\"formaPagamento\":\"PIX\",\"itens\":[{\"produtoId\":" + p + ",\"quantidade\":3}]}", 201)
                .get("id").asLong();
        // estoque insuficiente desfaz a venda inteira
        enviar("/api/caixa/vendas", "{\"formaPagamento\":\"PIX\",\"itens\":[{\"produtoId\":" + p + ",\"quantidade\":99}]}", 422);
        assertThat(obter("/api/produtos/" + p).get("quantidadeEstoque").asInt()).isEqualTo(5);

        JsonNode atual = obter("/api/caixa/atual");
        assertThat(atual.at("/resumo/quantidadeVendas").asInt()).isEqualTo(2);
        assertThat(atual.at("/resumo/totalVendas").decimalValue()).isEqualByComparingTo("32.50");
        assertThat(atual.at("/resumo/totalPorForma/DINHEIRO").decimalValue()).isEqualByComparingTo("13.00");
        assertThat(atual.at("/resumo/dinheiroEsperadoNaGaveta").decimalValue()).isEqualByComparingTo("113.00");

        // cancelar devolve ao estoque e sai do total
        enviar("/api/caixa/vendas/" + vendaPix + "/cancelar", "", 200);
        enviar("/api/caixa/vendas/" + vendaPix + "/cancelar", "", 422);
        assertThat(obter("/api/produtos/" + p).get("quantidadeEstoque").asInt()).isEqualTo(8);
        assertThat(obter("/api/caixa/atual").at("/resumo/totalVendas").decimalValue()).isEqualByComparingTo("13.00");

        JsonNode fechado = enviar("/api/caixa/" + caixa + "/fechar", "{\"valorContado\":110}", 200);
        assertThat(fechado.get("aberto").asBoolean()).isFalse();
        assertThat(fechado.at("/resumo/diferenca").decimalValue()).isEqualByComparingTo("-3.00");
        mvc.perform(get("/api/caixa/atual")).andExpect(status().isNoContent());

        JsonNode historico = obter("/api/caixa");
        assertThat(historico.get(0).get("totalVendas").decimalValue()).isEqualByComparingTo("13.00");

        // reabrir depois de fechado funciona
        enviar("/api/caixa/abrir", "{}", 201);
    }
}
