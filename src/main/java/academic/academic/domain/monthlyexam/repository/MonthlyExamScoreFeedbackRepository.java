package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.MonthlyExamScoreFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyExamScoreFeedbackRepository extends JpaRepository<MonthlyExamScoreFeedback, Long> {

    Optional<MonthlyExamScoreFeedback> findByMonthlyExamRecordId(Long monthlyExamRecordId);
}
