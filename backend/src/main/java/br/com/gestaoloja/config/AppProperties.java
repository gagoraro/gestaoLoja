package br.com.gestaoloja.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Loja loja) {
    public record Cors(List<String> allowedOrigins) {}
    public record Loja(String nome, int validadeOrcamentoDias) {}
}
