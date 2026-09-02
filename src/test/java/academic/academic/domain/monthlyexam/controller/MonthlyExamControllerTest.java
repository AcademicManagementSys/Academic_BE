package academic.academic.domain.monthlyexam.controller;

import academic.academic.domain.monthlyexam.dto.MonthlyExamCreateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordCreateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordDetailResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordUpdateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamTrendResponse;
import academic.academic.domain.monthlyexam.dto.ScoreFeedbackResponse;
import academic.academic.domain.monthlyexam.dto.ScoreFeedbackUpsertRequest;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackCreateRequest;
import academic.academic.domain.monthlyexam.dto.TypeFeedbackResponse;
import academic.academic.domain.monthlyexam.entity.FeedbackStatus;
import academic.academic.domain.monthlyexam.service.MonthlyExamFeedbackService;
import academic.academic.domain.monthlyexam.service.MonthlyExamRecordService;
import academic.academic.domain.monthlyexam.service.MonthlyExamService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.JwtProvider;
import academic.academic.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonthlyExamController.class)
@Import(JwtProvider.class)
class MonthlyExamControllerTest {

    private static final String TEACHER_TOKEN = AuthTestSupport.bearer(2L, Role.TEACHER);
    private static final String PARENT_TOKEN = AuthTestSupport.bearer(45L, Role.PARENT);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MonthlyExamService monthlyExamService;

    @MockitoBean
    private MonthlyExamRecordService monthlyExamRecordService;

