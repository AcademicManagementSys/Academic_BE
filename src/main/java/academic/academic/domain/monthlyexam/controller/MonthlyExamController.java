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
import academic.academic.domain.monthlyexam.dto.TypeFeedbackUpdateRequest;
import academic.academic.domain.monthlyexam.service.MonthlyExamFeedbackService;
import academic.academic.domain.monthlyexam.service.MonthlyExamRecordService;
import academic.academic.domain.monthlyexam.service.MonthlyExamService;
import academic.academic.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 월말모의고사 API (SCR-10, SCR-16, FR-05-01 ~ FR-05-08)
 */
@RestController
@RequiredArgsConstructor
public class MonthlyExamController {

    private final MonthlyExamService monthlyExamService;
    private final MonthlyExamRecordService monthlyExamRecordService;
    private final MonthlyExamFeedbackService monthlyExamFeedbackService;

    @GetMapping("/v1/monthly-exams")
    public ApiResponse<List<MonthlyExamResponse>> getMonthlyExams() {
        return ApiResponse.of(monthlyExamService.list());
    }

    @PostMapping("/v1/monthly-exams")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MonthlyExamResponse> createMonthlyExam(@Valid @RequestBody MonthlyExamCreateRequest request) {
        return ApiResponse.of(monthlyExamService.create(request));
    }

    @GetMapping("/v1/monthly-exams/{id}/records")
    public ApiResponse<List<MonthlyExamRecordResponse>> getMonthlyExamRecords(
            @PathVariable Long id,
            @RequestParam Long classId) {
        return ApiResponse.of(monthlyExamRecordService.getByClass(id, classId));
    }

    @PostMapping("/v1/monthly-exam-records")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MonthlyExamRecordResponse> createMonthlyExamRecord(
            @Valid @RequestBody MonthlyExamRecordCreateRequest request) {
        return ApiResponse.of(monthlyExamRecordService.create(request));
    }

    @PatchMapping("/v1/monthly-exam-records/{id}")
    public ApiResponse<MonthlyExamRecordResponse> updateMonthlyExamRecord(
            @PathVariable Long id,
            @RequestBody MonthlyExamRecordUpdateRequest request) {
        return ApiResponse.of(monthlyExamRecordService.update(id, request));
    }

    @GetMapping("/v1/monthly-exam-records/{id}")
    public ApiResponse<MonthlyExamRecordDetailResponse> getMonthlyExamRecordDetail(@PathVariable Long id) {
        return ApiResponse.of(monthlyExamFeedbackService.getDetail(id));
    }

    @GetMapping("/v1/students/{studentId}/monthly-exams")
    public ApiResponse<List<MonthlyExamTrendResponse>> getStudentMonthlyExams(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.of(monthlyExamRecordService.getStudentTrend(studentId, limit));
    }

    @PostMapping("/v1/monthly-exam-records/{id}/type-feedbacks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TypeFeedbackResponse> addTypeFeedback(
            @PathVariable Long id,
            @Valid @RequestBody TypeFeedbackCreateRequest request) {
        return ApiResponse.of(monthlyExamFeedbackService.addTypeFeedback(id, request));
    }

    @PatchMapping("/v1/type-feedbacks/{id}")
    public ApiResponse<TypeFeedbackResponse> updateTypeFeedback(
            @PathVariable Long id,
            @RequestBody TypeFeedbackUpdateRequest request) {
        return ApiResponse.of(monthlyExamFeedbackService.updateTypeFeedback(id, request));
    }

    @DeleteMapping("/v1/type-feedbacks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTypeFeedback(@PathVariable Long id) {
        monthlyExamFeedbackService.deleteTypeFeedback(id);
    }

    @PutMapping("/v1/monthly-exam-records/{id}/score-feedback")
    public ApiResponse<ScoreFeedbackResponse> upsertScoreFeedback(
            @PathVariable Long id,
            @Valid @RequestBody ScoreFeedbackUpsertRequest request) {
        return ApiResponse.of(monthlyExamFeedbackService.upsertScoreFeedback(id, request));
    }
}
