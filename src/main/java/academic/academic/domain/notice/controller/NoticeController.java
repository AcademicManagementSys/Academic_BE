package academic.academic.domain.notice.controller;

import academic.academic.domain.notice.dto.NoticeCreateRequest;
import academic.academic.domain.notice.dto.NoticePinUpdateRequest;
import academic.academic.domain.notice.dto.NoticeResponse;
import academic.academic.domain.notice.dto.NoticeUpdateRequest;
import academic.academic.domain.notice.entity.NoticeScope;
import academic.academic.domain.notice.service.NoticeService;
import academic.academic.domain.parentstudent.dto.ChildResponse;
import academic.academic.domain.parentstudent.service.ParentStudentService;
import academic.academic.domain.student.service.StudentService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공지사항 API (SCR-18, SCR-19, FR-09-01 ~ FR-09-06)
 */
@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final StudentService studentService;
    private final ParentStudentService parentStudentService;
    private final AuthorizationService authorizationService;

    @GetMapping("/v1/notices")
    public ApiResponse<List<NoticeResponse>> getNotices(@CurrentUser AuthenticatedUser me,
                                                          @RequestParam(required = false) String scope,
                                                          @RequestParam(required = false) Long classId,
                                                          @RequestParam(required = false) Integer limit) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        if (me.role() == Role.TEACHER && classId != null) {
            authorizationService.requireTeacherOwnsClass(me.id(), classId);
        }
        List<NoticeResponse> notices = noticeService.search(scope, classId, limit);
        if (me.role() == Role.TEACHER && classId == null) {
            // admin(전체) 대비 teacher(전체+담당 반)로 결과를 좁힌다 (API_명세서_V2 §14).
            notices = notices.stream()
                    .filter(n -> n.scope() == NoticeScope.ALL
                            || (n.classId() != null && authorizationService.teacherOwnsClass(me.id(), n.classId())))
                    .toList();
        }
        return ApiResponse.of(notices);
    }

    @PostMapping("/v1/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeResponse> createNotice(@CurrentUser AuthenticatedUser me,
                                                     @Valid @RequestBody NoticeCreateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        if (request.scope() == NoticeScope.CLASS) {
            authorizationService.requireCanManageClass(me, request.classId());
        }
        return ApiResponse.of(noticeService.create(request, me.id()));
    }

    @GetMapping("/v1/notices/{id}")
    public ApiResponse<NoticeResponse> getNotice(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        NoticeService.NoticeScopeInfo scopeInfo = noticeService.getScopeInfo(id);
        if (scopeInfo.scope() == NoticeScope.CLASS && !authorizationService.canViewClassScopedContent(me, scopeInfo.classId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "본인·자녀와 관련 없는 반의 공지입니다.");
        }
        return ApiResponse.of(noticeService.getNotice(id));
    }

    @PatchMapping("/v1/notices/{id}")
    public ApiResponse<NoticeResponse> updateNotice(@CurrentUser AuthenticatedUser me, @PathVariable Long id,
                                                     @RequestBody NoticeUpdateRequest request) {
        requireCanModify(me, id);
        return ApiResponse.of(noticeService.update(id, request));
    }

    @DeleteMapping("/v1/notices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotice(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        requireCanModify(me, id);
        noticeService.delete(id);
    }

    @PatchMapping("/v1/notices/{id}/pin")
    public ApiResponse<NoticeResponse> updatePinned(@CurrentUser AuthenticatedUser me, @PathVariable Long id,
                                                     @Valid @RequestBody NoticePinUpdateRequest request) {
        requireCanModify(me, id);
        return ApiResponse.of(noticeService.updatePinned(id, request));
    }

    @GetMapping("/v1/me/notices")
    public ApiResponse<List<NoticeResponse>> getMyNotices(@CurrentUser AuthenticatedUser me,
                                                            @RequestHeader(value = "X-Active-Student-Id", required = false) Long activeStudentId,
                                                            @RequestParam(required = false) Integer limit) {
        authorizationService.requireRole(me, Role.PARENT, Role.STUDENT);
        Long studentId = resolveTargetStudentId(me, activeStudentId);
        return ApiResponse.of(noticeService.getRelevantToStudent(studentId, limit));
    }

    private void requireCanModify(AuthenticatedUser me, Long noticeId) {
        NoticeService.NoticeScopeInfo scopeInfo = noticeService.getScopeInfo(noticeId);
        if (scopeInfo.scope() == NoticeScope.ALL) {
            authorizationService.requireRole(me, Role.ADMIN);
            return;
        }
        authorizationService.requireAuthorAndOwnsClass(me, noticeService.getAuthorId(noticeId), scopeInfo.classId());
    }

    private Long resolveTargetStudentId(AuthenticatedUser me, Long activeStudentId) {
        if (me.role() == Role.STUDENT) {
            return studentService.findStudentIdByUserId(me.id());
        }
        List<ChildResponse> children = parentStudentService.getChildren(me.id());
        if (children.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "연결된 자녀가 없습니다.");
        }
        if (activeStudentId == null) {
            return children.get(0).studentId();
        }
        boolean isMyChild = children.stream().anyMatch(c -> c.studentId().equals(activeStudentId));
        if (!isMyChild) {
            throw new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "자녀가 아닌 학생입니다. studentId=" + activeStudentId);
        }
        return activeStudentId;
    }
}
