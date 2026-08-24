package academic.academic.domain.parentstudent.controller;

import academic.academic.domain.parentstudent.dto.ChildResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentCreateRequest;
import academic.academic.domain.parentstudent.dto.ParentStudentResponse;
import academic.academic.domain.parentstudent.service.ParentStudentService;
import academic.academic.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 학부모-자녀 연결 API (SCR-17, FR-01-04).
 * 인증/세션 체계가 아직 없어 스펙의 /me/children 대신 parentUserId를 명시하는 경로를 사용한다.
 */
@RestController
@RequiredArgsConstructor
public class ParentStudentController {

    private final ParentStudentService parentStudentService;

    @GetMapping("/v1/parents/{parentUserId}/children")
    public ApiResponse<List<ChildResponse>> getChildren(@PathVariable Long parentUserId) {
        return ApiResponse.of(parentStudentService.getChildren(parentUserId));
    }

    @PostMapping("/v1/parent-links")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParentStudentResponse> createLink(@Valid @RequestBody ParentStudentCreateRequest request) {
        return ApiResponse.of(parentStudentService.createLink(request));
    }

    @DeleteMapping("/v1/parent-links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLink(@PathVariable Long id) {
        parentStudentService.deleteLink(id);
    }
}
