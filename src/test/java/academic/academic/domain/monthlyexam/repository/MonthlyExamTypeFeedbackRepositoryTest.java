package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.FeedbackStatus;
import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.monthlyexam.entity.MonthlyExamTypeFeedback;
import academic.academic.domain.monthlyexam.entity.TypeCategory;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MonthlyExamTypeFeedbackRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MonthlyExamTypeFeedbackRepository monthlyExamTypeFeedbackRepository;

    @Test
    void 성적에_속한_유형별_피드백_목록을_조회한다() {
        MonthlyExam exam = entityManager.persist(MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build());
        SchoolClass schoolClass = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        Student student = entityManager.persist(Student.builder().name("김민준").schoolClass(schoolClass).build());
        MonthlyExamRecord record = entityManager.persist(MonthlyExamRecord.builder()
                .monthlyExam(exam).student(student).rawScore(82).build());
        TypeCategory vocab = entityManager.persist(TypeCategory.builder().name("어휘").build());
        TypeCategory blank = entityManager.persist(TypeCategory.builder().name("빈칸추론").build());
        entityManager.persist(MonthlyExamTypeFeedback.builder().monthlyExamRecord(record).typeCategory(vocab)
                .status(FeedbackStatus.STRENGTH).feedbackText("어휘 문제 정답률 95%로 우수합니다.").build());
        entityManager.persist(MonthlyExamTypeFeedback.builder().monthlyExamRecord(record).typeCategory(blank)
                .status(FeedbackStatus.NEEDS_WORK).feedbackText("반복적으로 오답이 발생해 추가 연습이 필요합니다.").build());
        entityManager.flush();
        entityManager.clear();

        List<MonthlyExamTypeFeedback> result = monthlyExamTypeFeedbackRepository
                .findByMonthlyExamRecordId(record.getId());

        assertThat(result).hasSize(2)
                .extracting(f -> f.getTypeCategory().getName())
                .containsExactlyInAnyOrder("어휘", "빈칸추론");
    }
}
