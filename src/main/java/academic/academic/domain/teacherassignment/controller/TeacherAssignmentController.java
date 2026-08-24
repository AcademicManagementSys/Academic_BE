package academic.academic.domain.teacherassignment.controller;

import academic.academic.domain.teacherassignment.dto.TeacherAssignmentCreateRequest;
import academic.academic.domain.teacherassignment.dto.TeacherAssignmentResponse;
import academic.academic.domain.teacherassignment.service.TeacherAssignmentService;
import academic.academic.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 선생님 배정 API (FR-01-03)
 */
@RestController
@RequestMapping("/v1/teacher-assignments")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService teacherAssignmentService;

    @GetMapping
    public ApiResponse<List<TeacherAssignmentResponse>> getAssignments(
            @RequestParam(required = false) Long teacherId) {
        return ApiResponse.of(teacherAssignmentService.getAssignments(teacherId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherAssignmentResponse> createAssignment(
            @Valid @RequestBody TeacherAssignmentCreateRequest request) {
        return ApiResponse.of(teacherAssignmentService.createAssignment(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable Long id) {
        teacherAssignmentService.deleteAssignment(id);
    }
}
