package academic.academic.domain.homework.repository;

import academic.academic.domain.homework.entity.HomeworkRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HomeworkRecordRepository extends JpaRepository<HomeworkRecord, Long> {

    Optional<HomeworkRecord> findByHomeworkItemIdAndStudentId(Long homeworkItemId, Long studentId);

    long countByStudentIdAndCreatedAtAfter(Long studentId, LocalDateTime since);

    @Query("select r from HomeworkRecord r where r.student.id = :studentId "
            + "order by r.homeworkItem.assignedDate desc, r.id desc")
    List<HomeworkRecord> findRecentByStudentId(@Param("studentId") Long studentId, Pageable pageable);

    List<HomeworkRecord> findByHomeworkItemId(Long homeworkItemId);

    @Query("select r from HomeworkRecord r "
            + "where r.student.id = :studentId "
            + "and r.homeworkItem.assignedDate between :from and :to "
            + "order by r.homeworkItem.assignedDate desc, r.id desc")
    List<HomeworkRecord> findByStudentIdAndAssignedDateBetween(@Param("studentId") Long studentId,
                                                                @Param("from") LocalDate from,
                                                                @Param("to") LocalDate to);

    void deleteByHomeworkItemId(Long homeworkItemId);

    @Query("select r from HomeworkRecord r "
            + "where r.homeworkItem.schoolClass.id = :classId "
            + "and r.homeworkItem.assignedDate between :start and :end")
    List<HomeworkRecord> findByClassIdAndAssignedDateBetween(@Param("classId") Long classId,
                                                              @Param("start") LocalDate start,
                                                              @Param("end") LocalDate end);
}
