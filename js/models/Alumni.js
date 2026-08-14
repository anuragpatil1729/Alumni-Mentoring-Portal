/**
 * Alumni Class (OOP Principle: Inheritance & Polymorphism)
 * Extends base User class with professional alumni-specific attributes.
 */
class Alumni extends User {
  constructor(id, name, email, password, company, jobTitle, graduationYear, createdAt) {
    super(id, name, email, password, 'alumni', createdAt);
    this.company = company;
    this.jobTitle = jobTitle;
    this.graduationYear = graduationYear;
  }

  getCompany() {
    return this.company;
  }

  getJobTitle() {
    return this.jobTitle;
  }

  getGraduationYear() {
    return this.graduationYear;
  }

  // Polymorphic implementation
  getRoleDetails() {
    return {
      roleTitle: 'Alumni',
      badgeClass: 'badge-alumni',
      attributes: {
        'Company': this.company,
        'Job Title': this.jobTitle,
        'Alumni Class': this.graduationYear
      }
    };
  }

  toJSON() {
    return {
      ...super.toJSON(),
      company: this.company,
      jobTitle: this.jobTitle,
      graduationYear: this.graduationYear
    };
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = Alumni;
}
