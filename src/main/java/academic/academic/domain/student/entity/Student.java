package academic.academic.domain.student.entity;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 학생 본인 로그인 계정 (FR-01-07: 학생 등록 시 함께 발급).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 100)
    private String school;

    @Column(length = 20)
    private String grade;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDate enrolledAt;

    @Builder
    public Student(String name, LocalDate birthDate, String school, String grade, String phone,
                    SchoolClass schoolClass, LocalDate enrolledAt) {
        this.name = name;
        this.birthDate = birthDate;
        this.school = school;
        this.grade = grade;
        this.phone = phone;
        this.schoolClass = schoolClass;
        this.status = StudentStatus.ENROLLED;
        this.enrolledAt = enrolledAt != null ? enrolledAt : LocalDate.now();
    }

    public void update(String name, LocalDate birthDate, String school, String grade, String phone,
                        SchoolClass schoolClass) {
        if (name != null) {
            this.name = name;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (school != null) {
            this.school = school;
        }
        if (grade != null) {
            this.grade = grade;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (schoolClass != null) {
            this.schoolClass = schoolClass;
        }
    }

    public void changeStatus(StudentStatus status) {
        this.status = status;
    }

    public void linkUser(User user) {
        this.user = user;
    }
}
