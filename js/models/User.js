/**
 * Base User Class (OOP Principle: Encapsulation & Polymorphism Base)
 */
class User {
  #password; // Private field for security encapsulation

  constructor(id, name, email, password, role, createdAt = new Date().toISOString()) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.role = role;
    this.createdAt = createdAt;
    this.#password = password;
  }

  // Getters
  getId() {
    return this.id;
  }

  getName() {
    return this.name;
  }

  getEmail() {
    return this.email;
  }

  getRole() {
    return this.role;
  }

  getCreatedAt() {
    return this.createdAt;
  }

  // Authenticate password
  validatePassword(inputPassword) {
    return this.#password === inputPassword;
  }

  // Polymorphic method to be overridden by subclasses
  getRoleDetails() {
    return {
      roleTitle: 'Standard User',
      badgeClass: 'badge-user',
      attributes: {}
    };
  }

  // JSON summary excluding private password
  toJSON() {
    return {
      id: this.id,
      name: this.name,
      email: this.email,
      role: this.role,
      createdAt: this.createdAt,
      roleDetails: this.getRoleDetails()
    };
  }
}

// Export for browser script compatibility
if (typeof module !== 'undefined' && module.exports) {
  module.exports = User;
}
