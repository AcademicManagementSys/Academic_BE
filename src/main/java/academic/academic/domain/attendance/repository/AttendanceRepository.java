package academic.academic.domain.attendance.repository;

import academic.academic.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByStudentIdAndDate(Long studentId, LocalDate date);

    @Query("select a from Attendance a where a.student.schoolClass.id = :classId and a.date = :date")
    List<Attendance> findByClassIdAndDate(@Param("classId") Long classId, @Param("date") LocalDate date);

    List<Attendance> findByStudentIdAndDateBetweenOrderByDateAsc(Long studentId, LocalDate start, LocalDate end);

    @Query("select a from Attendance a where a.student.schoolClass.id = :classId and a.date between :start and :end")
    List<Attendance> findByClassIdAndDateBetween(@Param("classId") Long classId,
                                                  @Param("start") LocalDate start,
                                                  @Param("end") LocalDate end);
}
