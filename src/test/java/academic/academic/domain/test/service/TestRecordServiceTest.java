package academic.academic.domain.test.service;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.test.dto.TestRecordBulkRequest;
import academic.academic.domain.test.dto.TestRecordItem;
import academic.academic.domain.test.dto.TestRecordResponse;
import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.entity.TestSubject;
import academic.academic.domain.test.repository.TestRecordRepository;
import academic.academic.domain.test.repository.TestSessionRepository;
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
class TestRecordServiceTest {

    @Mock
    private TestSessionRepository testSessionRepository;
    @Mock
    private TestRecordRepository testRecordRepository;
    @Mock
    private StudentRepository studentRepository;

    private TestRecordService testRecordService;

    private SchoolClass schoolClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        testRecordService = new TestRecordService(testSessionRepository, testRecordRepository, studentRepository);

        schoolClass = SchoolClass.builder().name("중2 심화반").build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);

        student1 = Student.builder().name("김민준").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student1, "id", 101L);

        student2 = Student.builder().name("이서연").schoolClass(schoolClass).build();
        ReflectionTestUtils.setField(student2, "id", 102L);
    }

    private TestSession session() {
        TestSession session = TestSession.builder().schoolClass(schoolClass).title("8월 3주차 테스트")
                .testDate(LocalDate.of(2026, 8, 19)).build();
        ReflectionTestUtils.setField(session, "id", 901L);
        return session;
    }

    @Nested
    class GetSessionRecords {

        @Test
        void 반_소속_학생_전원에_대해_4개_영역_매트릭스를_반환한다() {
            TestSession session = session();
            TestRecord existing = TestRecord.builder().testSession(session).student(student1)
                    .subject(TestSubject.VOCAB).taken(true).score(18).maxScore(20).comment("오타 1개").build();

            given(testSessionRepository.findById(901L)).willReturn(Optional.of(session));
            given(studentRepository.findBySchoolClassId(3L)).willReturn(List.of(student1, student2));
            given(testRecordRepository.findByTestSessionId(901L)).willReturn(List.of(existing));

            List<TestRecordResponse> responses = testRecordService.getSessionRecords(901L);

            assertThat(responses).hasSize(8);
            TestRecordResponse checked = responses.stream()
                    .filter(r -> r.studentId().equals(101L) && r.subject() == TestSubject.VOCAB)
                    .findFirst().orElseThrow();
            assertThat(checked.isTaken()).isTrue();
            assertThat(checked.score()).isEqualTo(18);

            TestRecordResponse unchecked = responses.stream()
                    .filter(r -> r.studentId().equals(101L) && r.subject() == TestSubject.READING)
                    .findFirst().orElseThrow();
            assertThat(unchecked.id()).isNull();
            assertThat(unchecked.isTaken()).isFalse();

            long student2Count = responses.stream().filter(r -> r.studentId().equals(102L)).count();
            assertThat(student2Count).isEqualTo(4);
        }

        @Test
        void 회차가_없으면_NOT_FOUND_예외() {
            given(testSessionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> testRecordService.getSessionRecords(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class SaveBulk {

        @Test
        void 새로운_기록을_생성한다() {
            TestSession session = session();
            TestRecordBulkRequest request = new TestRecordBulkRequest(901L, List.of(
                    new TestRecordItem(101L, TestSubject.VOCAB, true, 18, 20, "오타 1개"),
                    new TestRecordItem(101L, TestSubject.READING, true, 16, 20, null)
            ));

            given(testSessionRepository.findById(901L)).willReturn(Optional.of(session));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student1));
            given(testRecordRepository.findByTestSessionIdAndStudentIdAndSubject(901L, 101L, TestSubject.VOCAB))
                    .willReturn(Optional.empty());
            given(testRecordRepository.findByTestSessionIdAndStudentIdAndSubject(901L, 101L, TestSubject.READING))
                    .willReturn(Optional.empty());
            given(testRecordRepository.save(any(TestRecord.class))).willAnswer(invocation -> invocation.getArgument(0));

            List<TestRecordResponse> responses = testRecordService.saveBulk(request);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).score()).isEqualTo(18);
            assertThat(responses.get(1).subject()).isEqualTo(TestSubject.READING);
        }

        @Test
        void 이미_존재하는_기록은_새로_생성하지_않고_갱신한다() {
            TestSession session = session();
            TestRecord existing = TestRecord.builder().testSession(session).student(student1)
                    .subject(TestSubject.VOCAB).taken(false).build();
            ReflectionTestUtils.setField(existing, "id", 5000L);

            TestRecordBulkRequest request = new TestRecordBulkRequest(901L, List.of(
                    new TestRecordItem(101L, TestSubject.VOCAB, true, 20, 20, null)
            ));

            given(testSessionRepository.findById(901L)).willReturn(Optional.of(session));
            given(studentRepository.findById(101L)).willReturn(Optional.of(student1));
            given(testRecordRepository.findByTestSessionIdAndStudentIdAndSubject(901L, 101L, TestSubject.VOCAB))
                    .willReturn(Optional.of(existing));

            List<TestRecordResponse> responses = testRecordService.saveBulk(request);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).id()).isEqualTo(5000L);
            assertThat(responses.get(0).isTaken()).isTrue();
            assertThat(responses.get(0).score()).isEqualTo(20);
            verify(testRecordRepository, never()).save(any(TestRecord.class));
        }

        @Test
        void 회차가_없으면_NOT_FOUND_예외() {
            TestRecordBulkRequest request = new TestRecordBulkRequest(999L, List.of(
                    new TestRecordItem(101L, TestSubject.VOCAB, true, 18, 20, null)
            ));
            given(testSessionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> testRecordService.saveBulk(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            TestSession session = session();
            TestRecordBulkRequest request = new TestRecordBulkRequest(901L, List.of(
                    new TestRecordItem(999L, TestSubject.VOCAB, true, 18, 20, null)
            ));
            given(testSessionRepository.findById(901L)).willReturn(Optional.of(session));
            given(studentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> testRecordService.saveBulk(request))
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

            TestSession session = session();
            TestRecordBulkRequest request = new TestRecordBulkRequest(901L, List.of(
                    new TestRecordItem(200L, TestSubject.VOCAB, true, 18, 20, null)
            ));

            given(testSessionRepository.findById(901L)).willReturn(Optional.of(session));
            given(studentRepository.findById(200L)).willReturn(Optional.of(outsider));

            assertThatThrownBy(() -> testRecordService.saveBulk(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    class GetStudentTests {

        @Test
        void 최근_회차의_기록을_회차_최신순_영역순으로_반환한다() {
            TestSession session1 = TestSession.builder().schoolClass(schoolClass).title("1회차")
                    .testDate(LocalDate.of(2026, 7, 1)).build();
            ReflectionTestUtils.setField(session1, "id", 801L);
            TestSession session2 = TestSession.builder().schoolClass(schoolClass).title("2회차")
                    .testDate(LocalDate.of(2026, 8, 1)).build();
            ReflectionTestUtils.setField(session2, "id", 802L);

            TestRecord r1 = TestRecord.builder().testSession(session1).student(student1)
                    .subject(TestSubject.VOCAB).taken(true).score(18).build();
            TestRecord r2Reading = TestRecord.builder().testSession(session2).student(student1)
                    .subject(TestSubject.READING).taken(true).score(16).build();
            TestRecord r2Vocab = TestRecord.builder().testSession(session2).student(student1)
                    .subject(TestSubject.VOCAB).taken(true).score(19).build();

            given(studentRepository.existsById(101L)).willReturn(true);
            given(testSessionRepository.findRecentSessionsByStudentId(101L, PageRequest.of(0, 10)))
                    .willReturn(List.of(session2, session1));
            given(testRecordRepository.findByStudentIdAndTestSessionIdIn(101L, List.of(802L, 801L)))
                    .willReturn(List.of(r1, r2Reading, r2Vocab));

            List<TestRecordResponse> responses = testRecordService.getStudentTests(101L, 10);

            assertThat(responses).hasSize(3);
            assertThat(responses.get(0).testSessionId()).isEqualTo(802L);
            assertThat(responses.get(0).subject()).isEqualTo(TestSubject.VOCAB);
            assertThat(responses.get(1).subject()).isEqualTo(TestSubject.READING);
            assertThat(responses.get(2).testSessionId()).isEqualTo(801L);
        }

        @Test
        void 응시한_회차가_없으면_빈_목록을_반환한다() {
            given(studentRepository.existsById(101L)).willReturn(true);
            given(testSessionRepository.findRecentSessionsByStudentId(101L, PageRequest.of(0, 10)))
                    .willReturn(List.of());

            List<TestRecordResponse> responses = testRecordService.getStudentTests(101L, 10);

            assertThat(responses).isEmpty();
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            given(studentRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> testRecordService.getStudentTests(999L, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }
}
