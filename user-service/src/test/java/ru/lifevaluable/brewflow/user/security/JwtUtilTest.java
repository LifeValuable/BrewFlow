package ru.lifevaluable.brewflow.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.lifevaluable.brewflow.user.entity.Role;
import ru.lifevaluable.brewflow.user.entity.User;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class JwtUtilTest {
    private JwtUtil jwtUtil;

    private static final String TEST_SECRET =
        "YnJld2Zsb3ctdGVzdC1zZWNyZXQta2V5LXNob3VsZC1iZS1hdC1sZWFzdC0zMl9jaGFyYWN0ZXJzLWxvbmc=";

    private static final int TEST_EXPIRATION_MS = 3_600_000;


    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", TEST_EXPIRATION_MS);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    @DisplayName("generateToken должен создавать корректный jwt для роли USER")
    void generateToken_ShouldCreateValidJwtForUserRole() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(Role.USER);

        String token = jwtUtil.generateToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email")).isEqualTo(user.getEmail());
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("generateToken должен создавать корректный jwt для роли ADMIN")
    void generateToken_ShouldCreateValidJwtForAdminRole() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setRole(Role.ADMIN);

        String token = jwtUtil.generateToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo(user.getEmail());
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }
}
