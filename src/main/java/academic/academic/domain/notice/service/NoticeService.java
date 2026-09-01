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
import academic.academic.global.util.EnumParser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공지사항 관리 (SCR-18, SCR-19, FR-09-01 ~ FR-09-06).
 * 인증/세션 체계가 아직 없어 작성자를 authorId로 명시하고, /me/notices 대신
 * 학생 id를 명시하는 경로로 학부모/학생용 조회를 제공한다(ParentStudentController와 동일한 방식).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public NoticeResponse create(NoticeCreateRequest request) {
        User author = userRepository.findById(request.authorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "작성자를 찾을 수 없습니다. id=" + request.authorId()));
        if (author.getRole() != Role.ADMIN && author.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "공지사항은 원장/관리자 또는 선생님만 작성할 수 있습니다.");
        }
        if (request.scope() == NoticeScope.ALL && author.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "학원 전체 대상 공지는 원장/관리자만 작성할 수 있습니다.");
        }

        SchoolClass schoolClass = null;
        if (request.scope() == NoticeScope.CLASS) {
            if (request.classId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "반별 공지는 classId가 필수입니다.");
            }
            schoolClass = schoolClassRepository.findById(request.classId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + request.classId()));
        }

        Notice notice = noticeRepository.save(Notice.builder()
                .author(author)
                .scope(request.scope())
                .schoolClass(schoolClass)
                .title(request.title())
                .content(request.content())
                .pinned(Boolean.TRUE.equals(request.isPinned()))
                .build());
        return NoticeResponse.from(notice);
    }

    public List<NoticeResponse> search(String scope, Long classId, Integer limit) {
        NoticeScope scopeEnum = EnumParser.parse(NoticeScope.class, scope, "scope");
        return noticeRepository.search(scopeEnum, classId, toPageable(limit)).stream()
                .map(NoticeResponse::from)
                .toList();
    }

    public NoticeResponse getNotice(Long id) {
        return NoticeResponse.from(getEntity(id));
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeUpdateRequest request) {
        Notice notice = getEntity(id);
        notice.update(request.title(), request.content());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(Long id) {
        noticeRepository.delete(getEntity(id));
    }

    @Transactional
    public NoticeResponse updatePinned(Long id, NoticePinUpdateRequest request) {
        Notice notice = getEntity(id);
        notice.changePinned(request.isPinned());
        return NoticeResponse.from(notice);
    }

    public List<NoticeResponse> getRelevantToStudent(Long studentId, Integer limit) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + studentId));

        SchoolClass schoolClass = student.getSchoolClass();
        List<Notice> notices = schoolClass != null
                ? noticeRepository.findRelevantToClass(schoolClass.getId(), toPageable(limit))
                : noticeRepository.search(NoticeScope.ALL, null, toPageable(limit));
        return notices.stream().map(NoticeResponse::from).toList();
    }

    private Pageable toPageable(Integer limit) {
        return limit != null ? PageRequest.of(0, limit) : Pageable.unpaged();
    }

    private Notice getEntity(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다. id=" + id));
    }
}
