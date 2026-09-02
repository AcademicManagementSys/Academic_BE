package academic.academic.domain.student.controller;

import academic.academic.domain.student.dto.NotificationBadgeResponse;
import academic.academic.domain.student.dto.StudentCreateRequest;
import academic.academic.domain.student.dto.StudentResponse;
import academic.academic.domain.student.dto.StudentSummaryResponse;
import academic.academic.domain.student.dto.StudentUpdateRequest;
import academic.academic.domain.student.service.NotificationBadgeService;
import academic.academic.domain.student.service.StudentService;
import academic.academic.domain.student.service.StudentSummaryService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 학생 관리 API (SCR-04, SCR-11, SCR-12, FR-01-01, FR-01-04, FR-01-05, FR-01-06, FR-07-01, FR-08-01)
 */
@RestController
@RequestMapping("/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentSummaryService studentSummaryService;
    private final NotificationBadgeService notificationBadgeService;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ApiResponse<List<StudentResponse>> getStudents(@CurrentUser AuthenticatedUser me,
                                                            @RequestParam(required = false) Long classId,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String keyword) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        if (me.role() == Role.TEACHER) {
            if (classId != null) {
                authorizationService.requireTeacherOwnsClass(me.id(), classId);
                return ApiResponse.of(studentService.getStudents(classId, status, keyword));
            }
            return ApiResponse.of(studentService.getStudentsForTeacher(me.id(), status, keyword));
        }
        return ApiResponse.of(studentService.getStudents(classId, status, keyword));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> createStudent(@CurrentUser AuthenticatedUser me,
                                                        @Valid @RequestBody StudentCreateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(studentService.createStudent(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResponse> getStudent(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        authorizationService.requireCanViewStudent(me, id);
        return ApiResponse.of(studentService.getStudent(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<StudentResponse> updateStudent(@CurrentUser AuthenticatedUser me, @PathVariable Long id,
                                                        @RequestBody StudentUpdateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(studentService.updateStudent(id, request));
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<StudentSummaryResponse> getStudentSummary(@CurrentUser AuthenticatedUser me, @PathVariable Long id,
                                                                    @RequestParam(required = false) String month) {
        // API_명세서_V2 §5는 SCR-11(선생님용 상세)만 명시하지만, FR-07-01(학부모/학생 홈 요약)도
        // 이 API를 그대로 쓰고 있어 본인/자녀 조회까지 함께 허용한다(Document/스펙_변경_제안.md 참고).
        authorizationService.requireCanViewStudent(me, id);
        return ApiResponse.of(studentSummaryService.getSummary(id, month));
    }

    @GetMapping("/{id}/notifications/badge")
    public ApiResponse<NotificationBadgeResponse> getNotificationBadge(@CurrentUser AuthenticatedUser me, @PathVariable Long id,
                                                                          @RequestParam(required = false) String since) {
        authorizationService.requireCanViewStudent(me, id);
        return ApiResponse.of(notificationBadgeService.getBadge(id, since));
    }
}
