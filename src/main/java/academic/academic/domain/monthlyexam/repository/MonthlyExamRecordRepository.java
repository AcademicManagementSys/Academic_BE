package academic.academic.domain.monthlyexam.repository;

import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MonthlyExamRecordRepository extends JpaRepository<MonthlyExamRecord, Long> {

    Optional<MonthlyExamRecord> findByMonthlyExamIdAndStudentId(Long monthlyExamId, Long studentId);

    long countByStudentIdAndCreatedAtAfter(Long studentId, LocalDateTime since);

    List<MonthlyExamRecord> findByMonthlyExamIdAndStudent_SchoolClassId(Long monthlyExamId, Long classId);

    @Query("select r from MonthlyExamRecord r where r.student.id = :studentId "
            + "order by r.monthlyExam.examMonth desc, r.id desc")
    List<MonthlyExamRecord> findRecentByStudentId(@Param("studentId") Long studentId, Pageable pageable);
}
