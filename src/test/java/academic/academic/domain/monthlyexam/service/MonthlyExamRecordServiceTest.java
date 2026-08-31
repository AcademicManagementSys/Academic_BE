package academic.academic.domain.monthlyexam.service;

import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordCreateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordUpdateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamTrendResponse;
import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRecordRepository;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MonthlyExamRecordServiceTest {

    @Mock
    private MonthlyExamRepository monthlyExamRepository;
    @Mock
    private MonthlyExamRecordRepository monthlyExamRecordRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private StudentRepository studentRepository;

    private MonthlyExamRecordService monthlyExamRecordService;

    private MonthlyExam exam;
    private SchoolClass schoolClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        monthlyExamRecordService = new MonthlyExamRecordService(
                monthlyExamRepository, monthlyExamRecordRepository, schoolClassRepository, studentRepository);

        exam = MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build();
        ReflectionTestUtils.setField(exam, "id", 12L);

        schoolClass = SchoolClass.builder().name("중2 심화반").build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student1 = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student1, "id", 101L);

        student2 = Student.builder().name("이서연").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student2, "id", 102L);
    }

    @Nested
    class Create {

        @Test
        void 성적을_등록한다() {
            MonthlyExamRecordCreateRequest request = new MonthlyExamRecordCreateRequest(12L, 101L, 82, 128, 91, "2등급");
            given(monthlyExamRepository.findById(12L)).willReturn(Optional.of(exam));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student1));
            given(monthlyExamRecordRepository.findByMonthlyExamIdAndStudentId(12L, 101L)).willReturn(Optional.empty());
            given(monthlyExamRecordRepository.save(any(MonthlyExamRecord.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            MonthlyExamRecordResponse response = monthlyExamRecordService.create(request);

            assertThat(response.rawScore()).isEqualTo(82);
            assertThat(response.studentName()).isEqualTo("김민준");
        }

        @Test
        void 이미_등록된_성적이면_VALIDATION_ERROR_예외() {
            MonthlyExamRecordCreateRequest request = new MonthlyExamRecordCreateRequest(12L, 101L, 82, null, null, null);
            MonthlyExamRecord existing = MonthlyExamRecord.builder().monthlyExam(exam).student(student1).rawScore(70).build();

            given(monthlyExamRepository.findById(12L)).willReturn(Optional.of(exam));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student1));
            given(monthlyExamRecordRepository.findByMonthlyExamIdAndStudentId(12L, 101L)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> monthlyExamRecordService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void 회차가_없으면_NOT_FOUND_예외() {
            MonthlyExamRecordCreateRequest request = new MonthlyExamRecordCreateRequest(999L, 101L, 82, null, null, null);
            given(monthlyExamRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> monthlyExamRecordService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            MonthlyExamRecordCreateRequest request = new MonthlyExamRecordCreateRequest(12L, 999L, 82, null, null, null);
            given(monthlyExamRepository.findById(12L)).willReturn(Optional.of(exam));
            given(studentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> monthlyExamRecordService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class Update {

        @Test
        void 성적을_수정한다() {
            MonthlyExamRecord record = MonthlyExamRecord.builder().monthlyExam(exam).student(student1).rawScore(70).build();
            ReflectionTestUtils.setField(record, "id", 5000L);
            MonthlyExamRecordUpdateRequest request = new MonthlyExamRecordUpdateRequest(85, 130, 93, "1등급");
            given(monthlyExamRecordRepository.findById(5000L)).willReturn(Optional.of(record));

            MonthlyExamRecordResponse response = monthlyExamRecordService.update(5000L, request);

            assertThat(response.rawScore()).isEqualTo(85);
            assertThat(response.grade()).isEqualTo("1등급");
        }

        @Test
        void 성적이_없으면_NOT_FOUND_예외() {
            given(monthlyExamRecordRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> monthlyExamRecordService.update(999L, new MonthlyExamRecordUpdateRequest(85, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class GetByClass {

        @Test
        void 반_소속_학생_전원에_대해_성적_또는_미등록_상태를_반환한다() {
            MonthlyExamRecord existing = MonthlyExamRecord.builder().monthlyExam(exam).student(student1).rawScore(82).build();

            given(monthlyExamRepository.findById(12L)).willReturn(Optional.of(exam));
            given(schoolClassRepository.existsById(3L)).willReturn(true);
            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1, student2));
            given(monthlyExamRecordRepository.findByMonthlyExamIdAndStudent_SchoolClassId(12L, 3L))
                    .willReturn(List.of(existing));

            List<MonthlyExamRecordResponse> responses = monthlyExamRecordService.getByClass(12L, 3L);

            assertThat(responses).hasSize(2);
            MonthlyExamRecordResponse recorded = responses.stream()
                    .filter(r -> r.studentId().equals(101L)).findFirst().orElseThrow();
            assertThat(recorded.rawScore()).isEqualTo(82);

            MonthlyExamRecordResponse unrecorded = responses.stream()
                    .filter(r -> r.studentId().equals(102L)).findFirst().orElseThrow();
            assertThat(unrecorded.id()).isNull();
            assertThat(unrecorded.rawScore()).isNull();
        }

        @Test
        void 반이_없으면_NOT_FOUND_예외() {
            given(monthlyExamRepository.findById(12L)).willReturn(Optional.of(exam));
            given(schoolClassRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> monthlyExamRecordService.getByClass(12L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class GetStudentTrend {

        @Test
        void 최근_성적을_시행연월_오름차순으로_반환한다() {
            MonthlyExam exam1 = MonthlyExam.builder().examName("6월 학평").examMonth("2026-06").build();
            ReflectionTestUtils.setField(exam1, "id", 10L);
            MonthlyExam exam2 = MonthlyExam.builder().examName("7월 학평").examMonth("2026-07").build();
            ReflectionTestUtils.setField(exam2, "id", 11L);

            MonthlyExamRecord r1 = MonthlyExamRecord.builder().monthlyExam(exam1).student(student1).rawScore(72).build();
            MonthlyExamRecord r2 = MonthlyExamRecord.builder().monthlyExam(exam2).student(student1).rawScore(78).build();

            given(studentRepository.existsById(101L)).willReturn(true);
            given(monthlyExamRecordRepository.findRecentByStudentId(101L, PageRequest.of(0, 5)))
                    .willReturn(List.of(r2, r1));

            List<MonthlyExamTrendResponse> result = monthlyExamRecordService.getStudentTrend(101L, 5);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).examMonth()).isEqualTo("2026-06");
            assertThat(result.get(1).examMonth()).isEqualTo("2026-07");
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            given(studentRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> monthlyExamRecordService.getStudentTrend(999L, 5))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }
}
