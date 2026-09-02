package academic.academic.domain.teacherassignment.controller;

import academic.academic.domain.teacherassignment.dto.TeacherAssignmentCreateRequest;
import academic.academic.domain.teacherassignment.dto.TeacherAssignmentResponse;
import academic.academic.domain.teacherassignment.service.TeacherAssignmentService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 선생님 배정 API (FR-01-03). 반/학생 배정 관리는 원장/관리자 전용(SCR-05).
 */
@RestController
@RequestMapping("/v1/teacher-assignments")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService teacherAssignmentService;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ApiResponse<List<TeacherAssignmentResponse>> getAssignments(
            @CurrentUser AuthenticatedUser me,
            @RequestParam(required = false) Long teacherId) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(teacherAssignmentService.getAssignments(teacherId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherAssignmentResponse> createAssignment(
            @CurrentUser AuthenticatedUser me,
            @Valid @RequestBody TeacherAssignmentCreateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(teacherAssignmentService.createAssignment(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        authorizationService.requireRole(me, Role.ADMIN);
        teacherAssignmentService.deleteAssignment(id);
    }
}
