package academic.academic.domain.user.service;

import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.teacherassignment.repository.TeacherAssignmentRepository;
import academic.academic.domain.user.dto.PasswordResetResponse;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private TeacherAssignmentRepository teacherAssignmentRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    private User teacher;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, schoolClassRepository, teacherAssignmentRepository, passwordEncoder);

        teacher = User.builder().name("김선생").role(Role.TEACHER).loginId("teacher1").passwordHash("old-hash").build();
        ReflectionTestUtils.setField(teacher, "id", 1L);
    }

    @Nested
    class ResetPassword {

        @Test
        void 임시_비밀번호를_발급하고_최초_로그인_변경_플래그를_내려준다() {
            given(userRepository.findById(1L)).willReturn(Optional.of(teacher));

            PasswordResetResponse response = userService.resetPassword(1L);

            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.loginId()).isEqualTo("teacher1");
            assertThat(response.tempPassword()).isNotBlank();
            assertThat(response.mustChangePassword()).isTrue();
            assertThat(passwordEncoder.matches(response.tempPassword(), teacher.getPasswordHash())).isTrue();
            assertThat(teacher.getPasswordHash()).isNotEqualTo("old-hash");
        }

        @Test
        void 매번_다른_임시_비밀번호를_발급한다() {
            given(userRepository.findById(1L)).willReturn(Optional.of(teacher));

            String first = userService.resetPassword(1L).tempPassword();
            String second = userService.resetPassword(1L).tempPassword();

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        void 계정이_없으면_NOT_FOUND_예외() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.resetPassword(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }
}
