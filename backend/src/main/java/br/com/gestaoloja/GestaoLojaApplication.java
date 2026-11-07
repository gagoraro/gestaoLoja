package br.com.gestaoloja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GestaoLojaApplication {
    public static void main(String[] args) {
        SpringApplication.run(GestaoLojaApplication.class, args);
    }
}
