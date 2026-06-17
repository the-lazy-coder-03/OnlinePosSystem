package org.example.onlinepossystem.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds:7200}") long expirationSeconds
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes long");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", userDetails.getUsername());
        payload.put("roles", roles);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());

        try {
            String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception ex) {
            throw new JwtAuthenticationException("Unable to create JWT", ex);
        }
    }

    public String extractUsername(String token) {
        Object subject = parseClaims(token).get("sub");
        if (!(subject instanceof String username) || username.isBlank()) {
            throw new JwtAuthenticationException("JWT subject is missing");
        }
        return username;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isExpired(parseClaims(token));
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Map<String, Object> parseClaims(String token) {
        String[] parts = splitToken(token);
        verifySignature(parts);

        try {
            Map<String, Object> claims = objectMapper.readValue(base64UrlDecode(parts[1]), CLAIMS_TYPE);
            if (isExpired(claims)) {
                throw new JwtAuthenticationException("JWT has expired");
            }
            return claims;
        } catch (JwtAuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtAuthenticationException("JWT payload is invalid", ex);
        }
    }

    private boolean isExpired(Map<String, Object> claims) {
        Object exp = claims.get("exp");
        if (!(exp instanceof Number expiration)) {
            throw new JwtAuthenticationException("JWT expiration is missing");
        }
        return expiration.longValue() <= Instant.now().getEpochSecond();
    }

    private String[] splitToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtAuthenticationException("JWT is missing");
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new JwtAuthenticationException("JWT must have header, payload, and signature");
        }
        return parts;
    }

    private void verifySignature(String[] parts) {
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new JwtAuthenticationException("JWT signature is invalid");
        }
    }

    private String base64UrlJson(Map<String, Object> value) throws Exception {
        return base64UrlEncode(objectMapper.writeValueAsBytes(value));
    }

    private byte[] base64UrlDecode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            throw new JwtAuthenticationException("JWT Base64Url value is invalid", ex);
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new JwtAuthenticationException("Unable to sign JWT", ex);
        }
    }
}
