package academic.academic.domain.student.controller;

import academic.academic.domain.student.dto.StudentCreateRequest;
import academic.academic.domain.student.dto.StudentResponse;
import academic.academic.domain.student.dto.StudentSummaryResponse;
import academic.academic.domain.student.dto.StudentUpdateRequest;
import academic.academic.domain.student.service.StudentService;
import academic.academic.domain.student.service.StudentSummaryService;
import academic.academic.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 학생 관리 API (SCR-04, SCR-11, SCR-12, FR-01-01, FR-01-04, FR-01-05, FR-01-06, FR-07-01)
 */
@RestController
@RequestMapping("/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentSummaryService studentSummaryService;

    @GetMapping
    public ApiResponse<List<StudentResponse>> getStudents(@RequestParam(required = false) Long classId,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String keyword) {
        return ApiResponse.of(studentService.getStudents(classId, status, keyword));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> createStudent(@Valid @RequestBody StudentCreateRequest request) {
        return ApiResponse.of(studentService.createStudent(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResponse> getStudent(@PathVariable Long id) {
        return ApiResponse.of(studentService.getStudent(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<StudentResponse> updateStudent(@PathVariable Long id,
                                                        @RequestBody StudentUpdateRequest request) {
        return ApiResponse.of(studentService.updateStudent(id, request));
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<StudentSummaryResponse> getStudentSummary(@PathVariable Long id,
                                                                    @RequestParam(required = false) String month) {
        return ApiResponse.of(studentSummaryService.getSummary(id, month));
    }
}
