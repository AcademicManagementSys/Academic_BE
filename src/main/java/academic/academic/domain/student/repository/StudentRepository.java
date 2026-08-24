package academic.academic.domain.student.repository;

import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsBySchoolClassId(Long classId);

    List<Student> findBySchoolClassId(Long classId);

    @Query("select s from Student s "
            + "where (:classId is null or s.schoolClass.id = :classId) "
            + "and (:status is null or s.status = :status) "
            + "and (:excludeWithdrawn = false or s.status <> academic.academic.domain.student.entity.StudentStatus.WITHDRAWN) "
            + "and (:keyword is null or s.name like concat('%', :keyword, '%')) "
            + "order by s.name asc")
    List<Student> search(@Param("classId") Long classId,
                          @Param("status") StudentStatus status,
                          @Param("excludeWithdrawn") boolean excludeWithdrawn,
                          @Param("keyword") String keyword);
}
