package academic.academic.domain.test.controller;

import academic.academic.domain.test.dto.TestRecordBulkRequest;
import academic.academic.domain.test.dto.TestRecordResponse;
import academic.academic.domain.test.dto.TestSessionCreateRequest;
import academic.academic.domain.test.dto.TestSessionResponse;
import academic.academic.domain.test.service.TestRecordService;
import academic.academic.domain.test.service.TestSessionService;
import academic.academic.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 테스트 API (SCR-09, SCR-15, FR-04-01 ~ FR-04-06)
 */
@RestController
@RequiredArgsConstructor
public class TestController {

    private final TestSessionService testSessionService;
    private final TestRecordService testRecordService;

    @GetMapping("/v1/test-sessions")
    public ApiResponse<List<TestSessionResponse>> getTestSessions(@RequestParam Long classId) {
        return ApiResponse.of(testSessionService.search(classId));
    }

    @PostMapping("/v1/test-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TestSessionResponse> createTestSession(@Valid @RequestBody TestSessionCreateRequest request) {
        return ApiResponse.of(testSessionService.create(request));
    }

    @GetMapping("/v1/test-sessions/{id}/records")
    public ApiResponse<List<TestRecordResponse>> getTestSessionRecords(@PathVariable Long id) {
        return ApiResponse.of(testRecordService.getSessionRecords(id));
    }

    @PostMapping("/v1/test-records/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<TestRecordResponse>> saveBulk(@Valid @RequestBody TestRecordBulkRequest request) {
        return ApiResponse.of(testRecordService.saveBulk(request));
    }

    @GetMapping("/v1/students/{studentId}/tests")
    public ApiResponse<List<TestRecordResponse>> getStudentTests(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.of(testRecordService.getStudentTests(studentId, limit));
    }
}
