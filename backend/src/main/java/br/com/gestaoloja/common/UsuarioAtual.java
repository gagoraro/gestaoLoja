package br.com.gestaoloja.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Identifica quem está fazendo a operação (e-mail do usuário logado no Supabase). */
public final class UsuarioAtual {
    private UsuarioAtual() {}

    public static String identificacao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            return email != null ? email : jwt.getSubject();
        }
        return "local";
    }
}
