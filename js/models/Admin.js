/**
 * Admin Class (OOP Principle: Inheritance & Polymorphism)
 * Extends base User class with administrative privileges and codes.
 */
class Admin extends User {
  constructor(id, name, email, password, adminCode, permissions = [], createdAt) {
    super(id, name, email, password, 'admin', createdAt);
    this.adminCode = adminCode;
    this.permissions = Array.isArray(permissions) && permissions.length > 0 ? permissions : ['admin_access'];
  }

  getAdminCode() {
    return this.adminCode;
  }

  getPermissions() {
    return this.permissions;
  }

  hasPermission(permission) {
    return this.permissions.includes(permission);
  }

  // Polymorphic implementation
  getRoleDetails() {
    return {
      roleTitle: 'System Administrator',
      badgeClass: 'badge-admin',
      attributes: {
        'Admin Code': this.adminCode,
        'Permissions': this.permissions.join(', ')
      }
    };
  }

  toJSON() {
    return {
      ...super.toJSON(),
      adminCode: this.adminCode,
      permissions: this.permissions
    };
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = Admin;
}
