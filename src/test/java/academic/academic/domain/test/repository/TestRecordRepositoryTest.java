package academic.academic.domain.test.repository;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.test.entity.TestRecord;
import academic.academic.domain.test.entity.TestSession;
import academic.academic.domain.test.entity.TestSubject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TestRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestRecordRepository testRecordRepository;

    @Test
    void 회차_학생_영역으로_기록을_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        TestSession session = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("8월 3주차 테스트")
                .testDate(LocalDate.of(2026, 8, 19)).build());
        entityManager.persist(TestRecord.builder().testSession(session).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(18).maxScore(20).comment("오타 1개").build());
        entityManager.flush();
        entityManager.clear();

        Optional<TestRecord> found = testRecordRepository
                .findByTestSessionIdAndStudentIdAndSubject(session.getId(), student.getId(), TestSubject.VOCAB);

        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isEqualTo(18);
    }

    @Test
    void 같은_회차_같은_학생_같은_영역의_기록은_중복_저장할_수_없다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        TestSession session = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("8월 3주차 테스트")
                .testDate(LocalDate.of(2026, 8, 19)).build());
        entityManager.persist(TestRecord.builder().testSession(session).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(18).build());
        entityManager.flush();

        TestRecord duplicate = TestRecord.builder().testSession(session).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(20).build();

        assertThatThrownBy(() -> testRecordRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 회차별_전체_기록_목록을_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student1 = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        Student student2 = entityManager.persist(Student.builder().name("이서연").schoolClass(schoolClass).build());
        TestSession session = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("8월 3주차 테스트")
                .testDate(LocalDate.of(2026, 8, 19)).build());
        entityManager.persist(TestRecord.builder().testSession(session).student(student1)
                .subject(TestSubject.VOCAB).taken(true).score(18).build());
        entityManager.persist(TestRecord.builder().testSession(session).student(student2)
                .subject(TestSubject.READING).taken(true).score(16).build());
        entityManager.flush();
        entityManager.clear();

        List<TestRecord> result = testRecordRepository.findByTestSessionId(session.getId());

        assertThat(result).hasSize(2)
                .extracting(r -> r.getStudent().getId())
                .containsExactlyInAnyOrder(student1.getId(), student2.getId());
    }

    @Test
    void 학생의_지정된_회차들에_대한_기록을_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        TestSession session1 = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("1회차")
                .testDate(LocalDate.of(2026, 7, 1)).build());
        TestSession session2 = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("2회차")
                .testDate(LocalDate.of(2026, 8, 1)).build());
        TestSession outOfRange = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("제외 회차")
                .testDate(LocalDate.of(2026, 6, 1)).build());
        entityManager.persist(TestRecord.builder().testSession(session1).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(18).build());
        entityManager.persist(TestRecord.builder().testSession(session2).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(19).build());
        entityManager.persist(TestRecord.builder().testSession(outOfRange).student(student)
                .subject(TestSubject.VOCAB).taken(true).score(10).build());
        entityManager.flush();
        entityManager.clear();

        List<TestRecord> result = testRecordRepository.findByStudentIdAndTestSessionIdIn(
                student.getId(), List.of(session1.getId(), session2.getId()));

        assertThat(result).hasSize(2)
                .extracting(r -> r.getTestSession().getTitle())
                .containsExactlyInAnyOrder("1회차", "2회차");
    }

    @Test
    void 지정된_회차들에_기록이_있는_학생_id를_중복없이_조회한다() {
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student1 = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        Student student2 = entityManager.persist(Student.builder().name("이서연").schoolClass(schoolClass).build());
        TestSession session1 = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("단어")
                .testDate(LocalDate.of(2026, 8, 19)).build());
        TestSession session2 = entityManager.persist(TestSession.builder().schoolClass(schoolClass).title("독해")
                .testDate(LocalDate.of(2026, 8, 19)).build());
        entityManager.persist(TestRecord.builder().testSession(session1).student(student1)
                .subject(TestSubject.VOCAB).taken(true).score(18).build());
        entityManager.persist(TestRecord.builder().testSession(session2).student(student1)
                .subject(TestSubject.READING).taken(true).score(15).build());
        entityManager.flush();
        entityManager.clear();

        List<Long> result = testRecordRepository.findDistinctStudentIdsByTestSessionIdIn(
                List.of(session1.getId(), session2.getId()));

        assertThat(result).containsExactly(student1.getId());
        assertThat(result).doesNotContain(student2.getId());
    }
}
