package academic.academic.domain.student.controller;

import academic.academic.domain.student.dto.AttendanceSummaryResponse;
import academic.academic.domain.student.dto.HomeworkSummaryItem;
import academic.academic.domain.student.dto.RecentMonthlyExamSummary;
import academic.academic.domain.student.dto.RecentTestSummary;
import academic.academic.domain.student.dto.StudentSummaryResponse;
import academic.academic.domain.student.dto.StudentSummaryStudentInfo;
import academic.academic.domain.student.dto.TestScoresSummary;
import academic.academic.domain.student.service.StudentService;
import academic.academic.domain.student.service.StudentSummaryService;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private StudentSummaryService studentSummaryService;

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

        mockMvc.perform(get("/v1/students/101/summary").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student.name").value("김민준"))
                .andExpect(jsonPath("$.data.attendance.presentDays").value(18))
                .andExpect(jsonPath("$.data.homework[0].title").value("구문 노트 정리"))
                .andExpect(jsonPath("$.data.recentTest.scores.vocab").value(18))
                .andExpect(jsonPath("$.data.recentMonthlyExam.deltaFromPrev").value(-2));
    }

    @Test
    void month_생략시_기본값으로_조회한다() throws Exception {
        given(studentSummaryService.getSummary(eq(101L), isNull())).willReturn(
                new StudentSummaryResponse(
                        new StudentSummaryStudentInfo(101L, "김민준", "중2", "중2 심화반"),
                        new AttendanceSummaryResponse(0, 0, 0, 0),
                        List.of(), null, null));

        mockMvc.perform(get("/v1/students/101/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentTest").doesNotExist());
    }

    @Test
    void 학생이_없으면_404를_반환한다() throws Exception {
        given(studentSummaryService.getSummary(eq(999L), isNull()))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=999"));

        mockMvc.perform(get("/v1/students/999/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void month_형식이_올바르지_않으면_422를_반환한다() throws Exception {
        given(studentSummaryService.getSummary(eq(101L), eq("2026/08")))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "month 형식이 올바르지 않습니다. (yyyy-MM)"));

        mockMvc.perform(get("/v1/students/101/summary").param("month", "2026/08"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
