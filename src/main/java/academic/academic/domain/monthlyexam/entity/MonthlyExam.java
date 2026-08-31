package academic.academic.domain.monthlyexam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 월말모의고사 회차 (FR-05-01). 출석/숙제/테스트와 별개의 독립 메뉴로 관리되며, 반과 직접 연결되지 않는다.
 */
@Entity
@Table(name = "monthly_exams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_name", nullable = false, length = 200)
    private String examName;

    @Column(name = "exam_month", nullable = false, length = 7)
    private String examMonth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MonthlyExam(String examName, String examMonth) {
        this.examName = examName;
        this.examMonth = examMonth;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
