package academic.academic.domain.homework.controller;

import academic.academic.domain.homework.dto.HomeworkItemCreateRequest;
import academic.academic.domain.homework.dto.HomeworkItemRecordsGroup;
import academic.academic.domain.homework.dto.HomeworkItemResponse;
import academic.academic.domain.homework.dto.HomeworkItemUpdateRequest;
import academic.academic.domain.homework.dto.HomeworkRecordBulkRequest;
import academic.academic.domain.homework.dto.HomeworkRecordItem;
import academic.academic.domain.homework.dto.HomeworkRecordResponse;
import academic.academic.domain.homework.service.HomeworkItemService;
import academic.academic.domain.homework.service.HomeworkRecordService;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeworkController.class)
class HomeworkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HomeworkItemService homeworkItemService;

    @MockitoBean
    private HomeworkRecordService homeworkRecordService;

    @Test
    void 반의_숙제_항목_목록을_조회한다() throws Exception {
        given(homeworkItemService.search(3L, LocalDate.of(2026, 8, 17)))
                .willReturn(List.of(new HomeworkItemResponse(501L, 3L, "중2 심화반", null, null,
                        "단어장 Ch.5", "Unit 12-15", LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 19))));

        mockMvc.perform(get("/v1/homework-items").param("classId", "3").param("week", "2026-08-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("단어장 Ch.5"));
    }

    @Test
    void classId가_없으면_422를_반환한다() throws Exception {
        mockMvc.perform(get("/v1/homework-items"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 숙제_항목을_생성하면_201을_반환한다() throws Exception {
        HomeworkItemCreateRequest request = new HomeworkItemCreateRequest(
                3L, null, "단어장 Ch.5", "Unit 12-15", LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 19));
        given(homeworkItemService.create(any(HomeworkItemCreateRequest.class)))
                .willReturn(new HomeworkItemResponse(501L, 3L, "중2 심화반", null, null,
                        "단어장 Ch.5", "Unit 12-15", LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 19)));

        mockMvc.perform(post("/v1/homework-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("단어장 Ch.5"));
    }

    @Test
    void 제목이_없는_요청은_422를_반환한다() throws Exception {
        String invalidJson = """
                {"classId":3,"assignedDate":"2026-08-17"}
                """;

        mockMvc.perform(post("/v1/homework-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 숙제_항목을_수정한다() throws Exception {
        given(homeworkItemService.update(anyLong(), any(HomeworkItemUpdateRequest.class)))
                .willReturn(new HomeworkItemResponse(501L, 3L, "중2 심화반", null, null,
                        "단어장 Ch.6", "Unit 16-20", LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 26)));

        mockMvc.perform(patch("/v1/homework-items/501")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"단어장 Ch.6\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("단어장 Ch.6"));
    }

    @Test
    void 존재하지_않는_항목을_수정하면_404를_반환한다() throws Exception {
        given(homeworkItemService.update(anyLong(), any(HomeworkItemUpdateRequest.class)))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "숙제 항목을 찾을 수 없습니다. id=999"));

        mockMvc.perform(patch("/v1/homework-items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"단어장 Ch.6\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 숙제_항목을_삭제하면_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/v1/homework-items/501"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 항목별_학생_기록을_조회한다() throws Exception {
        given(homeworkRecordService.getItemRecords(501L))
                .willReturn(List.of(new HomeworkRecordResponse(1L, 501L, "단어장 Ch.5",
                        LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 19), 101L, "김민준", true, 98, "오타 1개")));

        mockMvc.perform(get("/v1/homework-items/501/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].studentName").value("김민준"))
                .andExpect(jsonPath("$.data[0].isDone").value(true));
    }

    @Test
    void 학생_항목_매트릭스를_일괄_저장하면_201을_반환한다() throws Exception {
        HomeworkRecordBulkRequest request = new HomeworkRecordBulkRequest(3L, List.of(
                new HomeworkItemRecordsGroup(501L, List.of(new HomeworkRecordItem(101L, true, 98, "오타 1개")))
        ));
        given(homeworkRecordService.saveBulk(any(HomeworkRecordBulkRequest.class)))
                .willReturn(List.of(new HomeworkRecordResponse(1L, 501L, "단어장 Ch.5",
                        LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 19), 101L, "김민준", true, 98, "오타 1개")));

        mockMvc.perform(post("/v1/homework-records/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].score").value(98));
    }

    @Test
    void classId가_없는_bulk_요청은_422를_반환한다() throws Exception {
        String invalidJson = """
                {"items":[{"homeworkItemId":501,"records":[{"studentId":101,"isDone":true}]}]}
                """;

        mockMvc.perform(post("/v1/homework-records/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 학생_숙제_이력을_조회한다() throws Exception {
        given(homeworkRecordService.getStudentHomework(101L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .willReturn(List.of(
                        new HomeworkRecordResponse(1L, 501L, "단어장 Ch.5", LocalDate.of(2026, 8, 17),
                                LocalDate.of(2026, 8, 19), 101L, "김민준", true, 98, null),
                        new HomeworkRecordResponse(2L, 502L, "문제집 p.20-25", LocalDate.of(2026, 8, 10),
                                LocalDate.of(2026, 8, 12), 101L, "김민준", false, null, "재제출 요청")
                ));

        mockMvc.perform(get("/v1/students/101/homework")
                        .param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].comment").value("재제출 요청"));
    }

    @Test
    void from_to가_없으면_422를_반환한다() throws Exception {
        mockMvc.perform(get("/v1/students/101/homework"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
