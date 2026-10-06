package com.example.torneos.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenService {
    private static final String ISSUER = "torneos-api";
    private static final String ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms:86400000}")
    private long expirationMs;

    public JwtTokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void validarConfiguracion() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
        }
        if (expirationMs < 60_000) {
            throw new IllegalStateException("JWT_EXPIRATION_MS debe ser de al menos 60000 ms");
        }
    }

    public String emitir(long usuarioId, String identificacion, String rol) {
        try {
            long ahora = Instant.now().toEpochMilli();
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> claims = new HashMap<>();
            claims.put("iss", ISSUER);
            claims.put("sub", identificacion);
            claims.put("uid", usuarioId);
            claims.put("rol", rol);
            claims.put("iat", ahora / 1000);
            claims.put("exp", (ahora + expirationMs) / 1000);
            String entrada = codificar(header) + "." + codificar(claims);
            return entrada + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(firmar(entrada));
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo emitir el token", error);
        }
    }

    public Map<String, Object> validar(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) throw new IllegalArgumentException("Token inválido");
            String entrada = partes[0] + "." + partes[1];
            byte[] firmaRecibida = Base64.getUrlDecoder().decode(partes[2]);
            if (!MessageDigest.isEqual(firmar(entrada), firmaRecibida)) throw new IllegalArgumentException("Firma inválida");
            Map<String, Object> header = decodificar(partes[0]);
            if (!"HS256".equals(header.get("alg"))) throw new IllegalArgumentException("Algoritmo inválido");
            Map<String, Object> claims = decodificar(partes[1]);
            if (!ISSUER.equals(claims.get("iss"))) throw new IllegalArgumentException("Emisor inválido");
            Object expiracion = claims.get("exp");
            if (!(expiracion instanceof Number) || ((Number) expiracion).longValue() <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Token vencido");
            }
            if (!(claims.get("uid") instanceof Number) || claims.get("sub") == null || claims.get("rol") == null) {
                throw new IllegalArgumentException("Claims incompletos");
            }
            return claims;
        } catch (Exception error) {
            if (error instanceof IllegalArgumentException illegalArgument) throw illegalArgument;
            throw new IllegalArgumentException("Token inválido", error);
        }
    }

    private String codificar(Map<String, Object> valor) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(valor));
    }

    private Map<String, Object> decodificar(String valor) throws Exception {
        byte[] json = Base64.getUrlDecoder().decode(valor);
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private byte[] firmar(String entrada) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        return mac.doFinal(entrada.getBytes(StandardCharsets.UTF_8));
    }
}