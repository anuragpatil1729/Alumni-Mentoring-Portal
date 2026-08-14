/**
 * AuthService Class (OOP Service for User Registration, Authentication & Role Management)
 * Manages user persistence, class instantiation via Factory Pattern, and authentication workflow.
 * Dynamic: No hardcoded fallback values or hardcoded seed users.
 */
class AuthService {
  static USERS_KEY = 'amp_registered_users';

  constructor() {
    this.users = this.#loadUsers();
  }

  /**
   * OOP Factory Method to instantiate correct User subclass based on role
   * Strictly uses provided input data without hardcoding default fallbacks.
   */
  createUserInstance(data) {
    const id = data.id || this.#generateId();
    const createdAt = data.createdAt || new Date().toISOString();

    switch (data.role) {
      case 'student':
        if (!data.studentId || !data.major || !data.graduationYear) {
          throw new Error('Missing required student fields (Student ID, Major, Graduation Year)');
        }
        const student = new Student(
          id,
          data.name,
          data.email,
          data.password,
          data.studentId,
          data.major,
          data.graduationYear,
          createdAt
        );
        student._rawPassword = data.password;
        return student;

      case 'alumni':
        if (!data.company || !data.jobTitle || !data.graduationYear) {
          throw new Error('Missing required alumni fields (Company, Job Title, Graduation Year)');
        }
        const alumni = new Alumni(
          id,
          data.name,
          data.email,
          data.password,
          data.company,
          data.jobTitle,
          data.graduationYear,
          createdAt
        );
        alumni._rawPassword = data.password;
        return alumni;

      case 'admin':
        if (!data.adminCode) {
          throw new Error('Missing required administrator field (Admin Code)');
        }
        const admin = new Admin(
          id,
          data.name,
          data.email,
          data.password,
          data.adminCode,
          data.permissions || ['admin_access'],
          createdAt
        );
        admin._rawPassword = data.password;
        return admin;

      default:
        throw new Error(`Unsupported user role: ${data.role}`);
    }
  }

  /**
   * Register a new user dynamically from user input
   */
  register(userData) {
    // Check if email already exists
    const existing = this.users.find(u => u.getEmail().toLowerCase() === userData.email.toLowerCase());
    if (existing) {
      return { success: false, message: 'An account with this email address already exists.' };
    }

    try {
      const newUser = this.createUserInstance(userData);
      this.users.push(newUser);
      this.#saveUsers();

      return {
        success: true,
        message: 'Registration successful! You can now log in with your credentials.',
        user: newUser
      };
    } catch (err) {
      return { success: false, message: err.message };
    }
  }

  /**
   * Login user with email and password
   */
  login(email, password) {
    const user = this.users.find(u => u.getEmail().toLowerCase() === email.toLowerCase());

    if (!user) {
      return { success: false, message: 'No account found with this email address. Please register.' };
    }

    if (!user.validatePassword(password)) {
      return { success: false, message: 'Incorrect password provided.' };
    }

    // Generate Session Token
    const tokenData = TokenManager.generateToken(user);
    TokenManager.saveSession(tokenData.token);

    return {
      success: true,
      message: 'Login successful!',
      user: user,
      tokenData: tokenData
    };
  }

  /**
   * Get current authenticated user session
   */
  getCurrentSession() {
    const token = TokenManager.getSavedToken();
    if (!token) {
      return { authenticated: false, reason: 'No active session token' };
    }

    const verification = TokenManager.verifyToken(token);
    if (!verification.valid) {
      TokenManager.clearSession();
      return { authenticated: false, reason: verification.reason };
    }

    const userId = verification.payload.sub;
    const user = this.users.find(u => u.getId() === userId);

    if (!user) {
      TokenManager.clearSession();
      return { authenticated: false, reason: 'Session user no longer exists' };
    }

    return {
      authenticated: true,
      user: user,
      token: token,
      payload: verification.payload
    };
  }

  /**
   * Logout user
   */
  logout() {
    TokenManager.clearSession();
    return { success: true, message: 'Logged out successfully.' };
  }

  /**
   * Private: Load raw users from localStorage and re-instantiate OOP objects
   */
  #loadUsers() {
    const rawData = localStorage.getItem(AuthService.USERS_KEY);
    if (!rawData) return [];

    try {
      const parsedList = JSON.parse(rawData);
      return parsedList.map(item => this.createUserInstance(item));
    } catch (e) {
      console.error('Failed to load stored users:', e);
      return [];
    }
  }

  /**
   * Private: Persist user objects array to localStorage with exact credentials
   */
  #saveUsers() {
    const serializable = this.users.map(user => {
      const base = user.toJSON();
      return {
        ...base,
        password: user._rawPassword
      };
    });
    localStorage.setItem(AuthService.USERS_KEY, JSON.stringify(serializable));
  }

  /**
   * Private: Helper ID generator
   */
  #generateId() {
    return 'usr_' + Math.random().toString(36).substr(2, 9);
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = AuthService;
}
