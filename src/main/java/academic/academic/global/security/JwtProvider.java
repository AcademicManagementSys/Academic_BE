package academic.academic.global.security;

import academic.academic.domain.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Access/Refresh Token 발급·검증. Refresh Token도 서명된 JWT로 발급하되(자체 검증 가능),
 * 폐기·로테이션을 위해 {@link academic.academic.domain.auth.entity.RefreshToken}으로 서버에도
 * 해시를 저장해 이중으로 관리한다.
 */
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_LOGIN_ID = "loginId";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                        @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
                        @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
    }

    public String generateAccessToken(Long userId, Role role, String loginId) {
        return generateToken(userId, role, loginId, TYPE_ACCESS, accessTokenTtl);
    }

    public String generateRefreshToken(Long userId, Role role, String loginId) {
        return generateToken(userId, role, loginId, TYPE_REFRESH, refreshTokenTtl);
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public Optional<ParsedToken> parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public Optional<ParsedToken> parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private String generateToken(Long userId, Role role, String loginId, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_LOGIN_ID, loginId)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    private Optional<ParsedToken> parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            Long userId = Long.valueOf(claims.getSubject());
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            String loginId = claims.get(CLAIM_LOGIN_ID, String.class);
            return Optional.of(new ParsedToken(userId, role, loginId, claims.getExpiration()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record ParsedToken(Long userId, Role role, String loginId, Date expiration) {
    }
}
