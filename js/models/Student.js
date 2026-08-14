/**
 * Student Class (OOP Principle: Inheritance & Polymorphism)
 * Extends base User class with student-specific attributes and behaviors.
 */
class Student extends User {
  constructor(id, name, email, password, studentId, major, graduationYear, createdAt) {
    super(id, name, email, password, 'student', createdAt);
    this.studentId = studentId;
    this.major = major;
    this.graduationYear = graduationYear;
  }

  getStudentId() {
    return this.studentId;
  }

  getMajor() {
    return this.major;
  }

  getGraduationYear() {
    return this.graduationYear;
  }

  // Polymorphic implementation
  getRoleDetails() {
    return {
      roleTitle: 'Student',
      badgeClass: 'badge-student',
      attributes: {
        'Student ID': this.studentId,
        'Major / Field': this.major,
        'Graduation Year': this.graduationYear
      }
    };
  }

  toJSON() {
    return {
      ...super.toJSON(),
      studentId: this.studentId,
      major: this.major,
      graduationYear: this.graduationYear
    };
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = Student;
}
