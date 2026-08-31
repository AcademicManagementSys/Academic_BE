package academic.academic.domain.monthlyexam.dto;

import academic.academic.domain.monthlyexam.entity.MonthlyExam;

public record MonthlyExamResponse(
        Long id,
        String examName,
        String examMonth
) {
    public static MonthlyExamResponse from(MonthlyExam exam) {
        return new MonthlyExamResponse(exam.getId(), exam.getExamName(), exam.getExamMonth());
    }
}
