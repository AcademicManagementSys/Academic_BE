package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MonthlyExamRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MonthlyExamRepository monthlyExamRepository;

    @Test
    void 월말모의고사_회차를_시행연월_내림차순으로_조회한다() {
        entityManager.persist(MonthlyExam.builder().examName("6월 학평").examMonth("2026-06").build());
        entityManager.persist(MonthlyExam.builder().examName("8월 학평").examMonth("2026-08").build());
        entityManager.persist(MonthlyExam.builder().examName("7월 학평").examMonth("2026-07").build());
        entityManager.flush();
        entityManager.clear();

        List<MonthlyExam> result = monthlyExamRepository.findAllByOrderByExamMonthDescIdDesc();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getExamMonth()).isEqualTo("2026-08");
        assertThat(result.get(1).getExamMonth()).isEqualTo("2026-07");
        assertThat(result.get(2).getExamMonth()).isEqualTo("2026-06");
    }
}
