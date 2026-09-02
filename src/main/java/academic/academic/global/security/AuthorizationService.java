package academic.academic.global.security;

import academic.academic.domain.parentstudent.repository.ParentStudentRepository;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.domain.teacherassignment.repository.TeacherAssignmentRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API_명세서_V2 §15 권한 매트릭스를 서버에서 강제하는 중앙 체크 지점. 역할이 맞아도 범위(담당 반/자녀/
 * 본인)를 벗어나면 FORBIDDEN_SCOPE로 거부한다(NFR-05). admin은 모든 소유권 체크를 통과한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final SchoolClassRepository schoolClassRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;

    public void requireRole(AuthenticatedUser me, Role... allowed) {
        for (Role role : allowed) {
            if (me.role() == role) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN_ROLE, "이 API에 접근할 수 있는 역할이 아닙니다.");
    }

    public boolean teacherOwnsClass(Long teacherId, Long classId) {
        return schoolClassRepository.existsByIdAndTeacherId(classId, teacherId)
                || teacherAssignmentRepository.existsByTeacherIdAndSchoolClassId(teacherId, classId);
    }

    public void requireTeacherOwnsClass(Long teacherId, Long classId) {
        if (!teacherOwnsClass(teacherId, classId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "담당하지 않은 반의 데이터에는 접근할 수 없습니다.");
        }
    }

    public boolean teacherOwnsStudent(Long teacherId, Long studentId) {
        if (teacherAssignmentRepository.existsByTeacherIdAndStudentId(teacherId, studentId)) {
            return true;
        }
        return studentRepository.findById(studentId)
                .map(student -> student.getSchoolClass() != null
                        && teacherOwnsClass(teacherId, student.getSchoolClass().getId()))
                .orElse(false);
    }

    public void requireTeacherOwnsStudent(Long teacherId, Long studentId) {
        if (!teacherOwnsStudent(teacherId, studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "담당하지 않은 학생의 데이터에는 접근할 수 없습니다.");
        }
    }

    public boolean parentOwnsStudent(Long parentUserId, Long studentId) {
        return parentStudentRepository.existsByParentUserIdAndStudentId(parentUserId, studentId);
    }

    public void requireParentOwnsStudent(Long parentUserId, Long studentId) {
        if (!parentOwnsStudent(parentUserId, studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "자녀가 아닌 학생의 데이터에는 접근할 수 없습니다.");
        }
    }

    public boolean isSelf(Long studentUserId, Long studentId) {
        return studentRepository.existsByIdAndUserId(studentId, studentUserId);
    }

    /**
     * admin(전체) / teacher(담당) / parent(자녀) / student(본인)만 통과. 나머지는 FORBIDDEN_SCOPE.
     * /students/{id}/* 계열 조회 API 전반에서 공통으로 쓰는 조합 체크.
     */
    public void requireCanViewStudent(AuthenticatedUser me, Long studentId) {
        boolean allowed = switch (me.role()) {
            case ADMIN -> true;
            case TEACHER -> teacherOwnsStudent(me.id(), studentId);
            case PARENT -> parentOwnsStudent(me.id(), studentId);
            case STUDENT -> isSelf(me.id(), studentId);
        };
        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "해당 학생의 데이터에는 접근할 수 없습니다.");
        }
    }

    /**
     * scope=class 공지처럼 "그 반과 관련 있는 사람만 볼 수 있는" 콘텐츠 조회 가능 여부.
     * admin은 전체, teacher는 담당 반, parent는 그 반에 자녀가 있으면, student는 본인이 그 반 소속이면 true.
     */
    public boolean canViewClassScopedContent(AuthenticatedUser me, Long classId) {
        return switch (me.role()) {
            case ADMIN -> true;
            case TEACHER -> teacherOwnsClass(me.id(), classId);
            case PARENT -> parentStudentRepository.findByParentUserId(me.id()).stream()
                    .anyMatch(link -> link.getStudent().getSchoolClass() != null
                            && link.getStudent().getSchoolClass().getId().equals(classId));
            case STUDENT -> studentRepository.findByUserId(me.id())
                    .map(s -> s.getSchoolClass() != null && s.getSchoolClass().getId().equals(classId))
                    .orElse(false);
        };
    }

    /**
     * 공지 수정/삭제/고정처럼 "본인이 작성했고 + 지금도 그 반을 담당해야" 통과하는 체크
     * (API_명세서_V2 §14 "teacher(본인이 작성한 담당 반 공지만)").
     */
    public void requireAuthorAndOwnsClass(AuthenticatedUser me, Long resourceAuthorId, Long classId) {
        if (me.role() == Role.ADMIN) {
            return;
        }
        if (me.role() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ROLE, "이 API에 접근할 수 있는 역할이 아닙니다.");
        }
        boolean isAuthor = resourceAuthorId != null && resourceAuthorId.equals(me.id());
        boolean ownsClass = classId != null && teacherOwnsClass(me.id(), classId);
        if (!isAuthor || !ownsClass) {
            throw new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "본인이 작성한 담당 반 공지만 수정할 수 있습니다.");
        }
    }

    /** admin이거나 담당 반이면 통과 — classId 기반 입력(bulk 등) API 공통 체크. */
    public void requireCanManageClass(AuthenticatedUser me, Long classId) {
        requireCanManageClassOrStudent(me, classId, null);
    }

    /**
     * 숙제 항목처럼 classId 또는 studentId 중 하나로만 스코프가 정해지는 자원의 입력·수정 권한 체크.
     * admin이거나, teacher가 그 반/학생을 담당하면 통과한다.
     */
    public void requireCanManageClassOrStudent(AuthenticatedUser me, Long classId, Long studentId) {
        if (me.role() == Role.ADMIN) {
            return;
        }
        if (me.role() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ROLE, "이 API에 접근할 수 있는 역할이 아닙니다.");
        }
        if (classId != null) {
            requireTeacherOwnsClass(me.id(), classId);
        } else if (studentId != null) {
            requireTeacherOwnsStudent(me.id(), studentId);
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "담당하지 않은 데이터에는 접근할 수 없습니다.");
        }
    }
}
