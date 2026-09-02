package academic.academic.domain.student.controller;

import academic.academic.domain.student.dto.AttendanceSummaryResponse;
import academic.academic.domain.student.dto.HomeworkSummaryItem;
import academic.academic.domain.student.dto.NotificationBadgeResponse;
import academic.academic.domain.student.dto.RecentMonthlyExamSummary;
import academic.academic.domain.student.dto.RecentTestSummary;
import academic.academic.domain.student.dto.StudentSummaryResponse;
import academic.academic.domain.student.dto.StudentSummaryStudentInfo;
import academic.academic.domain.student.dto.TestScoresSummary;
import academic.academic.domain.student.service.NotificationBadgeService;
import academic.academic.domain.student.service.StudentService;
import academic.academic.domain.student.service.StudentSummaryService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@Import(JwtProvider.class)
class StudentControllerTest {

    private static final String TEACHER_TOKEN = AuthTestSupport.bearer(2L, Role.TEACHER);
    private static final String PARENT_TOKEN = AuthTestSupport.bearer(45L, Role.PARENT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private StudentSummaryService studentSummaryService;

    @MockitoBean
    private NotificationBadgeService notificationBadgeService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void 학생_홈_요약을_조회한다() throws Exception {
        StudentSummaryResponse response = new StudentSummaryResponse(
                new StudentSummaryStudentInfo(101L, "김민준", "중2", "중2 심화반"),
                new AttendanceSummaryResponse(18, 20, 1, 0),
                List.of(new HomeworkSummaryItem("구문 노트 정리", false, LocalDate.of(2026, 8, 17))),
                new RecentTestSummary(LocalDate.of(2026, 8, 19), new TestScoresSummary(18, 16, 11, 15)),
                new RecentMonthlyExamSummary("2026-08", 82, -2)
        );
        given(studentSummaryService.getSummary(eq(101L), eq("2026-08"))).willReturn(response);

        mockMvc.perform(get("/v1/students/101/summary").header("Authorization", PARENT_TOKEN).param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student.name").value("김민준"))
                .andExpect(jsonPath("$.data.attendance.presentDays").value(18))
                .andExpect(jsonPath("$.data.homework[0].title").value("구문 노트 정리"))
                .andExpect(jsonPath("$.data.recentTest.scores.vocab").value(18))
                .andExpect(jsonPath("$.data.recentMonthlyExam.deltaFromPrev").value(-2));
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/v1/students/101/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 자녀가_아니면_403을_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN_SCOPE, "해당 학생의 데이터에는 접근할 수 없습니다."))
                .given(authorizationService).requireCanViewStudent(any(), eq(101L));

        mockMvc.perform(get("/v1/students/101/summary").header("Authorization", PARENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_SCOPE"));
    }

    @Test
    void month_생략시_기본값으로_조회한다() throws Exception {
        given(studentSummaryService.getSummary(eq(101L), isNull())).willReturn(
                new StudentSummaryResponse(
                        new StudentSummaryStudentInfo(101L, "김민준", "중2", "중2 심화반"),
                        new AttendanceSummaryResponse(0, 0, 0, 0),
                        List.of(), null, null));

        mockMvc.perform(get("/v1/students/101/summary").header("Authorization", TEACHER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentTest").doesNotExist());
    }

    @Test
    void 학생이_없으면_404를_반환한다() throws Exception {
        given(studentSummaryService.getSummary(eq(999L), isNull()))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=999"));

        mockMvc.perform(get("/v1/students/999/summary").header("Authorization", TEACHER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void month_형식이_올바르지_않으면_422를_반환한다() throws Exception {
        given(studentSummaryService.getSummary(eq(101L), eq("2026/08")))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "month 형식이 올바르지 않습니다. (yyyy-MM)"));

        mockMvc.perform(get("/v1/students/101/summary").header("Authorization", TEACHER_TOKEN).param("month", "2026/08"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 알림_배지를_조회한다() throws Exception {
        given(notificationBadgeService.getBadge(eq(101L), eq("2026-08-19T00:00:00"))).willReturn(
                new NotificationBadgeResponse(LocalDateTime.of(2026, 8, 19, 0, 0), 1, 2, 0, 1, 4));

        mockMvc.perform(get("/v1/students/101/notifications/badge").header("Authorization", PARENT_TOKEN)
                        .param("since", "2026-08-19T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.homeworkCount").value(2));
    }

    @Test
    void since_생략시_기본값으로_배지를_조회한다() throws Exception {
        given(notificationBadgeService.getBadge(eq(101L), isNull())).willReturn(
                new NotificationBadgeResponse(LocalDateTime.of(2026, 9, 1, 0, 0), 0, 0, 0, 0, 0));

        mockMvc.perform(get("/v1/students/101/notifications/badge").header("Authorization", PARENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    void since_형식이_올바르지_않으면_422를_반환한다() throws Exception {
        given(notificationBadgeService.getBadge(eq(101L), eq("2026-08-19")))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "since 형식이 올바르지 않습니다. (yyyy-MM-ddTHH:mm:ss)"));

        mockMvc.perform(get("/v1/students/101/notifications/badge").header("Authorization", PARENT_TOKEN)
                        .param("since", "2026-08-19"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
