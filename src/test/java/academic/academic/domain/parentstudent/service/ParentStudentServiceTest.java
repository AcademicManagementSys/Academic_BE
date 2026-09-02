package academic.academic.domain.parentstudent.service;

import academic.academic.domain.parentstudent.dto.ChildResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentCreateRequest;
import academic.academic.domain.parentstudent.dto.ParentStudentResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentUpdateRequest;
import academic.academic.domain.parentstudent.entity.ParentStudent;
import academic.academic.domain.parentstudent.entity.RelationType;
import academic.academic.domain.parentstudent.repository.ParentStudentRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ParentStudentServiceTest {

    @Mock
    private ParentStudentRepository parentStudentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentRepository studentRepository;

    private ParentStudentService parentStudentService;

    private User parent;
    private Student student;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        parentStudentService = new ParentStudentService(parentStudentRepository, userRepository, studentRepository);

        parent = User.builder().name("김민준 학부모").role(Role.PARENT).loginId("parent1").passwordHash("hash").build();
        ReflectionTestUtils.setField(parent, "id", 45L);

        schoolClass = SchoolClass.builder().name("중2 심화반").build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student, "id", 101L);
    }

    @Nested
    class CreateLink {

        @Test
        void 부모_구분을_포함해_연결을_생성한다() {
            given(userRepository.findById(45L)).willReturn(Optional.of(parent));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student));
            given(parentStudentRepository.existsByParentUserIdAndStudentId(45L, 101L)).willReturn(false);
            given(parentStudentRepository.save(any(ParentStudent.class))).willAnswer(invocation -> invocation.getArgument(0));

            ParentStudentResponse response = parentStudentService.createLink(
                    new ParentStudentCreateRequest(45L, 101L, RelationType.MOTHER));

            assertThat(response.relationType()).isEqualTo(RelationType.MOTHER);
            assertThat(response.parentUserId()).isEqualTo(45L);
            assertThat(response.studentId()).isEqualTo(101L);
        }

        @Test
        void 학부모_계정이_없으면_NOT_FOUND_예외() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> parentStudentService.createLink(
                    new ParentStudentCreateRequest(99L, 101L, RelationType.FATHER)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 학부모가_아니면_VALIDATION_ERROR_예외() {
            User teacher = User.builder().name("김선생").role(Role.TEACHER).loginId("teacher1").passwordHash("hash").build();
            ReflectionTestUtils.setField(teacher, "id", 2L);
            given(userRepository.findById(2L)).willReturn(Optional.of(teacher));

            assertThatThrownBy(() -> parentStudentService.createLink(
                    new ParentStudentCreateRequest(2L, 101L, RelationType.FATHER)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void 이미_연결된_관계면_VALIDATION_ERROR_예외() {
            given(userRepository.findById(45L)).willReturn(Optional.of(parent));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student));
            given(parentStudentRepository.existsByParentUserIdAndStudentId(45L, 101L)).willReturn(true);

            assertThatThrownBy(() -> parentStudentService.createLink(
                    new ParentStudentCreateRequest(45L, 101L, RelationType.FATHER)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    class UpdateLink {

        @Test
        void 부모_구분을_변경한다() {
            ParentStudent link = ParentStudent.of(parent, student, RelationType.OTHER);
            given(parentStudentRepository.findById(7L)).willReturn(Optional.of(link));

            ParentStudentResponse response = parentStudentService.updateLink(7L,
                    new ParentStudentUpdateRequest(RelationType.MOTHER));

            assertThat(response.relationType()).isEqualTo(RelationType.MOTHER);
        }

        @Test
        void 연결이_없으면_NOT_FOUND_예외() {
            given(parentStudentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> parentStudentService.updateLink(999L,
                    new ParentStudentUpdateRequest(RelationType.MOTHER)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class GetChildren {

        @Test
        void 자녀_목록에_부모_구분을_포함한다() {
            ParentStudent link = ParentStudent.of(parent, student, RelationType.MOTHER);
            given(parentStudentRepository.findByParentUserId(45L)).willReturn(List.of(link));

            List<ChildResponse> children = parentStudentService.getChildren(45L);

            assertThat(children).hasSize(1);
            assertThat(children.get(0).relationType()).isEqualTo(RelationType.MOTHER);
            assertThat(children.get(0).className()).isEqualTo("중2 심화반");
        }
    }
}
