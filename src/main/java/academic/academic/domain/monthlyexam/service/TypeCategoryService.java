package academic.academic.domain.monthlyexam.service;

import academic.academic.domain.monthlyexam.dto.TypeCategoryCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeCategoryResponse;
import academic.academic.domain.monthlyexam.entity.TypeCategory;
import academic.academic.domain.monthlyexam.repository.TypeCategoryRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 유형 카테고리 마스터 관리 (FR-05-08).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TypeCategoryService {

    private final TypeCategoryRepository typeCategoryRepository;

    @Transactional
    public TypeCategoryResponse create(TypeCategoryCreateRequest request) {
        if (typeCategoryRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미 존재하는 유형 카테고리입니다: " + request.name());
        }
        TypeCategory typeCategory = typeCategoryRepository.save(TypeCategory.builder()
                .name(request.name())
                .build());
        return TypeCategoryResponse.from(typeCategory);
    }

    public List<TypeCategoryResponse> list() {
        return typeCategoryRepository.findAllByOrderByNameAsc().stream()
                .map(TypeCategoryResponse::from)
                .toList();
    }
}
