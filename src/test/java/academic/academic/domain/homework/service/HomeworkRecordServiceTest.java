package academic.academic.domain.homework.service;

import academic.academic.domain.homework.dto.HomeworkItemRecordsGroup;
import academic.academic.domain.homework.dto.HomeworkRecordBulkRequest;
import academic.academic.domain.homework.dto.HomeworkRecordItem;
import academic.academic.domain.homework.dto.HomeworkRecordResponse;
import academic.academic.domain.homework.entity.HomeworkItem;
import academic.academic.domain.homework.entity.HomeworkRecord;
import academic.academic.domain.homework.repository.HomeworkItemRepository;
import academic.academic.domain.homework.repository.HomeworkRecordRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HomeworkRecordServiceTest {

    @Mock
    private HomeworkItemRepository homeworkItemRepository;
    @Mock
    private HomeworkRecordRepository homeworkRecordRepository;
    @Mock
    private StudentRepository studentRepository;

    private HomeworkRecordService homeworkRecordService;

    private SchoolClass schoolClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        homeworkRecordService = new HomeworkRecordService(homeworkItemRepository, homeworkRecordRepository, studentRepository);

        schoolClass = SchoolClass.builder().name("중2 심화반").build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student1 = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student1, "id", 101L);

        student2 = Student.builder().name("이서연").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student2, "id", 102L);
    }

    private HomeworkItem classScopedItem() {
        HomeworkItem item = HomeworkItem.builder().schoolClass(schoolClass).title("단어장 Ch.5")
                .assignedDate(LocalDate.of(2026, 8, 17)).dueDate(LocalDate.of(2026, 8, 19)).build();
        ReflectionTestUtils.setField(item, "id", 501L);
        return item;
    }

    @Nested
    class GetItemRecords {

        @Test
        void 반_단위_항목은_반_소속_학생_전원에_대한_기록을_반환한다() {
            HomeworkItem item = classScopedItem();
            HomeworkRecord existing = HomeworkRecord.builder().homeworkItem(item).student(student1)
                    .done(true).score(98).comment("오타 1개").build();

            given(homeworkItemRepository.findById(501L)).willReturn(Optional.of(item));
            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1, student2));
            given(homeworkRecordRepository.findByHomeworkItemId(501L)).willReturn(List.of(existing));

            List<HomeworkRecordResponse> responses = homeworkRecordService.getItemRecords(501L);

            assertThat(responses).hasSize(2);
            HomeworkRecordResponse checked = responses.stream().filter(r -> r.studentId().equals(101L)).findFirst().orElseThrow();
            HomeworkRecordResponse unchecked = responses.stream().filter(r -> r.studentId().equals(102L)).findFirst().orElseThrow();
            assertThat(checked.isDone()).isTrue();
            assertThat(checked.score()).isEqualTo(98);
            assertThat(unchecked.id()).isNull();
            assertThat(unchecked.isDone()).isFalse();
        }

        @Test
        void 개별_학생_항목은_해당_학생_기록만_반환한다() {
            HomeworkItem item = HomeworkItem.builder().student(student1).title("독후감")
                    .assignedDate(LocalDate.of(2026, 8, 17)).build();
            ReflectionTestUtils.setField(item, "id", 502L);

            given(homeworkItemRepository.findById(502L)).willReturn(Optional.of(item));
            given(homeworkRecordRepository.findByHomeworkItemId(502L)).willReturn(List.of());

            List<HomeworkRecordResponse> responses = homeworkRecordService.getItemRecords(502L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).studentId()).isEqualTo(101L);
            assertThat(responses.get(0).id()).isNull();
        }

        @Test
        void 항목이_없으면_NOT_FOUND_예외() {
            given(homeworkItemRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> homeworkRecordService.getItemRecords(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class SaveBulk {

        @Test
        void 새로운_기록을_생성한다() {
            HomeworkItem item = classScopedItem();
            HomeworkRecordBulkRequest request = new HomeworkRecordBulkRequest(3L, List.of(
                    new HomeworkItemRecordsGroup(501L, List.of(
                            new HomeworkRecordItem(101L, true, 98, "오타 1개"),
                            new HomeworkRecordItem(102L, false, null, "재제출 요청")
                    ))
            ));

            given(homeworkItemRepository.findById(501L)).willReturn(Optional.of(item));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student1));
            given(studentRepository.findById(102L)).willReturn(Optional.of(student2));
            given(homeworkRecordRepository.findByHomeworkItemIdAndStudentId(501L, 101L)).willReturn(Optional.empty());
            given(homeworkRecordRepository.findByHomeworkItemIdAndStudentId(501L, 102L)).willReturn(Optional.empty());
            given(homeworkRecordRepository.save(any(HomeworkRecord.class))).willAnswer(invocation -> invocation.getArgument(0));

            List<HomeworkRecordResponse> responses = homeworkRecordService.saveBulk(request);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).isDone()).isTrue();
            assertThat(responses.get(0).score()).isEqualTo(98);
            assertThat(responses.get(1).comment()).isEqualTo("재제출 요청");
        }

        @Test
        void 이미_존재하는_기록은_새로_생성하지_않고_갱신한다() {
            HomeworkItem item = classScopedItem();
            HomeworkRecord existing = HomeworkRecord.builder().homeworkItem(item).student(student1)
                    .done(false).score(null).comment(null).build();
            ReflectionTestUtils.setField(existing, "id", 900L);

            HomeworkRecordBulkRequest request = new HomeworkRecordBulkRequest(3L, List.of(
                    new HomeworkItemRecordsGroup(501L, List.of(new HomeworkRecordItem(101L, true, 100, null)))
            ));

            given(homeworkItemRepository.findById(501L)).willReturn(Optional.of(item));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student1));
            given(homeworkRecordRepository.findByHomeworkItemIdAndStudentId(501L, 101L)).willReturn(Optional.of(existing));

            List<HomeworkRecordResponse> responses = homeworkRecordService.saveBulk(request);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).id()).isEqualTo(900L);
            assertThat(responses.get(0).isDone()).isTrue();
            assertThat(responses.get(0).score()).isEqualTo(100);
            verify(homeworkRecordRepository, never()).save(any(HomeworkRecord.class));
        }

        @Test
        void 숙제_항목이_없으면_NOT_FOUND_예외() {
            HomeworkRecordBulkRequest request = new HomeworkRecordBulkRequest(3L, List.of(
                    new HomeworkItemRecordsGroup(999L, List.of(new HomeworkRecordItem(101L, true, null, null)))
            ));
            given(homeworkItemRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> homeworkRecordService.saveBulk(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 요청한_반과_다른_반의_숙제_항목이면_VALIDATION_ERROR_예외() {
            HomeworkItem item = classScopedItem();
            HomeworkRecordBulkRequest request = new HomeworkRecordBulkRequest(4L, List.of(
                    new HomeworkItemRecordsGroup(501L, List.of(new HomeworkRecordItem(101L, true, null, null)))
            ));
            given(homeworkItemRepository.findById(501L)).willReturn(Optional.of(item));

            assertThatThrownBy(() -> homeworkRecordService.saveBulk(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void 반_소속이_아닌_학생이면_VALIDATION_ERROR_예외() {
            SchoolClass otherClass = SchoolClass.builder().name("초등 문법반").build();
            ReflectionTestUtils.setField(otherClass, "id", 4L);
            Student outsider = Student.builder().name("박서준").schoolClass(otherClass).build();
            ReflectionTestUtils.setField(outsider, "id", 200L);

            HomeworkItem item = classScopedItem();
            HomeworkRecordBulkRequest request = new HomeworkRecordBulkRequest(3L, List.of(
                    new HomeworkItemRecordsGroup(501L, List.of(new HomeworkRecordItem(200L, true, null, null)))
            ));

            given(homeworkItemRepository.findById(501L)).willReturn(Optional.of(item));
            given(studentRepository.findById(200L)).willReturn(Optional.of(outsider));

            assertThatThrownBy(() -> homeworkRecordService.saveBulk(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void 개별_학생_항목에_대상이_아닌_학생이면_VALIDATION_ERROR_예외() {
            HomeworkItem item = HomeworkItem.builder().student(student1).title("독후감")
                    .assignedDate(LocalDate.of(2026, 8, 17)).build();
            ReflectionTestUtils.setField(item, "id", 502L);

            HomeworkRecordBulkRequest request = new HomeworkRecordBulkRequest(3L, List.of(
                    new HomeworkItemRecordsGroup(502L, List.of(new HomeworkRecordItem(102L, true, null, null)))
            ));

            given(homeworkItemRepository.findById(502L)).willReturn(Optional.of(item));
            given(studentRepository.findById(102L)).willReturn(Optional.of(student2));

            assertThatThrownBy(() -> homeworkRecordService.saveBulk(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    class GetStudentHomework {

        @Test
        void 기간_내_숙제_기록을_반환한다() {
            HomeworkItem item = classScopedItem();
            HomeworkRecord record = HomeworkRecord.builder().homeworkItem(item).student(student1)
                    .done(true).score(98).comment(null).build();

            given(studentRepository.existsById(101L)).willReturn(true);
            given(homeworkRecordRepository.findByStudentIdAndAssignedDateBetween(
                    101L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                    .willReturn(List.of(record));

            List<HomeworkRecordResponse> responses = homeworkRecordService.getStudentHomework(
                    101L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).score()).isEqualTo(98);
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            given(studentRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> homeworkRecordService.getStudentHomework(
                    999L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }
}
