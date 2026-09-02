package academic.academic.domain.notice.service;

import academic.academic.domain.notice.dto.NoticeCreateRequest;
import academic.academic.domain.notice.dto.NoticePinUpdateRequest;
import academic.academic.domain.notice.dto.NoticeResponse;
import academic.academic.domain.notice.dto.NoticeUpdateRequest;
import academic.academic.domain.notice.entity.Notice;
import academic.academic.domain.notice.entity.NoticeScope;
import academic.academic.domain.notice.repository.NoticeRepository;
import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private StudentRepository studentRepository;

    private NoticeService noticeService;

    private User admin;
    private User teacher;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        noticeService = new NoticeService(noticeRepository, userRepository, schoolClassRepository, studentRepository);

        admin = User.builder().name("원장").role(Role.ADMIN).loginId("admin1").passwordHash("hash").build();
        ReflectionTestUtils.setField(admin, "id", 1L);

        teacher = User.builder().name("김선생").role(Role.TEACHER).loginId("teacher1").passwordHash("hash").build();
        ReflectionTestUtils.setField(teacher, "id", 2L);

        schoolClass = SchoolClass.builder().name("중2 심화반").teacher(teacher).build();
        ReflectionTestUtils.setField(schoolClass, "id", 3L);
    }

    @Nested
    class Create {

        @Test
        void 관리자는_전체_공지를_작성할_수_있다() {
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));
            given(noticeRepository.save(any(Notice.class))).willAnswer(invocation -> invocation.getArgument(0));

            NoticeResponse response = noticeService.create(new NoticeCreateRequest(
                    NoticeScope.ALL, null, "추석 연휴 휴원 안내", "내용", true), 1L);

            assertThat(response.authorId()).isEqualTo(1L);
            assertThat(response.scope()).isEqualTo(NoticeScope.ALL);
            assertThat(response.isPinned()).isTrue();
        }

        @Test
        void 선생님은_담당_반_공지를_작성할_수_있다() {
            given(userRepository.findById(2L)).willReturn(Optional.of(teacher));
            given(schoolClassRepository.findById(3L)).willReturn(Optional.of(schoolClass));
            given(noticeRepository.save(any(Notice.class))).willAnswer(invocation -> invocation.getArgument(0));

            NoticeResponse response = noticeService.create(new NoticeCreateRequest(
                    NoticeScope.CLASS, 3L, "특강 안내", "내용", null), 2L);

            assertThat(response.classId()).isEqualTo(3L);
            assertThat(response.isPinned()).isFalse();
        }

        @Test
        void 작성자가_없으면_NOT_FOUND_예외() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> noticeService.create(
                    new NoticeCreateRequest(NoticeScope.ALL, null, "제목", "내용", null), 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 학부모_학생은_공지를_작성할_수_없다() {
            User parent = User.builder().name("학부모").role(Role.PARENT).loginId("parent1").passwordHash("hash").build();
            ReflectionTestUtils.setField(parent, "id", 4L);
            given(userRepository.findById(4L)).willReturn(Optional.of(parent));

            assertThatThrownBy(() -> noticeService.create(
                    new NoticeCreateRequest(NoticeScope.ALL, null, "제목", "내용", null), 4L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void 선생님은_전체_공지를_작성할_수_없다() {
            given(userRepository.findById(2L)).willReturn(Optional.of(teacher));

            assertThatThrownBy(() -> noticeService.create(
                    new NoticeCreateRequest(NoticeScope.ALL, null, "제목", "내용", null), 2L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void 반별_공지인데_classId가_없으면_VALIDATION_ERROR_예외() {
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));

            assertThatThrownBy(() -> noticeService.create(
                    new NoticeCreateRequest(NoticeScope.CLASS, null, "제목", "내용", null), 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void 반이_없으면_NOT_FOUND_예외() {
            given(userRepository.findById(1L)).willReturn(Optional.of(admin));
            given(schoolClassRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> noticeService.create(
                    new NoticeCreateRequest(NoticeScope.CLASS, 999L, "제목", "내용", null), 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class Search {

        @Test
        void scope_classId_limit으로_조회한다() {
            Notice notice = Notice.builder().author(admin).scope(NoticeScope.ALL).title("공지").content("내용").pinned(false).build();
            given(noticeRepository.search(eq(NoticeScope.CLASS), eq(3L), eq(PageRequest.of(0, 5))))
                    .willReturn(List.of(notice));

            List<NoticeResponse> result = noticeService.search("class", 3L, 5);

            assertThat(result).hasSize(1);
        }

        @Test
        void limit이_없으면_unpaged로_조회한다() {
            given(noticeRepository.search(eq(null), eq(null), eq(Pageable.unpaged()))).willReturn(List.of());

            List<NoticeResponse> result = noticeService.search(null, null, null);

            assertThat(result).isEmpty();
        }

        @Test
        void scope_형식이_올바르지_않으면_VALIDATION_ERROR_예외() {
            assertThatThrownBy(() -> noticeService.search("invalid", null, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    class GetUpdateDelete {

        @Test
        void 공지가_없으면_NOT_FOUND_예외() {
            given(noticeRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> noticeService.getNotice(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        void 공지를_수정한다() {
            Notice notice = Notice.builder().author(admin).scope(NoticeScope.ALL).title("원제목").content("원내용").pinned(false).build();
            given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

            NoticeResponse response = noticeService.update(1L, new NoticeUpdateRequest("새 제목", null));

            assertThat(response.title()).isEqualTo("새 제목");
            assertThat(response.content()).isEqualTo("원내용");
        }

        @Test
        void 공지를_삭제한다() {
            Notice notice = Notice.builder().author(admin).scope(NoticeScope.ALL).title("제목").content("내용").pinned(false).build();
            given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

            noticeService.delete(1L);

            ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
            verify(noticeRepository).delete(captor.capture());
            assertThat(captor.getValue()).isSameAs(notice);
        }

        @Test
        void 상단_고정_상태를_변경한다() {
            Notice notice = Notice.builder().author(admin).scope(NoticeScope.ALL).title("제목").content("내용").pinned(false).build();
            given(noticeRepository.findById(1L)).willReturn(Optional.of(notice));

            NoticeResponse response = noticeService.updatePinned(1L, new NoticePinUpdateRequest(true));

            assertThat(response.isPinned()).isTrue();
        }
    }

    @Nested
    class RelevantToStudent {

        @Test
        void 소속_반이_있으면_전체_공지와_반_공지를_함께_조회한다() {
            Student student = Student.builder().name("김민준").schoolClass(schoolClass).build();
            ReflectionTestUtils.setField(student, "id", 101L);
            given(studentRepository.findById(101L)).willReturn(Optional.of(student));
            given(noticeRepository.findRelevantToClass(eq(3L), eq(Pageable.unpaged()))).willReturn(List.of());

            noticeService.getRelevantToStudent(101L, null);

            verify(noticeRepository).findRelevantToClass(eq(3L), eq(Pageable.unpaged()));
            verify(noticeRepository, never()).search(any(), any(), any());
        }

        @Test
        void 소속_반이_없으면_전체_공지만_조회한다() {
            Student student = Student.builder().name("김민준").build();
            ReflectionTestUtils.setField(student, "id", 102L);
            given(studentRepository.findById(102L)).willReturn(Optional.of(student));
            given(noticeRepository.search(eq(NoticeScope.ALL), eq(null), eq(Pageable.unpaged()))).willReturn(List.of());

            noticeService.getRelevantToStudent(102L, null);

            verify(noticeRepository).search(eq(NoticeScope.ALL), eq(null), eq(Pageable.unpaged()));
        }

        @Test
        void 학생이_없으면_NOT_FOUND_예외() {
            given(studentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> noticeService.getRelevantToStudent(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }
}