    @MockitoBean
    private MonthlyExamFeedbackService monthlyExamFeedbackService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void 월말모의고사_회차_목록을_조회한다() throws Exception {
        given(monthlyExamService.list())
                .willReturn(List.of(new MonthlyExamResponse(12L, "8월 학평", "2026-08")));

        mockMvc.perform(get("/v1/monthly-exams").header("Authorization", TEACHER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].examName").value("8월 학평"));
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/v1/monthly-exams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 월말모의고사_회차를_생성하면_201을_반환한다() throws Exception {
        MonthlyExamCreateRequest request = new MonthlyExamCreateRequest("8월 학평", "2026-08");
        given(monthlyExamService.create(any(MonthlyExamCreateRequest.class)))
                .willReturn(new MonthlyExamResponse(12L, "8월 학평", "2026-08"));

        mockMvc.perform(post("/v1/monthly-exams").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.examName").value("8월 학평"));
    }

    @Test
    void examMonth_형식이_올바르지_않으면_422를_반환한다() throws Exception {
        String invalidJson = """
                {"examName":"8월 학평","examMonth":"2026-8"}
                """;

        mockMvc.perform(post("/v1/monthly-exams").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 회차의_반별_성적_목록을_조회한다() throws Exception {
        given(monthlyExamRecordService.getByClass(12L, 3L))
                .willReturn(List.of(new MonthlyExamRecordResponse(5000L, 12L, "8월 학평", "2026-08",
                        101L, "김민준", 82, 128, 91, "2등급")));

        mockMvc.perform(get("/v1/monthly-exams/12/records").header("Authorization", TEACHER_TOKEN).param("classId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].studentName").value("김민준"));
    }

    @Test
    void classId가_없으면_422를_반환한다() throws Exception {
        mockMvc.perform(get("/v1/monthly-exams/12/records").header("Authorization", TEACHER_TOKEN))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 성적을_등록하면_201을_반환한다() throws Exception {
        MonthlyExamRecordCreateRequest request = new MonthlyExamRecordCreateRequest(12L, 101L, 82, 128, 91, "2등급");
        given(monthlyExamRecordService.create(any(MonthlyExamRecordCreateRequest.class)))
                .willReturn(new MonthlyExamRecordResponse(5000L, 12L, "8월 학평", "2026-08", 101L, "김민준", 82, 128, 91, "2등급"));

        mockMvc.perform(post("/v1/monthly-exam-records").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rawScore").value(82));
    }

    @Test
    void 성적을_수정한다() throws Exception {
        given(monthlyExamRecordService.update(anyLong(), any(MonthlyExamRecordUpdateRequest.class)))
                .willReturn(new MonthlyExamRecordResponse(5000L, 12L, "8월 학평", "2026-08", 101L, "김민준", 85, 130, 93, "1등급"));

        mockMvc.perform(patch("/v1/monthly-exam-records/5000").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawScore\":85}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rawScore").value(85));
    }

    @Test
    void 존재하지_않는_성적을_수정하면_404를_반환한다() throws Exception {
        given(monthlyExamRecordService.update(anyLong(), any(MonthlyExamRecordUpdateRequest.class)))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "월말모의고사 성적을 찾을 수 없습니다. id=999"));

        mockMvc.perform(patch("/v1/monthly-exam-records/999").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawScore\":85}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 회차_상세를_조회한다() throws Exception {
        MonthlyExamRecordResponse record = new MonthlyExamRecordResponse(5000L, 12L, "8월 학평", "2026-08",
                101L, "김민준", 82, 128, 91, "2등급");
        TypeFeedbackResponse typeFeedback = new TypeFeedbackResponse(1L, 1L, "어휘", FeedbackStatus.STRENGTH, "우수합니다.");
        ScoreFeedbackResponse scoreFeedback = new ScoreFeedbackResponse("80점대", "탄탄합니다.");
        given(monthlyExamFeedbackService.getDetail(5000L))
                .willReturn(new MonthlyExamRecordDetailResponse(record, List.of(typeFeedback), scoreFeedback));

        mockMvc.perform(get("/v1/monthly-exam-records/5000").header("Authorization", TEACHER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record.rawScore").value(82))
                .andExpect(jsonPath("$.data.typeFeedbacks[0].typeCategory").value("어휘"))
                .andExpect(jsonPath("$.data.scoreFeedback.scoreBand").value("80점대"));
    }

    @Test
    void 학생_월말모의고사_추이를_조회한다() throws Exception {
        given(monthlyExamRecordService.getStudentTrend(101L, 5))
                .willReturn(List.of(new MonthlyExamTrendResponse("2026-08", 82)));

        mockMvc.perform(get("/v1/students/101/monthly-exams").header("Authorization", PARENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rawScore").value(82));
    }

    @Test
    void limit_파라미터를_전달하면_그대로_사용한다() throws Exception {
        given(monthlyExamRecordService.getStudentTrend(101L, 3)).willReturn(List.of());

        mockMvc.perform(get("/v1/students/101/monthly-exams").header("Authorization", PARENT_TOKEN).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 유형별_피드백을_추가하면_201을_반환한다() throws Exception {
        TypeFeedbackCreateRequest request = new TypeFeedbackCreateRequest(1L, FeedbackStatus.STRENGTH, "우수합니다.");
        given(monthlyExamFeedbackService.addTypeFeedback(anyLong(), any(TypeFeedbackCreateRequest.class)))
                .willReturn(new TypeFeedbackResponse(1L, 1L, "어휘", FeedbackStatus.STRENGTH, "우수합니다."));

        mockMvc.perform(post("/v1/monthly-exam-records/5000/type-feedbacks").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.typeCategory").value("어휘"));
    }

    @Test
    void 유형별_피드백을_수정한다() throws Exception {
        given(monthlyExamFeedbackService.updateTypeFeedback(anyLong(), any()))
                .willReturn(new TypeFeedbackResponse(1L, 1L, "어휘", FeedbackStatus.NEEDS_WORK, "보완 필요"));

        mockMvc.perform(patch("/v1/type-feedbacks/1").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"needsWork\",\"feedbackText\":\"보완 필요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("needsWork"));
    }

    @Test
    void 유형별_피드백을_삭제하면_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/v1/type-feedbacks/1").header("Authorization", TEACHER_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void 점수대별_피드백을_등록_수정하면_200을_반환한다() throws Exception {
        ScoreFeedbackUpsertRequest request = new ScoreFeedbackUpsertRequest("80점대", "탄탄합니다.");
        given(monthlyExamFeedbackService.upsertScoreFeedback(anyLong(), any(ScoreFeedbackUpsertRequest.class)))
                .willReturn(new ScoreFeedbackResponse("80점대", "탄탄합니다."));

        mockMvc.perform(put("/v1/monthly-exam-records/5000/score-feedback").header("Authorization", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreBand").value("80점대"));
    }
}
