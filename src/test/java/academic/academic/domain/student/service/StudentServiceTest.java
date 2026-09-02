package academic.academic.domain.student.service;

import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.entity.RelationType;
import academic.academic.domain.parentstudent.repository.ParentStudentRepository;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.dto.ParentInfoRequest;
import academic.academic.domain.student.dto.StudentAccountRequest;
import academic.academic.domain.student.dto.StudentCreateRequest;
import academic.academic.domain.student.dto.StudentResponse;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.teacherassignment.repository.TeacherAssignmentRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock
    private ParentStudentRepository parentStudentRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, schoolClassRepository, userRepository,
                teacherAssignmentRepository, parentStudentRepository, passwordEncoder);
    }

    @Nested
    class CreateStudent {

        @Test
        void account를_생략하면_학생_본인_로그인_계정을_자동_발급한다() {
            given(userRepository.existsByLoginId(any())).willReturn(false);

            StudentResponse response = studentService.createStudent(new StudentCreateRequest(
                    "김민준", null, null, null, null, null, null, null, null, null));

            assertThat(response.account()).isNotNull();
            assertThat(response.account().loginId()).startsWith("std");
            assertThat(response.account().tempPassword()).isNotBlank();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(Role.STUDENT);
            assertThat(captor.getValue().getName()).isEqualTo("김민준");
        }

        @Test
        void account에_loginId를_지정하면_그대로_사용한다() {
            given(userRepository.existsByLoginId("kimminjun01")).willReturn(false);

            StudentResponse response = studentService.createStudent(new StudentCreateRequest(
                    "김민준", null, null, null, null, null, null, null, null,
                    new StudentAccountRequest("kimminjun01", false)));

            assertThat(response.account().loginId()).isEqualTo("kimminjun01");
        }

        @Test
        void 지정한_로그인_아이디가_중복되면_DUPLICATE_LOGIN_ID_예외() {
            given(userRepository.existsByLoginId("dup01")).willReturn(true);

            assertThatThrownBy(() -> studentService.createStudent(new StudentCreateRequest(
                    "김민준", null, null, null, null, null, null, null, null,
                    new StudentAccountRequest("dup01", false))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        @Test
        void autoGenerateLoginId가_true이면_지정한_loginId를_무시하고_자동_생성한다() {
            given(userRepository.existsByLoginId(any())).willReturn(false);

            StudentResponse response = studentService.createStudent(new StudentCreateRequest(
                    "김민준", null, null, null, null, null, null, null, null,
                    new StudentAccountRequest("ignored01", true)));

            assertThat(response.account().loginId()).isNotEqualTo("ignored01");
            assertThat(response.account().loginId()).startsWith("std");
        }

        @Test
        void 학부모_신규_생성_시_relationType이_저장된다() {
            given(userRepository.existsByLoginId(any())).willReturn(false);
            ParentInfoRequest parentInfo = new ParentInfoRequest(
                    true, null, "김민준 학부모", "010-1111-2222", "kimparent01", "pw1234", RelationType.MOTHER);

            studentService.createStudent(new StudentCreateRequest(
                    "김민준", null, null, null, null, null, null, null, parentInfo, null));

            ArgumentCaptor<ParentStudent> captor = ArgumentCaptor.forClass(ParentStudent.class);
            verify(parentStudentRepository).save(captor.capture());
            assertThat(captor.getValue().getRelationType()).isEqualTo(RelationType.MOTHER);
        }
    }
}
