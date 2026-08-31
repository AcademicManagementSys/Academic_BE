package academic.academic.domain.monthlyexam.service;

import academic.academic.domain.monthlyexam.dto.MonthlyExamCreateRequest;
import academic.academic.domain.monthlyexam.dto.MonthlyExamResponse;
import academic.academic.domain.monthlyexam.entity.MonthlyExam;
import academic.academic.domain.monthlyexam.repository.MonthlyExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyExamService {

    private final MonthlyExamRepository monthlyExamRepository;

    @Transactional
    public MonthlyExamResponse create(MonthlyExamCreateRequest request) {
        MonthlyExam exam = monthlyExamRepository.save(MonthlyExam.builder()
                .examName(request.examName())
                .examMonth(request.examMonth())
                .build());
        return MonthlyExamResponse.from(exam);
    }

    public List<MonthlyExamResponse> list() {
        return monthlyExamRepository.findAllByOrderByExamMonthDescIdDesc().stream()
                .map(MonthlyExamResponse::from)
                .toList();
    }
}
