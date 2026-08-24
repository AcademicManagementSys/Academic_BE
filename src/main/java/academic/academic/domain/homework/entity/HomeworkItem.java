package academic.academic.domain.homework.entity;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 숙제 항목 (FR-03-01, FR-03-02). 반 단위 또는 개별 학생 단위 중 하나로 생성된다.
 */
@Entity
@Table(name = "homework_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeworkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String scope;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public HomeworkItem(SchoolClass schoolClass, Student student, String title, String scope,
                         LocalDate assignedDate, LocalDate dueDate) {
        this.schoolClass = schoolClass;
        this.student = student;
        this.title = title;
        this.scope = scope;
        this.assignedDate = assignedDate;
        this.dueDate = dueDate;
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

    public void update(String title, String scope, LocalDate assignedDate, LocalDate dueDate) {
        if (title != null) {
            this.title = title;
        }
        if (scope != null) {
            this.scope = scope;
        }
        if (assignedDate != null) {
            this.assignedDate = assignedDate;
        }
        this.dueDate = dueDate;
    }
}
