package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyExamRepository extends JpaRepository<MonthlyExam, Long> {

    List<MonthlyExam> findAllByOrderByExamMonthDescIdDesc();
}
