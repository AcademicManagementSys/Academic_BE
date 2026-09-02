package academic.academic.domain.auth.service;

import academic.academic.domain.auth.dto.LoginRequest;
import academic.academic.domain.auth.dto.LoginResponse;
import academic.academic.domain.auth.dto.PasswordResetConfirmRequest;
import academic.academic.domain.auth.dto.PasswordResetRequestResponse;
import academic.academic.domain.auth.dto.TokenPairResponse;
import academic.academic.domain.auth.entity.PasswordResetToken;
import academic.academic.domain.auth.entity.RefreshToken;
import academic.academic.domain.auth.repository.PasswordResetTokenRepository;
import academic.academic.domain.auth.repository.RefreshTokenRepository;
import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.entity.RelationType;
import academic.academic.domain.parentstudent.repository.ParentStudentRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import academic.academic.global.security.JwtProvider;
import academic.academic.global.util.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ParentStudentRepository parentStudentRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtProvider jwtProvider = new JwtProvider(
            "test-jwt-secret-please-make-it-long-enough-32bytes+", 30, 14);

    private AuthService authService;

    private User teacher;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, parentStudentRepository, refreshTokenRepository,
                passwordResetTokenRepository, passwordEncoder, jwtProvider);

        teacher = User.builder().name("김선생").role(Role.TEACHER).loginId("teacher1")
                .passwordHash(passwordEncoder.encode("pw1234")).build();
        ReflectionTestUtils.setField(teacher, "id", 2L);
    }

    @Nested
    class Login {

        @Test
        void 로그인에_성공하면_토큰과_사용자_정보를_반환한다() {
            given(userRepository.findByLoginId("teacher1")).willReturn(Optional.of(teacher));

            LoginResponse response = authService.login(new LoginRequest("teacher1", "pw1234"));

            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.user().id()).isEqualTo(2L);
            assertThat(response.user().role()).isEqualTo(Role.TEACHER);
            assertThat(response.user().hasMultipleChildren()).isFalse();
            assertThat(jwtProvider.parseAccessToken(response.accessToken())).isPresent();
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        void 학부모_자녀가_2명_이상이면_hasMultipleChildren_true() {
            User parent = User.builder().name("학부모").role(Role.PARENT).loginId("parent1")
                    .passwordHash(passwordEncoder.encode("pw1234")).build();
            ReflectionTestUtils.setField(parent, "id", 45L);
            given(userRepository.findByLoginId("parent1")).willReturn(Optional.of(parent));
            given(parentStudentRepository.findByParentUserId(45L)).willReturn(
                    List.of(mockLink(), mockLink()));

            LoginResponse response = authService.login(new LoginRequest("parent1", "pw1234"));

            assertThat(response.user().hasMultipleChildren()).isTrue();
        }

        @Test
        void 아이디가_없으면_UNAUTHENTICATED_예외() {
            given(userRepository.findByLoginId("nobody")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "pw1234")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED);
        }

        @Test
        void 비밀번호가_틀리면_UNAUTHENTICATED_예외() {
            given(userRepository.findByLoginId("teacher1")).willReturn(Optional.of(teacher));

            assertThatThrownBy(() -> authService.login(new LoginRequest("teacher1", "wrong-pw")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED);
        }

        @Test
        void 비활성화된_계정이면_UNAUTHENTICATED_예외() {
            teacher.changeActive(false);
            given(userRepository.findByLoginId("teacher1")).willReturn(Optional.of(teacher));

            assertThatThrownBy(() -> authService.login(new LoginRequest("teacher1", "pw1234")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED);
        }

        private ParentStudent mockLink() {
            Student student = Student.builder().name("자녀").build();
            return ParentStudent.of(teacher, student, RelationType.MOTHER);
        }
    }

    @Nested
    class Refresh {

        @Test
        void 유효한_리프레시_토큰이면_새_토큰쌍을_발급하고_기존_토큰을_폐기한다() {
            String refreshTokenValue = jwtProvider.generateRefreshToken(2L, Role.TEACHER, "teacher1");
            RefreshToken stored = RefreshToken.builder()
                    .user(teacher).tokenHash(TokenHasher.sha256Hex(refreshTokenValue))
                    .expiresAt(LocalDateTime.now().plusDays(1)).build();
            given(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(refreshTokenValue)))
                    .willReturn(Optional.of(stored));
            given(userRepository.findById(2L)).willReturn(Optional.of(teacher));

            TokenPairResponse response = authService.refresh(refreshTokenValue);

            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotEqualTo(refreshTokenValue);
            assertThat(stored.isUsable(LocalDateTime.now())).isFalse();
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        void 폐기된_토큰으로_재발급하면_UNAUTHENTICATED_예외() {
            String refreshTokenValue = jwtProvider.generateRefreshToken(2L, Role.TEACHER, "teacher1");
            RefreshToken stored = RefreshToken.builder()
                    .user(teacher).tokenHash(TokenHasher.sha256Hex(refreshTokenValue))
                    .expiresAt(LocalDateTime.now().plusDays(1)).build();
            stored.revoke();
            given(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(refreshTokenValue)))
                    .willReturn(Optional.of(stored));

            assertThatThrownBy(() -> authService.refresh(refreshTokenValue))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED);
        }

        @Test
        void 서버에_없는_토큰이면_UNAUTHENTICATED_예외() {
            String refreshTokenValue = jwtProvider.generateRefreshToken(2L, Role.TEACHER, "teacher1");
            given(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(refreshTokenValue)))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(refreshTokenValue))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED);
        }

        @Test
        void 액세스_토큰으로_재발급을_시도하면_UNAUTHENTICATED_예외() {
            String accessToken = jwtProvider.generateAccessToken(2L, Role.TEACHER, "teacher1");

            assertThatThrownBy(() -> authService.refresh(accessToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Nested
    class Logout {

        @Test
        void 로그아웃하면_리프레시_토큰을_폐기한다() {
            String refreshTokenValue = jwtProvider.generateRefreshToken(2L, Role.TEACHER, "teacher1");
            RefreshToken stored = RefreshToken.builder()
                    .user(teacher).tokenHash(TokenHasher.sha256Hex(refreshTokenValue))
                    .expiresAt(LocalDateTime.now().plusDays(1)).build();
            given(refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(refreshTokenValue)))
                    .willReturn(Optional.of(stored));

            authService.logout(refreshTokenValue);

            assertThat(stored.isUsable(LocalDateTime.now())).isFalse();
        }

        @Test
        void 이미_무효한_토큰으로_로그아웃해도_예외를_던지지_않는다() {
            authService.logout("garbage-token");
        }
    }

    @Nested
    class PasswordReset {

        @Test
        void 재설정_토큰을_발급한다() {
            given(userRepository.findByLoginId("teacher1")).willReturn(Optional.of(teacher));

            PasswordResetRequestResponse response = authService.requestPasswordReset("teacher1");

            assertThat(response.resetToken()).isNotBlank();
            assertThat(response.expiresAt()).isAfter(LocalDateTime.now());
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        void 존재하지_않는_아이디면_NOT_FOUND_예외() {
            given(userRepository.findByLoginId("nobody")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.requestPasswordReset("nobody"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 유효한_토큰으로_비밀번호를_변경한다() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(teacher).tokenHash(TokenHasher.sha256Hex("raw-token"))
                    .expiresAt(LocalDateTime.now().plusMinutes(30)).build();
            given(passwordResetTokenRepository.findByTokenHash(TokenHasher.sha256Hex("raw-token")))
                    .willReturn(Optional.of(token));
            String oldHash = teacher.getPasswordHash();

            authService.confirmPasswordReset(new PasswordResetConfirmRequest("raw-token", "newPassword1!"));

            assertThat(teacher.getPasswordHash()).isNotEqualTo(oldHash);
            assertThat(passwordEncoder.matches("newPassword1!", teacher.getPasswordHash())).isTrue();
            assertThat(token.isUsable(LocalDateTime.now())).isFalse();
        }

        @Test
        void 만료된_토큰이면_UNAUTHENTICATED_예외() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(teacher).tokenHash(TokenHasher.sha256Hex("raw-token"))
                    .expiresAt(LocalDateTime.now().minusMinutes(1)).build();
            given(passwordResetTokenRepository.findByTokenHash(TokenHasher.sha256Hex("raw-token")))
                    .willReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.confirmPasswordReset(
                    new PasswordResetConfirmRequest("raw-token", "newPassword1!")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED);
        }

        @Test
        void 존재하지_않는_토큰이면_UNAUTHENTICATED_예외() {
            given(passwordResetTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.confirmPasswordReset(
                    new PasswordResetConfirmRequest("garbage", "newPassword1!")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHENTICATED);
        }
    }
}
