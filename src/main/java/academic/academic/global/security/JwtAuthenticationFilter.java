package academic.academic.global.security;

import academic.academic.global.exception.ErrorCode;
import academic.academic.global.exception.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Authorization: Bearer {accessToken} 헤더를 검증해 {@link AuthenticatedUser}를 request attribute로
 * 남긴다(API_명세서_V2 §2.2). 로그인/토큰 재발급/비밀번호 재설정 요청 경로만 인증 없이 통과시키고,
 * 나머지는 토큰이 없거나 무효하면 여기서 바로 401 UNAUTHENTICATED로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTRIBUTE = "academic.currentUser";

    private static final Set<String> WHITELIST = Set.of(
            "/v1/auth/login",
            "/v1/auth/refresh",
            "/v1/auth/password/reset-request",
            "/v1/auth/password/reset"
    );
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return WHITELIST.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            reject(response, "인증이 필요합니다.");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        Optional<JwtProvider.ParsedToken> parsed = jwtProvider.parseAccessToken(token);
        if (parsed.isEmpty()) {
            reject(response, "토큰이 유효하지 않거나 만료되었습니다.");
            return;
        }

        JwtProvider.ParsedToken claims = parsed.get();
        request.setAttribute(CURRENT_USER_ATTRIBUTE,
                new AuthenticatedUser(claims.userId(), claims.role(), claims.loginId()));
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(ErrorCode.UNAUTHENTICATED.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(ErrorCode.UNAUTHENTICATED, message));
    }
}
