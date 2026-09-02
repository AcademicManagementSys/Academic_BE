package academic.academic.support;

import academic.academic.domain.user.entity.Role;
import academic.academic.global.security.JwtProvider;

/**
 * {@code @WebMvcTest} 슬라이스에서 유효한 Access Token Authorization 헤더를 만들어주는 테스트 유틸.
 * 스프링 컨텍스트 없이 독립적으로 {@link JwtProvider}를 구성하므로, 각 테스트 클래스에서는
 * {@code @Import(JwtProvider.class)}로 실제 {@link academic.academic.global.security.JwtAuthenticationFilter}가
 * 요구하는 빈만 채워주면 된다. 시크릿 문자열은 application.yml의 기본값(app.jwt.secret)과 반드시 같아야
 * 필터가 서명을 검증할 수 있다 — 기본값을 바꾸면 이 값도 함께 바꿔야 한다.
 */
public final class AuthTestSupport {

    private static final JwtProvider JWT_PROVIDER = new JwtProvider(
            "local-dev-only-jwt-secret-please-override-in-prod-32bytes+", 30, 14);

    private AuthTestSupport() {
    }

    public static String bearer(Long userId, Role role) {
        return bearer(userId, role, "test-user");
    }

    public static String bearer(Long userId, Role role, String loginId) {
        return "Bearer " + JWT_PROVIDER.generateAccessToken(userId, role, loginId);
    }
}
