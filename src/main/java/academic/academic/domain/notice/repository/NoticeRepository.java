package academic.academic.domain.notice.repository;

import academic.academic.domain.notice.entity.Notice;
import academic.academic.domain.notice.entity.NoticeScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("select n from Notice n "
            + "where (:scope is null or n.scope = :scope) "
            + "and (:classId is null or n.schoolClass.id = :classId) "
            + "order by n.pinned desc, n.createdAt desc")
    List<Notice> search(@Param("scope") NoticeScope scope, @Param("classId") Long classId, Pageable pageable);

    @Query("select n from Notice n "
            + "where n.scope = academic.academic.domain.notice.entity.NoticeScope.ALL "
            + "or (n.scope = academic.academic.domain.notice.entity.NoticeScope.CLASS and n.schoolClass.id = :classId) "
            + "order by n.pinned desc, n.createdAt desc")
    List<Notice> findRelevantToClass(@Param("classId") Long classId, Pageable pageable);
}
