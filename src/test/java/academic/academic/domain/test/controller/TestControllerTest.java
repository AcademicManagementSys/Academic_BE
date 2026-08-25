package academic.academic.domain.test.controller;

import academic.academic.domain.test.dto.TestRecordBulkRequest;
import academic.academic.domain.test.dto.TestRecordItem;
import academic.academic.domain.test.dto.TestRecordResponse;
import academic.academic.domain.test.dto.TestSessionCreateRequest;
import academic.academic.domain.test.dto.TestSessionResponse;
import academic.academic.domain.test.entity.TestSubject;
import academic.academic.domain.test.service.TestRecordService;
import academic.academic.domain.test.service.TestSessionService;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TestSessionService testSessionService;

    @MockitoBean
    private TestRecordService testRecordService;

    @Test
    void 반의_테스트_회차_목록을_조회한다() throws Exception {
        given(testSessionService.search(3L))
                .willReturn(List.of(new TestSessionResponse(901L, 3L, "중2 심화반", "8월 3주차 테스트", LocalDate.of(2026, 8, 19))));

        mockMvc.perform(get("/v1/test-sessions").param("classId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("8월 3주차 테스트"));
    }

    @Test
    void classId가_없으면_422를_반환한다() throws Exception {
        mockMvc.perform(get("/v1/test-sessions"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 테스트_회차를_생성하면_201을_반환한다() throws Exception {
        TestSessionCreateRequest request = new TestSessionCreateRequest(3L, "8월 3주차 테스트", LocalDate.of(2026, 8, 19));
        given(testSessionService.create(any(TestSessionCreateRequest.class)))
                .willReturn(new TestSessionResponse(901L, 3L, "중2 심화반", "8월 3주차 테스트", LocalDate.of(2026, 8, 19)));

        mockMvc.perform(post("/v1/test-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("8월 3주차 테스트"));
    }

    @Test
    void 제목이_없는_요청은_422를_반환한다() throws Exception {
        String invalidJson = """
                {"classId":3,"testDate":"2026-08-19"}
                """;

        mockMvc.perform(post("/v1/test-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 회차_기록_매트릭스를_조회한다() throws Exception {
        given(testRecordService.getSessionRecords(901L))
                .willReturn(List.of(new TestRecordResponse(1L, 901L, "8월 3주차 테스트", LocalDate.of(2026, 8, 19),
                        101L, "김민준", TestSubject.VOCAB, true, 18, 20, "오타 1개")));

        mockMvc.perform(get("/v1/test-sessions/901/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].studentName").value("김민준"))
                .andExpect(jsonPath("$.data[0].subject").value("vocab"))
                .andExpect(jsonPath("$.data[0].isTaken").value(true));
    }

    @Test
    void 학생_영역_매트릭스를_일괄_저장하면_201을_반환한다() throws Exception {
        TestRecordBulkRequest request = new TestRecordBulkRequest(901L, List.of(
                new TestRecordItem(101L, TestSubject.VOCAB, true, 18, 20, "오타 1개")
        ));
        given(testRecordService.saveBulk(any(TestRecordBulkRequest.class)))
                .willReturn(List.of(new TestRecordResponse(1L, 901L, "8월 3주차 테스트", LocalDate.of(2026, 8, 19),
                        101L, "김민준", TestSubject.VOCAB, true, 18, 20, "오타 1개")));

        mockMvc.perform(post("/v1/test-records/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].score").value(18));
    }

    @Test
    void testSessionId가_없는_bulk_요청은_422를_반환한다() throws Exception {
        String invalidJson = """
                {"records":[{"studentId":101,"subject":"vocab","isTaken":true}]}
                """;

        mockMvc.perform(post("/v1/test-records/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 학생_테스트_이력을_조회한다() throws Exception {
        given(testRecordService.getStudentTests(101L, 10))
                .willReturn(List.of(new TestRecordResponse(1L, 901L, "8월 3주차 테스트", LocalDate.of(2026, 8, 19),
                        101L, "김민준", TestSubject.VOCAB, true, 18, 20, null)));

        mockMvc.perform(get("/v1/students/101/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].score").value(18));
    }

    @Test
    void limit_파라미터를_전달하면_그대로_사용한다() throws Exception {
        given(testRecordService.getStudentTests(101L, 5)).willReturn(List.of());

        mockMvc.perform(get("/v1/students/101/tests").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 존재하지_않는_회차의_기록을_조회하면_404를_반환한다() throws Exception {
        given(testRecordService.getSessionRecords(999L))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "테스트 회차를 찾을 수 없습니다. id=999"));

        mockMvc.perform(get("/v1/test-sessions/999/records"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
