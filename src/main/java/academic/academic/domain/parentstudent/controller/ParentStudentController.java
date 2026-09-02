package academic.academic.domain.parentstudent.controller;

import academic.academic.domain.parentstudent.dto.ChildResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentCreateRequest;
import academic.academic.domain.parentstudent.dto.ParentStudentResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentUpdateRequest;
import academic.academic.domain.parentstudent.service.ParentStudentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 학부모-자녀 연결 API (SCR-17, FR-01-04). 연결 관리(생성/수정/해제)는 admin 전용, 자녀 목록 조회는
 * 로그인한 본인의 /me/children(API_명세서_V2 §8)이다.
 */
@RestController
@RequiredArgsConstructor
public class ParentStudentController {

    private final ParentStudentService parentStudentService;
    private final AuthorizationService authorizationService;

    @GetMapping("/v1/me/children")
    public ApiResponse<List<ChildResponse>> getMyChildren(@CurrentUser AuthenticatedUser me) {
        authorizationService.requireRole(me, Role.PARENT);
        return ApiResponse.of(parentStudentService.getChildren(me.id()));
    }

    @PostMapping("/v1/parent-links")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParentStudentResponse> createLink(@CurrentUser AuthenticatedUser me,
                                                           @Valid @RequestBody ParentStudentCreateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(parentStudentService.createLink(request));
    }

    @PatchMapping("/v1/parent-links/{id}")
    public ApiResponse<ParentStudentResponse> updateLink(@CurrentUser AuthenticatedUser me, @PathVariable Long id,
                                                            @Valid @RequestBody ParentStudentUpdateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(parentStudentService.updateLink(id, request));
    }

    @DeleteMapping("/v1/parent-links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLink(@CurrentUser AuthenticatedUser me, @PathVariable Long id) {
        authorizationService.requireRole(me, Role.ADMIN);
        parentStudentService.deleteLink(id);
    }
}
