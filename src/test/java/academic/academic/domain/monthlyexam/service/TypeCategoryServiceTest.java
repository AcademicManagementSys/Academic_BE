package academic.academic.domain.monthlyexam.service;

import academic.academic.domain.monthlyexam.dto.TypeCategoryCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeCategoryResponse;
import academic.academic.domain.monthlyexam.entity.TypeCategory;
import academic.academic.domain.monthlyexam.repository.TypeCategoryRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TypeCategoryServiceTest {

    @Mock
    private TypeCategoryRepository typeCategoryRepository;

    private TypeCategoryService typeCategoryService;

    @BeforeEach
    void setUp() {
        typeCategoryService = new TypeCategoryService(typeCategoryRepository);
    }

    @Nested
    class Create {

        @Test
        void 유형_카테고리를_생성한다() {
            given(typeCategoryRepository.existsByName("어휘")).willReturn(false);
            given(typeCategoryRepository.save(any(TypeCategory.class))).willAnswer(invocation -> invocation.getArgument(0));

            TypeCategoryResponse response = typeCategoryService.create(new TypeCategoryCreateRequest("어휘"));

            assertThat(response.name()).isEqualTo("어휘");
        }

        @Test
        void 이미_존재하는_이름이면_VALIDATION_ERROR_예외() {
            given(typeCategoryRepository.existsByName("어휘")).willReturn(true);

            assertThatThrownBy(() -> typeCategoryService.create(new TypeCategoryCreateRequest("어휘")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    class ListCategories {

        @Test
        void 유형_카테고리_목록을_조회한다() {
            TypeCategory category = TypeCategory.builder().name("어휘").build();
            ReflectionTestUtils.setField(category, "id", 1L);
            given(typeCategoryRepository.findAllByOrderByNameAsc()).willReturn(List.of(category));

            List<TypeCategoryResponse> result = typeCategoryService.list();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("어휘");
        }
    }
}
