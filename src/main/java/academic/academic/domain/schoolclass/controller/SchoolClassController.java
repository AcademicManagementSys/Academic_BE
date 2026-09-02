package academic.academic.domain.schoolclass.controller;

import academic.academic.domain.schoolclass.dto.ClassCreateRequest;
import academic.academic.domain.schoolclass.dto.ClassResponse;
import academic.academic.domain.schoolclass.dto.ClassUpdateRequest;
import academic.academic.domain.schoolclass.service.SchoolClassService;
import academic.academic.domain.student.dto.StudentResponse;
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
 * 반 관리 API (SCR-05, FR-01-02). admin은 전체, teacher는 담당 반만 조회 가능 (API_명세서_V2 §6).
 */
@RestController
@RequestMapping("/v1/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ApiResponse<List<ClassResponse>> getClasses(@CurrentUser AuthenticatedUser me,
                                                         @RequestParam(required = false) Long teacherId) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        Long effectiveTeacherId = me.role() == Role.TEACHER ? me.id() : teacherId;
        return ApiResponse.of(schoolClassService.getClasses(effectiveTeacherId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassResponse> createClass(@CurrentUser AuthenticatedUser me,
                                                   @Valid @RequestBody ClassCreateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(schoolClassService.createClass(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClassResponse> getClass(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        if (me.role() == Role.TEACHER) {
            authorizationService.requireTeacherOwnsClass(me.id(), id);
        }
        return ApiResponse.of(schoolClassService.getClass(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ClassResponse> updateClass(@CurrentUser AuthenticatedUser me, @PathVariable Long id,
                                                   @RequestBody ClassUpdateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(schoolClassService.updateClass(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClass(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        authorizationService.requireRole(me, Role.ADMIN);
        schoolClassService.deleteClass(id);
    }

    @GetMapping("/{id}/students")
    public ApiResponse<List<StudentResponse>> getClassStudents(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        if (me.role() == Role.TEACHER) {
            authorizationService.requireTeacherOwnsClass(me.id(), id);
        }
        return ApiResponse.of(schoolClassService.getClassStudents(id));
    }
}
