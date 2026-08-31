package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.TypeCategory;

public record TypeCategoryResponse(
        Long id,
        String name
) {
    public static TypeCategoryResponse from(TypeCategory typeCategory) {
        return new TypeCategoryResponse(typeCategory.getId(), typeCategory.getName());
    }
}
