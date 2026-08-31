package academic.academic.domain.monthlyexam.controller;

import academic.academic.domain.monthlyexam.dto.TypeCategoryCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeCategoryResponse;
import academic.academic.domain.monthlyexam.service.TypeCategoryService;
import academic.academic.global.response.ApiResponse;
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
 * 유형 카테고리 마스터 API (FR-05-08)
 */
@RestController
@RequiredArgsConstructor
public class TypeCategoryController {

    private final TypeCategoryService typeCategoryService;

    @GetMapping("/v1/type-categories")
    public ApiResponse<List<TypeCategoryResponse>> getTypeCategories() {
        return ApiResponse.of(typeCategoryService.list());
    }

    @PostMapping("/v1/type-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TypeCategoryResponse> createTypeCategory(@Valid @RequestBody TypeCategoryCreateRequest request) {
        return ApiResponse.of(typeCategoryService.create(request));
    }
}
