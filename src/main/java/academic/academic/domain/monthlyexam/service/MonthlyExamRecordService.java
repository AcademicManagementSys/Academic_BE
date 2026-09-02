package academic.academic.domain.monthlyexam.service;

import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordCreateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordResponse;
import academic.academic.domain.monthlyexam.dto.MonthlyExamRecordUpdateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamTrendResponse;
import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.entity.MonthlyExamRecord;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRecordRepository;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRepository;
import academic.academic.domain.schoolclass.repository.SchoolClassRepository;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.student.repository.StudentRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyExamRecordService {

    private final MonthlyExamRepository monthlyExamRepository;
    private final MonthlyExamRecordRepository monthlyExamRecordRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public MonthlyExamRecordResponse create(MonthlyExamRecordCreateRequest request) {
        MonthlyExam exam = getExam(request.monthlyExamId());
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + request.studentId()));

        monthlyExamRecordRepository.findByMonthlyExamIdAndStudentId(exam.getId(), student.getId())
                .ifPresent(r -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "이미 등록된 성적입니다. monthlyExamId=" + exam.getId() + ", studentId=" + student.getId());
                });

        MonthlyExamRecord record = monthlyExamRecordRepository.save(MonthlyExamRecord.builder()
                .monthlyExam(exam)
                .student(student)
                .rawScore(request.rawScore())
                .stdScore(request.stdScore())
                .percentile(request.percentile())
                .grade(request.grade())
                .build());
        return MonthlyExamRecordResponse.from(record);
    }

    @Transactional
    public MonthlyExamRecordResponse update(Long id, MonthlyExamRecordUpdateRequest request) {
        MonthlyExamRecord record = getRecord(id);
        record.update(request.rawScore(), request.stdScore(), request.percentile(), request.grade());
        return MonthlyExamRecordResponse.from(record);
    }

    public List<MonthlyExamRecordResponse> getByClass(Long monthlyExamId, Long classId) {
        MonthlyExam exam = getExam(monthlyExamId);
        if (!schoolClassRepository.existsById(classId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "반을 찾을 수 없습니다. id=" + classId);
        }
        List<Student> students = studentRepository.findBySchoolClassId(classId);

        Map<Long, MonthlyExamRecord> byStudentId = monthlyExamRecordRepository
                .findByMonthlyExamIdAndStudent_SchoolClassId(exam.getId(), classId).stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));

        List<MonthlyExamRecordResponse> responses = new ArrayList<>();
        for (Student student : students) {
            MonthlyExamRecord record = byStudentId.get(student.getId());
            responses.add(record != null
                    ? MonthlyExamRecordResponse.from(record)
                    : MonthlyExamRecordResponse.unrecorded(exam, student));
        }
        return responses;
    }

    public List<MonthlyExamTrendResponse> getStudentTrend(Long studentId, int limit) {
        if (!studentRepository.existsById(studentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + studentId);
        }

        List<MonthlyExamRecord> recent = monthlyExamRecordRepository
                .findRecentByStudentId(studentId, PageRequest.of(0, limit));
        List<MonthlyExamRecord> chronological = new ArrayList<>(recent);
        Collections.reverse(chronological);

        return chronological.stream()
                .map(MonthlyExamTrendResponse::from)
                .toList();
    }

    MonthlyExamRecord getRecord(Long id) {
        return monthlyExamRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "월말모의고사 성적을 찾을 수 없습니다. id=" + id));
    }

    /** 소유권 체크용 — 성적이 어느 학생 소속인지 조회한다. */
    public Long getStudentIdForRecord(Long id) {
        return getRecord(id).getStudent().getId();
    }

    private MonthlyExam getExam(Long id) {
        return monthlyExamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "월말모의고사 회차를 찾을 수 없습니다. id=" + id));
    }
}
