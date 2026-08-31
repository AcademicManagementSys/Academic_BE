package academic.academic.domain.monthlyexam.controller;

import academic.academic.domain.monthlyexam.dto.TypeCategoryCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeCategoryResponse;
import academic.academic.domain.monthlyexam.service.TypeCategoryService;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TypeCategoryController.class)
class TypeCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TypeCategoryService typeCategoryService;

    @Test
    void 유형_카테고리_목록을_조회한다() throws Exception {
        given(typeCategoryService.list()).willReturn(List.of(new TypeCategoryResponse(1L, "어휘")));

        mockMvc.perform(get("/v1/type-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("어휘"));
    }

    @Test
    void 유형_카테고리를_생성하면_201을_반환한다() throws Exception {
        TypeCategoryCreateRequest request = new TypeCategoryCreateRequest("어휘");
        given(typeCategoryService.create(any(TypeCategoryCreateRequest.class)))
                .willReturn(new TypeCategoryResponse(1L, "어휘"));

        mockMvc.perform(post("/v1/type-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("어휘"));
    }

    @Test
    void 이름이_없으면_422를_반환한다() throws Exception {
        mockMvc.perform(post("/v1/type-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 중복된_이름이면_422를_반환한다() throws Exception {
        given(typeCategoryService.create(any(TypeCategoryCreateRequest.class)))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "이미 존재하는 유형 카테고리입니다: 어휘"));

        mockMvc.perform(post("/v1/type-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TypeCategoryCreateRequest("어휘"))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
