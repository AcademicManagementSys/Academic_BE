package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.MonthlyExamTypeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyExamTypeFeedbackRepository extends JpaRepository<MonthlyExamTypeFeedback, Long> {

    List<MonthlyExamTypeFeedback> findByMonthlyExamRecordId(Long monthlyExamRecordId);
}
