package academic.academic.domain.homework.repository;

import academic.academic.domain.homework.entity.HomeworkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HomeworkItemRepository extends JpaRepository<HomeworkItem, Long> {

    @Query("select hi from HomeworkItem hi where hi.schoolClass.id = :classId "
            + "and (cast(:weekStart as date) is null or hi.assignedDate between :weekStart and :weekEnd) "
            + "order by hi.assignedDate desc, hi.id desc")
    List<HomeworkItem> search(@Param("classId") Long classId,
                               @Param("weekStart") LocalDate weekStart,
                               @Param("weekEnd") LocalDate weekEnd);
}
