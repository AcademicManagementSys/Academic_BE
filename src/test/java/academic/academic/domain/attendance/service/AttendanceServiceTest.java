package academic.academic.domain.attendance.service;

import academic.academic.domain.attendance.dto.AttendanceBulkRequest;
import academic.academic.domain.attendance.dto.AttendanceRecordItem;
import academic.academic.domain.attendance.dto.AttendanceResponse;
import academic.academic.domain.attendance.dto.AttendanceUpdateRequest;
import academic.academic.domain.attendance.entity.Attendance;
import academic.academic.domain.attendance.entity.AttendanceStatus;
import academic.academic.domain.attendance.repository.AttendanceRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;

    private AttendanceService attendanceService;

    private SchoolClass schoolClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(attendanceRepository, studentRepository, schoolClassRepository);

        schoolClass = SchoolClass.builder().name("중2 심화반").schedule("월수금 16:00").build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student1 = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student1, "id", 101L);

        student2 = Student.builder().name("이서연").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student2, "id", 102L);
    }

    @Nested
    class SaveBulk {

        @Test
        void 새로운_출석_기록을_생성한다() {
            AttendanceBulkRequest request = new AttendanceBulkRequest(3L, LocalDate.of(2026, 8, 19), List.of(
                    new AttendanceRecordItem(101L, AttendanceStatus.PRESENT, null),
                    new AttendanceRecordItem(102L, AttendanceStatus.LATE, "15분 지각")
            ));

            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student1));
            given(studentRepository.findById(102L)).willReturn(Optional.of(student2));
            given(attendanceRepository.findByStudentIdAndDate(101L, request.date())).willReturn(Optional.empty());
            given(attendanceRepository.findByStudentIdAndDate(102L, request.date())).willReturn(Optional.empty());
            given(attendanceRepository.save(any(Attendance.class))).willAnswer(invocation -> invocation.getArgument(0));

            List<AttendanceResponse> responses = attendanceService.saveBulk(request);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
            assertThat(responses.get(1).note()).isEqualTo("15분 지각");
            verify(attendanceRepository, times(2)).save(any(Attendance.class));
        }

        @Test
        void 이미_존재하는_기록은_새로_생성하지_않고_갱신한다() {
            LocalDate date = LocalDate.of(2026, 8, 19);
            Attendance existing = Attendance.builder().student(student1).date(date)
                    .status(AttendanceStatus.ABSENT).note("결석").build();
            ReflectionTestUtils.setField(existing, "id", 500L);

            AttendanceBulkRequest request = new AttendanceBulkRequest(3L, date, List.of(
                    new AttendanceRecordItem(101L, AttendanceStatus.PRESENT, null)
            ));

            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student1));
            given(attendanceRepository.findByStudentIdAndDate(101L, date)).willReturn(Optional.of(existing));

            List<AttendanceResponse> responses = attendanceService.saveBulk(request);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).id()).isEqualTo(500L);
            assertThat(responses.get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
            verify(attendanceRepository, never()).save(any(Attendance.class));
        }

        @Test
        void 반이_존재하지_않으면_NOT_FOUND_예외() {
            AttendanceBulkRequest request = new AttendanceBulkRequest(999L, LocalDate.now(), List.of(
                    new AttendanceRecordItem(101L, AttendanceStatus.PRESENT, null)
            ));
            given(schoolClassRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.saveBulk(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 반_소속이_아닌_학생이면_VALIDATION_ERROR_예외() {
            SchoolClass otherClass = SchoolClass.builder().name("초등 문법반").build();
            ReflectionTestUtils.setField(otherClass, "id", 4L);
            Student outsider = Student.builder().name("박서준").schoolClass(otherClass).build();
            ReflectionTestUtils.setField(outsider, "id", 200L);

            AttendanceBulkRequest request = new AttendanceBulkRequest(3L, LocalDate.now(), List.of(
                    new AttendanceRecordItem(200L, AttendanceStatus.PRESENT, null)
            ));

            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(studentRepository.findById(200L)).willReturn(Optional.of(outsider));

            assertThatThrownBy(() -> attendanceService.saveBulk(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    class GetAttendanceByClassAndDate {

        @Test
        void 출석_미체크_학생은_status가_null인_응답으로_반환된다() {
            LocalDate date = LocalDate.of(2026, 8, 19);
            Attendance checked = Attendance.builder().student(student1).date(date)
                    .status(AttendanceStatus.PRESENT).build();

            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1, student2));
            given(attendanceRepository.findByClassIdAndDate(3L, date)).willReturn(List.of(checked));

            List<AttendanceResponse> responses = attendanceService.getAttendanceByClassAndDate(3L, "2026-08-19");

            assertThat(responses).hasSize(2);
            AttendanceResponse first = responses.stream().filter(r -> r.studentId().equals(101L)).findFirst().orElseThrow();
            AttendanceResponse second = responses.stream().filter(r -> r.studentId().equals(102L)).findFirst().orElseThrow();
            assertThat(first.status()).isEqualTo(AttendanceStatus.PRESENT);
            assertThat(second.status()).isNull();
            assertThat(second.id()).isNull();
        }

        @Test
        void 반이_존재하지_않으면_NOT_FOUND_예외() {
            given(schoolClassRepository.findById(3L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.getAttendanceByClassAndDate(3L, "2026-08-19"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 날짜_형식이_올바르지_않으면_VALIDATION_ERROR_예외() {
            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));

            assertThatThrownBy(() -> attendanceService.getAttendanceByClassAndDate(3L, "2026/08/19"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    class UpdateAttendance {

        @Test
        void 상태와_비고를_수정한다() {
            Attendance attendance = Attendance.builder().student(student1).date(LocalDate.of(2026, 8, 19))
                    .status(AttendanceStatus.PRESENT).build();
            ReflectionTestUtils.setField(attendance, "id", 500L);
            given(attendanceRepository.findById(500L)).willReturn(Optional.of(attendance));

            AttendanceResponse response = attendanceService.updateAttendance(500L,
                    new AttendanceUpdateRequest(AttendanceStatus.LATE, "15분 지각"));

            assertThat(response.status()).isEqualTo(AttendanceStatus.LATE);
            assertThat(response.note()).isEqualTo("15분 지각");
        }

        @Test
        void 기록이_없으면_NOT_FOUND_예외() {
            given(attendanceRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.updateAttendance(999L,
                    new AttendanceUpdateRequest(AttendanceStatus.LATE, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class GetStudentAttendance {

        @Test
        void 월별_범위의_출석_기록을_반환한다() {
            Attendance a1 = Attendance.builder().student(student1).date(LocalDate.of(2026, 8, 3))
                    .status(AttendanceStatus.PRESENT).build();
            Attendance a2 = Attendance.builder().student(student1).date(LocalDate.of(2026, 8, 10))
                    .status(AttendanceStatus.ABSENT).build();

            given(studentRepository.existsById(101L)).willReturn(true);
            given(attendanceRepository.findByStudentIdAndDateBetweenOrderByDateAsc(
                    101L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                    .willReturn(List.of(a1, a2));

            List<AttendanceResponse> responses = attendanceService.getStudentAttendance(101L, "2026-08");

            assertThat(responses).hasSize(2);
            assertThat(responses.get(1).status()).isEqualTo(AttendanceStatus.ABSENT);
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            given(studentRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> attendanceService.getStudentAttendance(999L, "2026-08"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 월_형식이_올바르지_않으면_VALIDATION_ERROR_예외() {
            given(studentRepository.existsById(101L)).willReturn(true);

            assertThatThrownBy(() -> attendanceService.getStudentAttendance(101L, "2026-13"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }
}
