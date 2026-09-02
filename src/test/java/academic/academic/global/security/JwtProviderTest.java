package academic.academic.global.security;

import academic.academic.domain.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider("test-jwt-secret-please-make-it-long-enough-32bytes+", 30, 14);
    }

    @Test
    void 액세스_토큰을_발급하고_파싱하면_원래_정보가_나온다() {
        String token = jwtProvider.generateAccessToken(1L, Role.TEACHER, "teacher1");

        Optional<JwtProvider.ParsedToken> parsed = jwtProvider.parseAccessToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(1L);
        assertThat(parsed.get().role()).isEqualTo(Role.TEACHER);
        assertThat(parsed.get().loginId()).isEqualTo("teacher1");
    }

    @Test
    void 리프레시_토큰을_발급하고_파싱하면_원래_정보가_나온다() {
        String token = jwtProvider.generateRefreshToken(2L, Role.PARENT, "parent1");

        Optional<JwtProvider.ParsedToken> parsed = jwtProvider.parseRefreshToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(2L);
        assertThat(parsed.get().role()).isEqualTo(Role.PARENT);
    }

    @Test
    void 액세스_토큰을_리프레시_토큰으로_파싱하면_빈값이다() {
        String accessToken = jwtProvider.generateAccessToken(1L, Role.ADMIN, "admin1");

        assertThat(jwtProvider.parseRefreshToken(accessToken)).isEmpty();
    }

    @Test
    void 리프레시_토큰을_액세스_토큰으로_파싱하면_빈값이다() {
        String refreshToken = jwtProvider.generateRefreshToken(1L, Role.ADMIN, "admin1");

        assertThat(jwtProvider.parseAccessToken(refreshToken)).isEmpty();
    }

    @Test
    void 형식이_잘못된_토큰은_빈값이다() {
        assertThat(jwtProvider.parseAccessToken("not-a-jwt")).isEmpty();
    }

    @Test
    void 다른_시크릿으로_서명된_토큰은_빈값이다() {
        JwtProvider otherProvider = new JwtProvider("completely-different-secret-value-32bytes-min!!", 30, 14);
        String token = otherProvider.generateAccessToken(1L, Role.ADMIN, "admin1");

        assertThat(jwtProvider.parseAccessToken(token)).isEmpty();
    }

    @Test
    void 만료_시간이_지난_토큰은_빈값이다() {
        JwtProvider expiredProvider = new JwtProvider(
                "test-jwt-secret-please-make-it-long-enough-32bytes+", 0, 0);
        String token = expiredProvider.generateAccessToken(1L, Role.ADMIN, "admin1");

        // TTL 0분 발급 직후에도 exp<=iat 이라 만료된 것으로 처리되어야 한다.
        assertThat(jwtProvider.parseAccessToken(token)).isEmpty();
    }
}
