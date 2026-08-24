package academic.academic.domain.homework.service;

import academic.academic.domain.homework.dto.HomeworkItemCreateRequest;
import academic.academic.domain.homework.dto.HomeworkItemResponse;
import academic.academic.domain.homework.dto.HomeworkItemUpdateRequest;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.repository.HomeworkItemRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HomeworkItemServiceTest {

    @Mock
    private HomeworkItemRepository homeworkItemRepository;
    @Mock
    private HomeworkRecordRepository homeworkRecordRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private StudentRepository studentRepository;

    private HomeworkItemService homeworkItemService;

    private SchoolClass schoolClass;
    private Student student;

    @BeforeEach
    void setUp() {
        homeworkItemService = new HomeworkItemService(
                homeworkItemRepository, homeworkRecordRepository, schoolClassRepository, studentRepository);

        schoolClass = SchoolClass.builder().name("중2 심화반").build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student, "id", 101L);
    }

    @Nested
    class Create {

        @Test
        void 반_단위_숙제_항목을_생성한다() {
            HomeworkItemCreateRequest request = new HomeworkItemCreateRequest(
                    3L, null, "단어장 Ch.5", "Unit 12-15",
                    LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 19));
            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(homeworkItemRepository.save(any(HomeworkItem.class))).willAnswer(invocation -> invocation.getArgument(0));

            HomeworkItemResponse response = homeworkItemService.create(request);

            assertThat(response.classId()).isEqualTo(3L);
            assertThat(response.studentId()).isNull();
            assertThat(response.title()).isEqualTo("단어장 Ch.5");
        }

        @Test
        void 개별_학생_단위_숙제_항목을_생성한다() {
            HomeworkItemCreateRequest request = new HomeworkItemCreateRequest(
                    null, 101L, "독후감", null, LocalDate.of(2026, 8, 17), null);
            given(studentRepository.findById(101L)).willReturn(Optional.of(student));
            given(homeworkItemRepository.save(any(HomeworkItem.class))).willAnswer(invocation -> invocation.getArgument(0));

            HomeworkItemResponse response = homeworkItemService.create(request);

            assertThat(response.studentId()).isEqualTo(101L);
            assertThat(response.classId()).isNull();
        }

        @Test
        void classId와_studentId가_모두_없으면_VALIDATION_ERROR_예외() {
            HomeworkItemCreateRequest request = new HomeworkItemCreateRequest(
                    null, null, "단어장 Ch.5", null, LocalDate.of(2026, 8, 17), null);

            assertThatThrownBy(() -> homeworkItemService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void classId와_studentId가_모두_있으면_VALIDATION_ERROR_예외() {
            HomeworkItemCreateRequest request = new HomeworkItemCreateRequest(
                    3L, 101L, "단어장 Ch.5", null, LocalDate.of(2026, 8, 17), null);

            assertThatThrownBy(() -> homeworkItemService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void 반이_존재하지_않으면_NOT_FOUND_예외() {
            HomeworkItemCreateRequest request = new HomeworkItemCreateRequest(
                    999L, null, "단어장 Ch.5", null, LocalDate.of(2026, 8, 17), null);
            given(schoolClassRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> homeworkItemService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 학생이_존재하지_않으면_NOT_FOUND_예외() {
            HomeworkItemCreateRequest request = new HomeworkItemCreateRequest(
                    null, 999L, "독후감", null, LocalDate.of(2026, 8, 17), null);
            given(studentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> homeworkItemService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class Search {

        @Test
        void week가_없으면_전체_범위로_조회한다() {
            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(homeworkItemRepository.search(eq(3L), isNull(), isNull())).willReturn(List.of());

            List<HomeworkItemResponse> result = homeworkItemService.search(3L, null);

            assertThat(result).isEmpty();
            verify(homeworkItemRepository).search(3L, null, null);
        }

        @Test
        void week가_있으면_주간_범위로_조회한다() {
            LocalDate week = LocalDate.of(2026, 8, 17);
            HomeworkItem item = HomeworkItem.builder().schoolClass(schoolClass).title("단어장 Ch.5")
                    .assignedDate(week).build();
            ReflectionTestUtils.setField(item, "id", 501L);

            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(homeworkItemRepository.search(3L, week, week.plusDays(6))).willReturn(List.of(item));

            List<HomeworkItemResponse> result = homeworkItemService.search(3L, week);

            assertThat(result).hasSize(1);
            verify(homeworkItemRepository).search(3L, week, week.plusDays(6));
        }

        @Test
        void 반이_존재하지_않으면_NOT_FOUND_예외() {
            given(schoolClassRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> homeworkItemService.search(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class Update {

        @Test
        void 제목과_기간을_수정한다() {
            HomeworkItem item = HomeworkItem.builder().schoolClass(schoolClass).title("단어장 Ch.5")
                    .assignedDate(LocalDate.of(2026, 8, 17)).build();
            ReflectionTestUtils.setField(item, "id", 501L);
            given(homeworkItemRepository.findById(501L)).willReturn(Optional.of(item));

            HomeworkItemResponse response = homeworkItemService.update(501L,
                    new HomeworkItemUpdateRequest("단어장 Ch.6", "Unit 16-20", LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 26)));

            assertThat(response.title()).isEqualTo("단어장 Ch.6");
            assertThat(response.scope()).isEqualTo("Unit 16-20");
            assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 8, 26));
        }

        @Test
        void 항목이_없으면_NOT_FOUND_예외() {
            given(homeworkItemRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> homeworkItemService.update(999L,
                    new HomeworkItemUpdateRequest("제목", null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class Delete {

        @Test
        void 항목과_소속_기록을_삭제한다() {
            HomeworkItem item = HomeworkItem.builder().schoolClass(schoolClass).title("단어장 Ch.5")
                    .assignedDate(LocalDate.of(2026, 8, 17)).build();
            ReflectionTestUtils.setField(item, "id", 501L);
            given(homeworkItemRepository.findById(501L)).willReturn(Optional.of(item));

            homeworkItemService.delete(501L);

            verify(homeworkRecordRepository, times(1)).deleteByHomeworkItemId(501L);
            verify(homeworkItemRepository, times(1)).delete(item);
        }

        @Test
        void 항목이_없으면_NOT_FOUND_예외() {
            given(homeworkItemRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> homeworkItemService.delete(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
            verify(homeworkRecordRepository, never()).deleteByHomeworkItemId(any());
        }
    }
}
