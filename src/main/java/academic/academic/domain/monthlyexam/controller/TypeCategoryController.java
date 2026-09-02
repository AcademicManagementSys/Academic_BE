package academic.academic.domain.monthlyexam.controller;

import academic.academic.domain.monthlyexam.dto.TypeCategoryCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeCategoryResponse;
import academic.academic.domain.monthlyexam.service.TypeCategoryService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 유형 카테고리 마스터 API (FR-05-08). 조회는 로그인한 전체 역할, 추가는 admin 전용.
 */
@RestController
@RequiredArgsConstructor
public class TypeCategoryController {

    private final TypeCategoryService typeCategoryService;
    private final AuthorizationService authorizationService;

    @GetMapping("/v1/type-categories")
    public ApiResponse<List<TypeCategoryResponse>> getTypeCategories(@CurrentUser AuthenticatedUser me) {
        return ApiResponse.of(typeCategoryService.list());
    }

    @PostMapping("/v1/type-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TypeCategoryResponse> createTypeCategory(@CurrentUser AuthenticatedUser me,
                                                                  @Valid @RequestBody TypeCategoryCreateRequest request) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(typeCategoryService.create(request));
    }
}
