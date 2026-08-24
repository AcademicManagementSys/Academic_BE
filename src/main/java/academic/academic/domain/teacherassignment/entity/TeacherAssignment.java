package academic.academic.domain.teacherassignment.entity;

import academic.academic.domain.schoolclass.entity.SchoolClass;
import academic.academic.domain.student.entity.Student;
import academic.academic.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teacher_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    private TeacherAssignment(User teacher, SchoolClass schoolClass, Student student) {
        this.teacher = teacher;
        this.schoolClass = schoolClass;
        this.student = student;
    }

    public static TeacherAssignment forClass(User teacher, SchoolClass schoolClass) {
        return new TeacherAssignment(teacher, schoolClass, null);
    }

    public static TeacherAssignment forStudent(User teacher, Student student) {
        return new TeacherAssignment(teacher, null, student);
    }
}
