package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.monthlyexam.entity.MonthlyExamScoreFeedback;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class MonthlyExamScoreFeedbackRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MonthlyExamScoreFeedbackRepository monthlyExamScoreFeedbackRepository;

    @Test
    void 성적으로_점수대별_피드백을_조회한다() {
        MonthlyExam exam = entityManager.persist(MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build());
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        MonthlyExamRecord record = entityManager.persist(MonthlyExamRecord.builder()
                .monthlyExam(exam).student(student).rawScore(82).build());
        entityManager.persist(MonthlyExamScoreFeedback.builder().monthlyExamRecord(record)
                .scoreBand("80점대").feedbackText("기본 문법과 어휘는 탄탄하나...").build());
        entityManager.flush();
        entityManager.clear();

        Optional<MonthlyExamScoreFeedback> found = monthlyExamScoreFeedbackRepository
                .findByMonthlyExamRecordId(record.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getScoreBand()).isEqualTo("80점대");
    }

    @Test
    void 하나의_성적에는_점수대별_피드백을_하나만_등록할_수_있다() {
        MonthlyExam exam = entityManager.persist(MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build());
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        MonthlyExamRecord record = entityManager.persist(MonthlyExamRecord.builder()
                .monthlyExam(exam).student(student).rawScore(82).build());
        entityManager.persist(MonthlyExamScoreFeedback.builder().monthlyExamRecord(record)
                .scoreBand("80점대").feedbackText("기본 문법과 어휘는 탄탄하나...").build());
        entityManager.flush();

        MonthlyExamScoreFeedback duplicate = MonthlyExamScoreFeedback.builder().monthlyExamRecord(record)
                .scoreBand("90점대").feedbackText("중복").build();

        assertThatThrownBy(() -> monthlyExamScoreFeedbackRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
