package academic.academic.global.security;

import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.entity.RelationType;
import academic.academic.domain.parentstudent.repository.ParentStudentRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.teacherassignment.repository.TeacherAssignmentRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ParentStudentRepository parentStudentRepository;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(
                schoolClassRepository, teacherAssignmentRepository, studentRepository, parentStudentRepository);
    }

    @Nested
    class RequireRole {

        @Test
        void 허용된_역할이면_통과한다() {
            AuthenticatedUser me = new AuthenticatedUser(1L, Role.ADMIN, "admin1");

            authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        }

        @Test
        void 허용되지_않은_역할이면_FORBIDDEN_ROLE_예외() {
            AuthenticatedUser me = new AuthenticatedUser(1L, Role.PARENT, "parent1");

            assertThatThrownBy(() -> authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN_ROLE);
        }
    }

    @Nested
    class TeacherOwnsClass {

        @Test
        void 담당_반이면_통과한다() {
            given(schoolClassRepository.existsByIdAndTeacherId(3L, 2L)).willReturn(true);

            authorizationService.requireTeacherOwnsClass(2L, 3L);
        }

        @Test
        void TeacherAssignment로_배정된_반도_통과한다() {
            given(schoolClassRepository.existsByIdAndTeacherId(3L, 2L)).willReturn(false);
            given(teacherAssignmentRepository.existsByTeacherIdAndSchoolClassId(2L, 3L)).willReturn(true);

            authorizationService.requireTeacherOwnsClass(2L, 3L);
        }

        @Test
        void 담당_반이_아니면_FORBIDDEN_SCOPE_예외() {
            given(schoolClassRepository.existsByIdAndTeacherId(3L, 2L)).willReturn(false);
            given(teacherAssignmentRepository.existsByTeacherIdAndSchoolClassId(2L, 3L)).willReturn(false);

            assertThatThrownBy(() -> authorizationService.requireTeacherOwnsClass(2L, 3L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN_SCOPE);
        }
    }

    @Nested
    class RequireCanViewStudent {

        @Test
        void admin은_항상_통과한다() {
            AuthenticatedUser me = new AuthenticatedUser(1L, Role.ADMIN, "admin1");

            authorizationService.requireCanViewStudent(me, 101L);
        }

        @Test
        void 담당_학생이면_teacher도_통과한다() {
            AuthenticatedUser me = new AuthenticatedUser(2L, Role.TEACHER, "teacher1");
            given(teacherAssignmentRepository.existsByTeacherIdAndStudentId(2L, 101L)).willReturn(true);

            authorizationService.requireCanViewStudent(me, 101L);
        }

        @Test
        void 자녀면_parent도_통과한다() {
            AuthenticatedUser me = new AuthenticatedUser(45L, Role.PARENT, "parent1");
            given(parentStudentRepository.existsByParentUserIdAndStudentId(45L, 101L)).willReturn(true);

            authorizationService.requireCanViewStudent(me, 101L);
        }

        @Test
        void 본인이면_student도_통과한다() {
            AuthenticatedUser me = new AuthenticatedUser(200L, Role.STUDENT, "student1");
            given(studentRepository.existsByIdAndUserId(101L, 200L)).willReturn(true);

            authorizationService.requireCanViewStudent(me, 101L);
        }

        @Test
        void 자녀가_아닌_parent는_FORBIDDEN_SCOPE_예외() {
            AuthenticatedUser me = new AuthenticatedUser(45L, Role.PARENT, "parent1");
            given(parentStudentRepository.existsByParentUserIdAndStudentId(45L, 101L)).willReturn(false);

            assertThatThrownBy(() -> authorizationService.requireCanViewStudent(me, 101L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN_SCOPE);
        }
    }

    @Nested
    class CanViewClassScopedContent {

        @Test
        void 자녀가_그_반_소속이면_parent는_true() {
            User parentUser = User.builder().name("학부모").role(Role.PARENT).loginId("parent1").passwordHash("hash").build();
            ReflectionTestUtils.setField(parentUser, "id", 45L);
            SchoolClass schoolClass = SchoolClass.builder().name("중2 심화반").build();
            ReflectionTestUtils.setField(schoolClass, "id", 3L);
            Student student = Student.builder().name("김민준").schoolClass(schoolClass).build();
            ReflectionTestUtils.setField(student, "id", 101L);
            ParentStudent link = ParentStudent.of(parentUser, student, RelationType.MOTHER);
            given(parentStudentRepository.findByParentUserId(45L)).willReturn(List.of(link));

            AuthenticatedUser me = new AuthenticatedUser(45L, Role.PARENT, "parent1");

            assertThat(authorizationService.canViewClassScopedContent(me, 3L)).isTrue();
            assertThat(authorizationService.canViewClassScopedContent(me, 999L)).isFalse();
        }

        @Test
        void 본인_반이면_student는_true() {
            SchoolClass schoolClass = SchoolClass.builder().name("중2 심화반").build();
            ReflectionTestUtils.setField(schoolClass, "id", 3L);
            Student student = Student.builder().name("김민준").schoolClass(schoolClass).build();
            given(studentRepository.findByUserId(200L)).willReturn(Optional.of(student));

            AuthenticatedUser me = new AuthenticatedUser(200L, Role.STUDENT, "student1");

            assertThat(authorizationService.canViewClassScopedContent(me, 3L)).isTrue();
            assertThat(authorizationService.canViewClassScopedContent(me, 999L)).isFalse();
        }
    }

    @Nested
    class RequireAuthorAndOwnsClass {

        @Test
        void 본인이_작성했고_담당_반이면_통과한다() {
            given(schoolClassRepository.existsByIdAndTeacherId(3L, 2L)).willReturn(true);
            AuthenticatedUser me = new AuthenticatedUser(2L, Role.TEACHER, "teacher1");

            authorizationService.requireAuthorAndOwnsClass(me, 2L, 3L);
        }

        @Test
        void 본인이_작성하지_않았으면_FORBIDDEN_SCOPE_예외() {
            given(schoolClassRepository.existsByIdAndTeacherId(3L, 2L)).willReturn(true);
            AuthenticatedUser me = new AuthenticatedUser(2L, Role.TEACHER, "teacher1");

            assertThatThrownBy(() -> authorizationService.requireAuthorAndOwnsClass(me, 999L, 3L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN_SCOPE);
        }
    }
}
