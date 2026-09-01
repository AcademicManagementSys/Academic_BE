package academic.academic.domain.notice.repository;

import academic.academic.domain.notice.entity.Notice;
import academic.academic.domain.notice.entity.NoticeScope;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NoticeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    void scope와_classId로_공지를_필터링하고_고정공지를_상단에_둔다() {
        User admin = entityManager.persist(User.builder().name("원장").role(Role.ADMIN)
                .loginId("admin1").passwordHash("hash").build());
        SchoolClass classA = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        SchoolClass classB = entityManager.persist(SchoolClass.builder().name("초등 문법반").build());

        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.ALL)
                .title("추석 연휴 휴원 안내").content("...").pinned(false).build());
        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.CLASS).schoolClass(classA)
                .title("A반 특강 안내").content("...").pinned(true).build());
        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.CLASS).schoolClass(classB)
                .title("B반 공지").content("...").pinned(false).build());
        entityManager.flush();
        entityManager.clear();

        List<Notice> classAOnly = noticeRepository.search(NoticeScope.CLASS, classA.getId(), Pageable.unpaged());
        assertThat(classAOnly).hasSize(1);
        assertThat(classAOnly.get(0).getTitle()).isEqualTo("A반 특강 안내");

        List<Notice> all = noticeRepository.search(null, null, Pageable.unpaged());
        assertThat(all).hasSize(3);
        assertThat(all.get(0).isPinned()).isTrue();
    }

    @Test
    void limit이_주어지면_제한된_개수만_반환한다() {
        User admin = entityManager.persist(User.builder().name("원장").role(Role.ADMIN)
                .loginId("admin1").passwordHash("hash").build());
        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.ALL).title("공지1").content("...").pinned(false).build());
        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.ALL).title("공지2").content("...").pinned(false).build());
        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.ALL).title("공지3").content("...").pinned(false).build());
        entityManager.flush();
        entityManager.clear();

        List<Notice> limited = noticeRepository.search(NoticeScope.ALL, null, PageRequest.of(0, 2));

        assertThat(limited).hasSize(2);
    }

    @Test
    void 학생_소속_반의_공지와_전체_공지를_함께_조회한다() {
        User admin = entityManager.persist(User.builder().name("원장").role(Role.ADMIN)
                .loginId("admin1").passwordHash("hash").build());
        SchoolClass classA = entityManager.persist(SchoolClass.builder().name("중2 심화반").build());
        SchoolClass classB = entityManager.persist(SchoolClass.builder().name("초등 문법반").build());

        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.ALL)
                .title("전체 공지").content("...").pinned(false).build());
        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.CLASS).schoolClass(classA)
                .title("A반 공지").content("...").pinned(false).build());
        entityManager.persist(Notice.builder().author(admin).scope(NoticeScope.CLASS).schoolClass(classB)
                .title("B반 공지(무관)").content("...").pinned(false).build());
        entityManager.flush();
        entityManager.clear();

        List<Notice> result = noticeRepository.findRelevantToClass(classA.getId(), Pageable.unpaged());

        assertThat(result).hasSize(2)
                .extracting(Notice::getTitle)
                .containsExactlyInAnyOrder("전체 공지", "A반 공지");
    }
}
