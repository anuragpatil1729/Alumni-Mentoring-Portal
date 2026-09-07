/**
 * Alumni Mentoring Portal - Vanilla JavaScript Application
 * Manages Login, Student Registration, Alumni Registration, and Mentors Search
 */

(function () {
  "use strict";

  // Application State
  const state = {
    currentUser: null,
    activeView: "login", // "login" | "studentRegister" | "alumniRegister" | "search"
    query: "",
    department: "All",
    industry: "All",
    minExp: "",
    debounceTimer: null,
    selectedMentor: null
  };

  // Avatar Gradients
  const AVATAR_GRADIENTS = [
    "linear-gradient(135deg, #2563eb, #3b82f6)",
    "linear-gradient(135deg, #0d9488, #14b8a6)",
    "linear-gradient(135deg, #7c3aed, #8b5cf6)",
    "linear-gradient(135deg, #d97706, #f59e0b)",
    "linear-gradient(135deg, #e11d48, #f43f5e)",
    "linear-gradient(135deg, #0284c7, #38bdf8)"
  ];

  // DOM Elements Cache
  const els = {};

  function initElements() {
    // Navigation
    els.guestNav = document.getElementById("guestNav");
    els.authNav = document.getElementById("authNav");
    els.navLoginBtn = document.getElementById("navLoginBtn");
    els.navStudentRegBtn = document.getElementById("navStudentRegBtn");
    els.navAlumniRegBtn = document.getElementById("navAlumniRegBtn");
    els.tabSearchBtn = document.getElementById("tabSearchBtn");
    els.logoutBtn = document.getElementById("logoutBtn");
    els.navUserName = document.getElementById("navUserName");
    els.navUserAvatar = document.getElementById("navUserAvatar");
    els.navUserRoleBadge = document.getElementById("navUserRoleBadge");

    // Views
    els.loginView = document.getElementById("loginView");
    els.studentRegisterView = document.getElementById("studentRegisterView");
    els.registerView = document.getElementById("registerView");
    els.searchView = document.getElementById("searchView");

    // Login Form Elements
    els.loginForm = document.getElementById("loginForm");
    els.loginEmail = document.getElementById("loginEmail");
    els.loginPassword = document.getElementById("loginPassword");
    els.loginSubmitBtn = document.getElementById("loginSubmitBtn");
    els.loginStatusAlert = document.getElementById("loginStatusAlert");
    els.fillDemoStudentBtn = document.getElementById("fillDemoStudentBtn");
    els.fillDemoAlumniBtn = document.getElementById("fillDemoAlumniBtn");
    els.linkLoginToStudent = document.getElementById("linkLoginToStudent");
    els.linkLoginToAlumni = document.getElementById("linkLoginToAlumni");

    // Student Registration Form Elements
    els.studentRegForm = document.getElementById("studentRegForm");
    els.studentRegStatusAlert = document.getElementById("studentRegStatusAlert");
    els.studentRegSubmitBtn = document.getElementById("studentRegSubmitBtn");
    els.linkStudentToLogin = document.getElementById("linkStudentToLogin");
    els.linkStudentToAlumni = document.getElementById("linkStudentToAlumni");

    // Alumni Registration Form Elements
    els.alumniRegForm = document.getElementById("alumniRegForm");
    els.regStatusAlert = document.getElementById("regStatusAlert");
    els.registerSubmitBtn = document.getElementById("registerSubmitBtn");
    els.linkAlumniToLogin = document.getElementById("linkAlumniToLogin");
    els.linkAlumniToStudent = document.getElementById("linkAlumniToStudent");

    // Search Controls
    els.searchInput = document.getElementById("searchInput");
    els.clearQueryBtn = document.getElementById("clearQueryBtn");
    els.searchBtn = document.getElementById("searchBtn");

    // Filters
    els.deptFilter = document.getElementById("deptFilter");
    els.industryFilter = document.getElementById("industryFilter");
    els.expFilter = document.getElementById("expFilter");
    els.resetFiltersBtn = document.getElementById("resetFiltersBtn");

    // Metrics Banner
    els.metricTotal = document.getElementById("metricTotal");
    els.metricMatched = document.getElementById("metricMatched");
    els.metricComparisons = document.getElementById("metricComparisons");
    els.metricTime = document.getElementById("metricTime");

    // Results Display
    els.mentorsGrid = document.getElementById("mentorsGrid");
    els.loadingSkeleton = document.getElementById("loadingSkeleton");
    els.emptyState = document.getElementById("emptyState");
    els.errorAlert = document.getElementById("errorAlert");
    els.errorText = document.getElementById("errorText");
    els.emptyActionBtn = document.getElementById("emptyActionBtn");

    // Connect Modal
    els.connectModal = document.getElementById("connectModal");
    els.modalCloseBtn = document.getElementById("modalCloseBtn");
    els.modalCancelBtn = document.getElementById("modalCancelBtn");
    els.connectForm = document.getElementById("connectForm");
    els.summaryName = document.getElementById("summaryName");
    els.summaryRole = document.getElementById("summaryRole");
    els.summaryDept = document.getElementById("summaryDept");
    els.connectSuccess = document.getElementById("connectSuccess");
    els.connectMsg = document.getElementById("connectMsg");
  }

  // =========================================================================
  // Session & Authentication State Management
  // =========================================================================
  function loadSession() {
    try {
      const stored = localStorage.getItem("alumniConnectUser");
      if (stored) {
        state.currentUser = JSON.parse(stored);
      }
    } catch (e) {
      state.currentUser = null;
    }
    updateAuthHeader();
  }

  function saveSession(user) {
    state.currentUser = user;
    try {
      localStorage.setItem("alumniConnectUser", JSON.stringify(user));
    } catch (e) {
      console.error("Could not persist session:", e);
    }
    updateAuthHeader();
  }

  function clearSession() {
    state.currentUser = null;
    try {
      localStorage.removeItem("alumniConnectUser");
    } catch (e) {}
    updateAuthHeader();
  }

  function updateAuthHeader() {
    if (state.currentUser) {
      // User is logged in
      els.guestNav.style.display = "none";
      els.authNav.style.display = "flex";

      const name = state.currentUser.fullName || "User";
      els.navUserName.textContent = name;
      els.navUserAvatar.textContent = name.charAt(0).toUpperCase();

      const role = (state.currentUser.role || "student").toLowerCase();
      els.navUserRoleBadge.textContent = role === "alumni" ? "Mentor" : "Student";
      els.navUserRoleBadge.className = `user-role-badge ${role}`;
    } else {
      // User is guest
      els.guestNav.style.display = "flex";
      els.authNav.style.display = "none";
    }
  }

  // =========================================================================
  // View Routing & Switching
  // =========================================================================
  function switchView(viewName) {
    // If user is not authenticated and tries to open search, redirect to login
    if (!state.currentUser && viewName === "search") {
      viewName = "login";
      setLoginAlert("error", "Please sign in to access the Alumni Mentors Directory.");
    }

    state.activeView = viewName;

    // Hide all views
    els.loginView.classList.remove("active");
    els.studentRegisterView.classList.remove("active");
    els.registerView.classList.remove("active");
    els.searchView.classList.remove("active");

    // Deactivate guest nav buttons
    els.navLoginBtn.classList.remove("active");
    els.navStudentRegBtn.classList.remove("active");
    els.navAlumniRegBtn.classList.remove("active");
    els.tabSearchBtn.classList.remove("active");

    if (viewName === "login") {
      els.loginView.classList.add("active");
      els.navLoginBtn.classList.add("active");
    } else if (viewName === "studentRegister") {
      els.studentRegisterView.classList.add("active");
      els.navStudentRegBtn.classList.add("active");
    } else if (viewName === "alumniRegister") {
      els.registerView.classList.add("active");
      els.navAlumniRegBtn.classList.add("active");
    } else if (viewName === "search") {
      els.searchView.classList.add("active");
      els.tabSearchBtn.classList.add("active");
      performSearch();
    }
  }

  // =========================================================================
  // Login Handling
  // =========================================================================
  async function handleLoginSubmit(e) {
    e.preventDefault();
    setLoginAlert(null);

    const email = els.loginEmail.value.trim();
    const password = els.loginPassword.value;

    let hasError = false;
    if (!email) {
      showFieldError("loginEmail", "Email address is required.");
      hasError = true;
    }
    if (!password) {
      showFieldError("loginPassword", "Password is required.");
      hasError = true;
    }

    if (hasError) return;

    els.loginSubmitBtn.disabled = true;
    els.loginSubmitBtn.textContent = "Signing In...";

    try {
      const res = await ApiClient.login({ email, password });

      if (res.ok && res.data.success && res.data.user) {
        setLoginAlert("success", "Login successful! Redirecting to mentors directory...");
        saveSession(res.data.user);

        setTimeout(() => {
          els.loginForm.reset();
          setLoginAlert(null);
          switchView("search");
        }, 600);

      } else {
        setLoginAlert("error", res.data.message || "Invalid email or password. Please try again.");
      }
    } catch (err) {
      console.error("Login request error:", err);
      setLoginAlert("error", "Cannot reach Java backend on port 5001. Please verify server is running.");
    } finally {
      els.loginSubmitBtn.disabled = false;
      els.loginSubmitBtn.textContent = "Sign In to Portal";
    }
  }

  function setLoginAlert(type, msg) {
    if (!type || !msg) {
      els.loginStatusAlert.className = "form-status-alert";
      els.loginStatusAlert.textContent = "";
      return;
    }
    els.loginStatusAlert.className = `form-status-alert ${type}`;
    els.loginStatusAlert.textContent = msg;
  }

  // =========================================================================
  // Validation & Password Strength Testing Engine
  // =========================================================================
  function evaluatePassword(password) {
    if (!password) {
      return {
        score: 0,
        level: "Enter a password",
        colorClass: "",
        hasLength: false,
        hasUpper: false,
        hasLower: false,
        hasDigit: false,
        hasSpecial: false
      };
    }

    const hasLength = password.length >= 8;
    const hasUpper = /[A-Z]/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasDigit = /\d/.test(password);
    const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);

    let passedCount = (hasLength ? 1 : 0) + (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);

    let score = 1;
    let level = "Weak";
    let colorClass = "weak";

    if (!hasLength) {
      score = 1;
      level = "Too Short";
      colorClass = "weak";
    } else if (passedCount <= 2) {
      score = 1;
      level = "Weak";
      colorClass = "weak";
    } else if (passedCount <= 4) {
      score = 2;
      level = "Fair";
      colorClass = "fair";
    } else {
      score = password.length >= 12 ? 4 : 3;
      level = score === 4 ? "Strong" : "Good";
      colorClass = score === 4 ? "strong" : "good";
    }

    return {
      score,
      level,
      colorClass,
      hasLength,
      hasUpper,
      hasLower,
      hasDigit,
      hasSpecial
    };
  }

  function updatePasswordMeter(prefix, password) {
    const wrap = document.getElementById(`${prefix}_meter_wrap`);
    const label = document.getElementById(`${prefix}_strength_label`);
    if (!wrap || !label) return;

    const res = evaluatePassword(password);
    label.textContent = res.level;
    label.className = `strength-label ${res.colorClass}`;

    const segments = wrap.querySelectorAll(".meter-segment");
    const colors = {
      1: "#ef4444", // Weak (Red)
      2: "#f97316", // Fair (Orange)
      3: "#eab308", // Good (Yellow/Amber)
      4: "#10b981"  // Strong (Green)
    };

    segments.forEach((seg, idx) => {
      if (res.score > 0 && idx < res.score) {
        seg.style.backgroundColor = colors[res.score];
      } else {
        seg.style.backgroundColor = "#e2e8f0";
      }
    });

    const rulePrefix = prefix === "sreg" ? "srule" : "arule";
    const ruleLen = document.getElementById(`${rulePrefix}_len`);
    const ruleUpper = document.getElementById(`${rulePrefix}_upper`);
    const ruleLower = document.getElementById(`${rulePrefix}_lower`);
    const ruleDigit = document.getElementById(`${rulePrefix}_digit`);
    const ruleSpecial = document.getElementById(`${rulePrefix}_special`);

    if (ruleLen) ruleLen.classList.toggle("passed", res.hasLength);
    if (ruleUpper) ruleUpper.classList.toggle("passed", res.hasUpper);
    if (ruleLower) ruleLower.classList.toggle("passed", res.hasLower);
    if (ruleDigit) ruleDigit.classList.toggle("passed", res.hasDigit);
    if (ruleSpecial) ruleSpecial.classList.toggle("passed", res.hasSpecial);
  }

  function checkPasswordMatch(pwd, confirmPwd, matchEl) {
    if (!matchEl) return;
    if (!confirmPwd) {
      matchEl.className = "password-match-msg";
      matchEl.textContent = "";
      return;
    }
    if (pwd === confirmPwd) {
      matchEl.className = "password-match-msg match";
      matchEl.textContent = "✓ Passwords match";
    } else {
      matchEl.className = "password-match-msg mismatch";
      matchEl.textContent = "✕ Passwords do not match";
    }
  }

  function validateFullNameLive(name, errEl) {
    if (!errEl) return;
    if (!name || name.trim().length === 0) {
      errEl.textContent = "";
      errEl.classList.remove("visible");
      return;
    }
    const trimmed = name.trim();
    if (trimmed.length < 2) {
      errEl.textContent = "Full name must be at least 2 characters.";
      errEl.classList.add("visible");
      return;
    }
    if (/[0-9]/.test(trimmed)) {
      errEl.textContent = "Full name cannot contain numbers.";
      errEl.classList.add("visible");
      return;
    }
    if (trimmed.includes("  ")) {
      errEl.textContent = "Full name cannot contain consecutive spaces.";
      errEl.classList.add("visible");
      return;
    }
    if (!/^[a-zA-Z\s.-]+$/.test(trimmed)) {
      errEl.textContent = "Full name can only contain letters, spaces, hyphens, and periods.";
      errEl.classList.add("visible");
      return;
    }
    errEl.textContent = "";
    errEl.classList.remove("visible");
  }

  // =========================================================================
  // Student Registration Handling
  // =========================================================================
  async function handleStudentRegisterSubmit(e) {
    e.preventDefault();
    clearFieldErrors();
    setStudentRegAlert(null);

    const form = els.studentRegForm;
    const fullName = form.fullName.value.trim();
    const email = form.email.value.trim();
    const mobileNumber = form.mobileNumber.value.trim();
    const password = form.password.value;
    const confirmPassword = form.confirmPassword.value;
    const studentId = form.studentId.value.trim();
    const department = form.department.value;
    const graduationYear = parseInt(form.graduationYear.value, 10);

    let clientErrors = {};

    // Validate Full Name
    if (!fullName) {
      clientErrors.s_fullName = "Full name is required.";
    } else if (fullName.length < 2 || fullName.length > 50) {
      clientErrors.s_fullName = "Full name must be between 2 and 50 characters.";
    } else if (/[0-9]/.test(fullName)) {
      clientErrors.s_fullName = "Full name cannot contain numbers.";
    } else if (fullName.includes("  ")) {
      clientErrors.s_fullName = "Full name cannot contain consecutive spaces.";
    } else if (!/^[a-zA-Z\s.-]{2,50}$/.test(fullName)) {
      clientErrors.s_fullName = "Full name can only contain letters, spaces, hyphens, and periods.";
    } else if ((fullName.match(/[a-zA-Z]/g) || []).length < 2) {
      clientErrors.s_fullName = "Full name must contain at least two letters.";
    }

    // Validate Email
    if (!email) {
      clientErrors.s_email = "Email is required.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      clientErrors.s_email = "Please enter a valid email address.";
    }

    // Validate Mobile
    if (!mobileNumber) {
      clientErrors.s_mobileNumber = "Mobile number is required.";
    } else if (!/^[6-9]\d{9}$/.test(mobileNumber)) {
      clientErrors.s_mobileNumber = "Enter a valid 10-digit mobile number starting with 6-9.";
    }

    // Validate Password Strength
    const pwdEval = evaluatePassword(password);
    if (!password) {
      clientErrors.s_password = "Password is required.";
    } else if (!pwdEval.hasLength || !pwdEval.hasUpper || !pwdEval.hasLower || !pwdEval.hasDigit || !pwdEval.hasSpecial) {
      clientErrors.s_password = "Password must be at least 8 chars with uppercase, lowercase, number & special char.";
    }

    // Confirm Password
    if (password !== confirmPassword) {
      clientErrors.s_confirmPassword = "Passwords do not match.";
    }

    // Validate Student ID
    if (!studentId) {
      clientErrors.s_studentId = "Student ID is required.";
    } else if (!/^[a-zA-Z0-9/-]{3,20}$/.test(studentId)) {
      clientErrors.s_studentId = "Student ID must be 3-20 characters alphanumeric (hyphens/slashes allowed).";
    }

    // Department
    if (!department) {
      clientErrors.s_department = "Please select your department / branch.";
    }

    // Graduation Year
    if (!graduationYear || isNaN(graduationYear)) {
      clientErrors.s_graduationYear = "Expected graduation year is required.";
    } else if (graduationYear < 2024 || graduationYear > 2035) {
      clientErrors.s_graduationYear = "Graduation year must be between 2024 and 2035.";
    }

    // Display client-side validation errors if any
    if (Object.keys(clientErrors).length > 0) {
      Object.entries(clientErrors).forEach(([field, msg]) => {
        showFieldError(field, msg);
      });
      setStudentRegAlert("error", "Please correct the highlighted errors before submitting.");
      return;
    }

    els.studentRegSubmitBtn.disabled = true;
    els.studentRegSubmitBtn.textContent = "Creating Student Profile...";

    const payload = {
      fullName,
      email,
      mobileNumber,
      password,
      studentId,
      department,
      graduationYear
    };

    try {
      const res = await ApiClient.registerStudent(payload);

      if (res.ok && res.data.success) {
        setStudentRegAlert("success", "Student account created successfully! Automatically signing you in...");
        form.reset();

        // Automatically log in the user
        const loginRes = await ApiClient.login({ email, password });
        if (loginRes.ok && loginRes.data.success && loginRes.data.user) {
          saveSession(loginRes.data.user);
          setTimeout(() => {
            setStudentRegAlert(null);
            switchView("search");
          }, 900);
        } else {
          setTimeout(() => {
            setStudentRegAlert(null);
            switchView("login");
            setLoginAlert("success", "Account created! Please sign in with your password.");
          }, 1200);
        }

      } else {
        if (res.data.errors) {
          Object.entries(res.data.errors).forEach(([field, msg]) => {
            showFieldError(`s_${field}`, msg);
          });
        }
        setStudentRegAlert("error", res.data.message || "Registration failed. Please verify your details.");
      }
    } catch (err) {
      console.error("Student registration error:", err);
      setStudentRegAlert("error", "Could not connect to Java backend on port 5001.");
    } finally {
      els.studentRegSubmitBtn.disabled = false;
      els.studentRegSubmitBtn.textContent = "Create Student Account";
    }
  }

  function setStudentRegAlert(type, msg) {
    if (!type || !msg) {
      els.studentRegStatusAlert.className = "form-status-alert";
      els.studentRegStatusAlert.textContent = "";
      return;
    }
    els.studentRegStatusAlert.className = `form-status-alert ${type}`;
    els.studentRegStatusAlert.textContent = msg;
  }

  // =========================================================================
  // Alumni Registration Handling
  // =========================================================================
  async function handleAlumniRegisterSubmit(e) {
    e.preventDefault();
    clearFieldErrors();
    setRegStatusAlert(null);

    const form = els.alumniRegForm;
    const name = form.name.value.trim();
    const email = form.email.value.trim();
    const mobileNumber = form.mobileNumber.value.trim();
    const password = form.password.value;
    const confirmPassword = form.confirmPassword.value;
    const department = form.department.value.trim();
    const graduationYear = parseInt(form.graduationYear.value, 10);
    const experience = parseInt(form.experience.value, 10);
    const company = form.company.value.trim();
    const designation = form.designation.value.trim();
    const linkedInProfile = form.linkedInProfile.value.trim();
    const industry = form.industry.value;
    const skills = form.skills.value.trim();
    const bio = form.bio.value.trim();
    const maxMentees = parseInt(form.maxMentees.value, 10);

    let clientErrors = {};

    if (!name) {
      clientErrors.fullName = "Full name is required.";
    } else if (name.length < 2 || name.length > 50) {
      clientErrors.fullName = "Full name must be between 2 and 50 characters.";
    } else if (/[0-9]/.test(name)) {
      clientErrors.fullName = "Full name cannot contain numbers.";
    } else if (name.includes("  ")) {
      clientErrors.fullName = "Full name cannot contain consecutive spaces.";
    } else if (!/^[a-zA-Z\s.-]{2,50}$/.test(name)) {
      clientErrors.fullName = "Full name can only contain letters, spaces, hyphens, and periods.";
    } else if ((name.match(/[a-zA-Z]/g) || []).length < 2) {
      clientErrors.fullName = "Full name must contain at least two letters.";
    }

    if (!email) clientErrors.email = "Email is required.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) clientErrors.email = "Please enter a valid email address.";

    if (!mobileNumber) clientErrors.mobileNumber = "Mobile number is required.";
    else if (!/^[6-9]\d{9}$/.test(mobileNumber)) clientErrors.mobileNumber = "Enter a valid 10-digit mobile number starting with 6-9.";

    const aPwdEval = evaluatePassword(password);
    if (!password) {
      clientErrors.password = "Password is required.";
    } else if (!aPwdEval.hasLength || !aPwdEval.hasUpper || !aPwdEval.hasLower || !aPwdEval.hasDigit || !aPwdEval.hasSpecial) {
      clientErrors.password = "Password must be at least 8 chars with uppercase, lowercase, number & special char.";
    }

    if (password !== confirmPassword) clientErrors.confirmPassword = "Passwords do not match.";
    if (!department) clientErrors.department = "Department is required.";

    const curYear = new Date().getFullYear();
    if (!graduationYear || isNaN(graduationYear)) clientErrors.graduationYear = "Graduation year is required.";
    else if (graduationYear < 1950 || graduationYear > curYear) clientErrors.graduationYear = `Graduation year must be between 1950 and ${curYear}.`;

    if (!company) clientErrors.company = "Current company is required.";
    if (!designation) clientErrors.designation = "Job designation is required.";

    if (linkedInProfile && !/^https?:\/\/(www\.)?linkedin\.com\/.*$/i.test(linkedInProfile)) {
      clientErrors.linkedInProfile = "Please enter a valid LinkedIn URL (e.g. https://linkedin.com/in/...).";
    }

    if (Object.keys(clientErrors).length > 0) {
      Object.entries(clientErrors).forEach(([field, msg]) => {
        showFieldError(field, msg);
      });
      setRegStatusAlert("error", "Please fix the highlighted errors before submitting.");
      return;
    }

    els.registerSubmitBtn.disabled = true;
    els.registerSubmitBtn.textContent = "Creating Alumni Profile...";

    const payload = {
      fullName: name,
      email,
      mobileNumber,
      password,
      department,
      graduationYear,
      experienceYears: isNaN(experience) ? null : experience,
      company,
      designation,
      linkedInProfile: linkedInProfile || null,
      industry,
      skills,
      bio,
      maxMentees: isNaN(maxMentees) ? null : maxMentees
    };

    try {
      const res = await ApiClient.registerAlumni(payload);

      if (res.ok && res.data.success) {
        setRegStatusAlert("success", "Alumni account created successfully! Automatically signing you in...");
        form.reset();

        const loginRes = await ApiClient.login({ email, password });
        if (loginRes.ok && loginRes.data.success && loginRes.data.user) {
          saveSession(loginRes.data.user);
          setTimeout(() => {
            setRegStatusAlert(null);
            switchView("search");
          }, 900);
        } else {
          setTimeout(() => {
            setRegStatusAlert(null);
            switchView("login");
            setLoginAlert("success", "Alumni registered! Please sign in with your credentials.");
          }, 1200);
        }

      } else {
        if (res.data.errors) {
          Object.entries(res.data.errors).forEach(([field, msg]) => {
            showFieldError(field, msg);
          });
        }
        setRegStatusAlert("error", res.data.message || "Registration failed. Please check the inputs.");
      }
    } catch (err) {
      console.error("Alumni registration error:", err);
      setRegStatusAlert("error", "Could not connect to Java backend server at http://localhost:5001");
    } finally {
      els.registerSubmitBtn.disabled = false;
      els.registerSubmitBtn.textContent = "Create Alumni Account";
    }
  }

  function setRegStatusAlert(type, text) {
    if (!type || !text) {
      els.regStatusAlert.className = "form-status-alert";
      els.regStatusAlert.textContent = "";
      return;
    }
    els.regStatusAlert.className = `form-status-alert ${type}`;
    els.regStatusAlert.textContent = text;
  }

  function showFieldError(field, msg) {
    const el = document.getElementById(`err_${field}`);
    if (el) {
      el.textContent = msg;
      el.classList.add("visible");
    }
  }

  function clearFieldErrors() {
    document.querySelectorAll(".field-error-msg").forEach((el) => {
      el.textContent = "";
      el.classList.remove("visible");
    });
  }

  // =========================================================================
  // Search Execution & Mentors Directory
  // =========================================================================
  async function performSearch() {
    setLoading(true);
    hideError();

    try {
      // Behind the scenes, we use the fast manual search algorithm
      const data = await ApiClient.searchMentors({
        query: state.query,
        algorithm: "linear",
        department: state.department,
        industry: state.industry,
        minExperience: state.minExp
      });

      renderMetrics(data.metrics);
      renderMentors(data.data || []);

    } catch (err) {
      console.error("Search execution error:", err);
      showError("Unable to connect to Java backend on port 5001. Ensure Java server is running.");
      renderMentors([]);
    } finally {
      setLoading(false);
      updateResetButtonVisibility();
    }
  }

  function renderMetrics(metrics) {
    if (!metrics) return;
    els.metricTotal.textContent = `${metrics.totalRecords || 0} mentors`;
    els.metricMatched.textContent = metrics.resultsCount || 0;
    els.metricComparisons.textContent = `${metrics.comparisons || 0} operations`;
    els.metricTime.textContent = `${metrics.executionTimeMs || 0} ms`;
  }

  function renderMentors(mentors) {
    els.mentorsGrid.innerHTML = "";

    if (!mentors || mentors.length === 0) {
      els.emptyState.classList.add("visible");
      return;
    }

    els.emptyState.classList.remove("visible");

    mentors.forEach((mentor) => {
      const cardEl = createMentorCard(mentor);
      els.mentorsGrid.appendChild(cardEl);
    });
  }

  function createMentorCard(mentor) {
    const card = document.createElement("div");
    card.className = "mentor-card";

    const fullName = mentor.fullName || "Alumni Mentor";
    const designation = mentor.designation || "Professional";
    const company = mentor.company || "Company";
    const department = mentor.department || "Engineering";
    const gradYear = mentor.graduationYear;
    const exp = mentor.experienceYears != null ? mentor.experienceYears : mentor.experience_years;
    const industry = mentor.industry;
    const bio = mentor.bio || "";
    const linkedIn = mentor.linkedInProfile || mentor.linkedin_profile;
    const skillsStr = mentor.skills || "";

    // Initials
    const initials = fullName
      .split(" ")
      .map((n) => n[0])
      .slice(0, 2)
      .join("")
      .toUpperCase();

    // Deterministic gradient
    const charSum = fullName.split("").reduce((acc, c) => acc + c.charCodeAt(0), 0);
    const bgGradient = AVATAR_GRADIENTS[charSum % AVATAR_GRADIENTS.length];

    // Card Top
    const cardTop = document.createElement("div");
    cardTop.className = "card-top";
    cardTop.innerHTML = `
      <div class="avatar" style="background: ${bgGradient}">${initials}</div>
      <div class="card-title-group">
        <h3 class="mentor-name" title="${escapeHtml(fullName)}">${escapeHtml(fullName)}</h3>
        <div class="mentor-role" title="${escapeHtml(designation)}">${escapeHtml(designation)}</div>
        <div class="company-badge"><span class="building-icon">🏢</span> ${escapeHtml(company)}</div>
      </div>
    `;
    card.appendChild(cardTop);

    // Badges
    const badges = document.createElement("div");
    badges.className = "card-badges";
    let badgesHtml = `<span class="badge badge-dept" title="Department">🎓 ${escapeHtml(department)}</span>`;
    if (gradYear) {
      badgesHtml += `<span class="badge badge-year" title="Graduation Year">Class of '${String(gradYear).slice(-2)}</span>`;
    }
    if (exp !== null && exp !== undefined) {
      badgesHtml += `<span class="badge badge-exp" title="Experience">⏱ ${exp} yrs exp</span>`;
    }
    if (industry) {
      badgesHtml += `<span class="badge badge-industry" title="Industry">🌐 ${escapeHtml(industry)}</span>`;
    }
    badges.innerHTML = badgesHtml;
    card.appendChild(badges);

    // Bio
    if (bio) {
      const bioEl = document.createElement("p");
      bioEl.className = "mentor-bio";
      bioEl.title = bio;
      bioEl.textContent = bio.length > 120 ? `${bio.substring(0, 117)}...` : bio;
      card.appendChild(bioEl);
    }

    // Skills
    const skillsList = skillsStr.split(",").map((s) => s.trim()).filter(Boolean);
    if (skillsList.length > 0) {
      const skillsSection = document.createElement("div");
      skillsSection.className = "skills-section";
      skillsSection.innerHTML = `<div class="skills-label">Skills & Expertise:</div>`;

      const chipsContainer = document.createElement("div");
      chipsContainer.className = "skills-chips";

      const displaySkills = skillsList.slice(0, 4);
      displaySkills.forEach((skill) => {
        const chip = document.createElement("button");
        chip.type = "button";
        chip.className = "skill-chip";
        chip.textContent = skill;
        chip.addEventListener("click", () => {
          state.query = skill;
          els.searchInput.value = skill;
          updateClearButton();
          performSearch();
        });
        chipsContainer.appendChild(chip);
      });

      if (skillsList.length > 4) {
        const moreSpan = document.createElement("span");
        moreSpan.className = "skill-chip-more";
        moreSpan.textContent = `+${skillsList.length - 4} more`;
        chipsContainer.appendChild(moreSpan);
      }

      skillsSection.appendChild(chipsContainer);
      card.appendChild(skillsSection);
    }

    // Card Action Buttons
    const actions = document.createElement("div");
    actions.className = "card-actions";

    if (linkedIn) {
      const linkedInBtn = document.createElement("a");
      linkedInBtn.href = linkedIn.startsWith("http") ? linkedIn : `https://${linkedIn}`;
      linkedInBtn.target = "_blank";
      linkedInBtn.rel = "noopener noreferrer";
      linkedInBtn.className = "linkedin-link-btn";
      linkedInBtn.textContent = "LinkedIn";
      actions.appendChild(linkedInBtn);
    }

    const connectBtn = document.createElement("button");
    connectBtn.type = "button";
    connectBtn.className = "connect-btn";
    connectBtn.textContent = "Request Session";
    connectBtn.addEventListener("click", () => openConnectModal(mentor));
    actions.appendChild(connectBtn);

    card.appendChild(actions);
    return card;
  }

  // =========================================================================
  // Connect Request Modal
  // =========================================================================
  function openConnectModal(mentor) {
    if (!state.currentUser) {
      switchView("login");
      setLoginAlert("error", "Please sign in to request a mentorship session.");
      return;
    }

    state.selectedMentor = mentor;
    els.summaryName.textContent = mentor.fullName || "Alumni Mentor";
    els.summaryRole.textContent = `${mentor.designation || "Professional"} at ${mentor.company || "Company"}`;
    els.summaryDept.textContent = `${mentor.department || "Engineering"} · ${mentor.experienceYears || 0} yrs experience`;

    els.connectSuccess.classList.remove("visible");
    els.connectSuccess.textContent = "";
    els.connectMsg.value = "";
    els.connectForm.style.display = "block";

    els.connectModal.classList.add("active");
  }

  function closeConnectModal() {
    els.connectModal.classList.remove("active");
    state.selectedMentor = null;
  }

  function handleConnectSubmit(e) {
    e.preventDefault();
    const sessionGoal = document.getElementById("sessionGoal").value;
    const connectMsg = els.connectMsg.value.trim();

    if (!connectMsg) return;

    els.connectForm.style.display = "none";
    const mentorName = state.selectedMentor ? state.selectedMentor.fullName : "your mentor";
    const studentName = state.currentUser ? state.currentUser.fullName : "Student";

    els.connectSuccess.innerHTML = `
      ✅ Mentorship request sent successfully!
      <br/><br/>
      <small><strong>${escapeHtml(mentorName)}</strong> will review your request from <strong>${escapeHtml(studentName)}</strong> and respond at your registered email (${escapeHtml(state.currentUser.email)}).</small>
    `;
    els.connectSuccess.classList.add("visible");

    setTimeout(() => {
      closeConnectModal();
    }, 2800);
  }

  // =========================================================================
  // UI Helpers
  // =========================================================================
  function setLoading(isLoading) {
    if (isLoading) {
      els.loadingSkeleton.style.display = "grid";
      els.mentorsGrid.style.display = "none";
      els.emptyState.classList.remove("visible");
    } else {
      els.loadingSkeleton.style.display = "none";
      els.mentorsGrid.style.display = "grid";
    }
  }

  function showError(msg) {
    els.errorText.textContent = msg;
    els.errorAlert.classList.add("visible");
  }

  function hideError() {
    els.errorAlert.classList.remove("visible");
  }

  function updateClearButton() {
    if (els.searchInput.value.trim().length > 0) {
      els.clearQueryBtn.classList.add("visible");
    } else {
      els.clearQueryBtn.classList.remove("visible");
    }
  }

  function updateResetButtonVisibility() {
    const hasFilters = state.department !== "All" || state.industry !== "All" || state.minExp !== "" || state.query.trim().length > 0;
    els.resetFiltersBtn.style.visibility = hasFilters ? "visible" : "hidden";
  }

  function resetAllFilters() {
    state.query = "";
    state.department = "All";
    state.industry = "All";
    state.minExp = "";

    els.searchInput.value = "";
    els.deptFilter.value = "All";
    els.industryFilter.value = "All";
    els.expFilter.value = "";

    updateClearButton();
    performSearch();
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  // =========================================================================
  // Event Bindings
  // =========================================================================
  function bindEvents() {
    // Navigation Tabs
    els.navLoginBtn.addEventListener("click", () => switchView("login"));
    els.navStudentRegBtn.addEventListener("click", () => switchView("studentRegister"));
    els.navAlumniRegBtn.addEventListener("click", () => switchView("alumniRegister"));
    els.tabSearchBtn.addEventListener("click", () => switchView("search"));

    // Logout
    els.logoutBtn.addEventListener("click", () => {
      clearSession();
      switchView("login");
      setLoginAlert("success", "You have been signed out successfully.");
    });

    // Switch View Links
    els.linkLoginToStudent.addEventListener("click", (e) => {
      e.preventDefault();
      switchView("studentRegister");
    });
    els.linkLoginToAlumni.addEventListener("click", (e) => {
      e.preventDefault();
      switchView("alumniRegister");
    });
    els.linkStudentToLogin.addEventListener("click", (e) => {
      e.preventDefault();
      switchView("login");
    });
    els.linkStudentToAlumni.addEventListener("click", (e) => {
      e.preventDefault();
      switchView("alumniRegister");
    });
    els.linkAlumniToLogin.addEventListener("click", (e) => {
      e.preventDefault();
      switchView("login");
    });
    els.linkAlumniToStudent.addEventListener("click", (e) => {
      e.preventDefault();
      switchView("studentRegister");
    });

    // Quick Demo Credentials Buttons
    els.fillDemoStudentBtn.addEventListener("click", () => {
      els.loginEmail.value = "rahul.student@college.edu";
      els.loginPassword.value = "Student@123";
      clearFieldErrors();
    });
    els.fillDemoAlumniBtn.addEventListener("click", () => {
      els.loginEmail.value = "anurag.patil@microsoft.com";
      els.loginPassword.value = "Alumni@123";
      clearFieldErrors();
    });

    // Forms
    els.loginForm.addEventListener("submit", handleLoginSubmit);
    els.studentRegForm.addEventListener("submit", handleStudentRegisterSubmit);
    els.alumniRegForm.addEventListener("submit", handleAlumniRegisterSubmit);

    // Live Password Strength Testing & Name Validation for Student Form
    const sregPwdInput = document.getElementById("sreg_password");
    const sregConfirmInput = document.getElementById("sreg_confirmPassword");
    const sregMatchMsg = document.getElementById("sreg_match_msg");
    const sregNameInput = document.getElementById("sreg_name");
    const sregNameErr = document.getElementById("err_s_fullName");

    if (sregPwdInput) {
      sregPwdInput.addEventListener("input", (e) => {
        updatePasswordMeter("sreg", e.target.value);
        if (sregConfirmInput && sregConfirmInput.value) {
          checkPasswordMatch(e.target.value, sregConfirmInput.value, sregMatchMsg);
        }
      });
    }
    if (sregConfirmInput) {
      sregConfirmInput.addEventListener("input", (e) => {
        const pwdVal = sregPwdInput ? sregPwdInput.value : "";
        checkPasswordMatch(pwdVal, e.target.value, sregMatchMsg);
      });
    }
    if (sregNameInput) {
      sregNameInput.addEventListener("input", (e) => {
        validateFullNameLive(e.target.value, sregNameErr);
      });
    }

    // Live Password Strength Testing & Name Validation for Alumni Form
    const regPwdInput = document.getElementById("reg_password");
    const regConfirmInput = document.getElementById("reg_confirmPassword");
    const regMatchMsg = document.getElementById("reg_match_msg");
    const regNameInput = document.getElementById("reg_name");
    const regNameErr = document.getElementById("err_fullName");

    if (regPwdInput) {
      regPwdInput.addEventListener("input", (e) => {
        updatePasswordMeter("reg", e.target.value);
        if (regConfirmInput && regConfirmInput.value) {
          checkPasswordMatch(e.target.value, regConfirmInput.value, regMatchMsg);
        }
      });
    }
    if (regConfirmInput) {
      regConfirmInput.addEventListener("input", (e) => {
        const pwdVal = regPwdInput ? regPwdInput.value : "";
        checkPasswordMatch(pwdVal, e.target.value, regMatchMsg);
      });
    }
    if (regNameInput) {
      regNameInput.addEventListener("input", (e) => {
        validateFullNameLive(e.target.value, regNameErr);
      });
    }

    // Form input clear error listeners
    document.querySelectorAll("input, select, textarea").forEach((input) => {
      input.addEventListener("input", () => {
        const errEl = document.getElementById(`err_${input.name}`) || document.getElementById(`err_s_${input.name}`);
        if (errEl && input.name !== "fullName" && input.name !== "name") {
          errEl.classList.remove("visible");
        }
      });
    });

    // Search Controls
    els.searchInput.addEventListener("input", (e) => {
      state.query = e.target.value;
      updateClearButton();
      clearTimeout(state.debounceTimer);
      state.debounceTimer = setTimeout(() => {
        performSearch();
      }, 280);
    });

    els.searchInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        clearTimeout(state.debounceTimer);
        performSearch();
      }
    });

    els.clearQueryBtn.addEventListener("click", () => {
      state.query = "";
      els.searchInput.value = "";
      updateClearButton();
      performSearch();
    });

    els.searchBtn.addEventListener("click", () => {
      clearTimeout(state.debounceTimer);
      performSearch();
    });

    // Filter selects
    els.deptFilter.addEventListener("change", (e) => {
      state.department = e.target.value;
      performSearch();
    });

    els.industryFilter.addEventListener("change", (e) => {
      state.industry = e.target.value;
      performSearch();
    });

    els.expFilter.addEventListener("change", (e) => {
      state.minExp = e.target.value;
      performSearch();
    });

    els.resetFiltersBtn.addEventListener("click", resetAllFilters);
    els.emptyActionBtn.addEventListener("click", resetAllFilters);

    // Connect Modal
    els.modalCloseBtn.addEventListener("click", closeConnectModal);
    els.modalCancelBtn.addEventListener("click", closeConnectModal);
    els.connectModal.addEventListener("click", (e) => {
      if (e.target === els.connectModal) closeConnectModal();
    });
    els.connectForm.addEventListener("submit", handleConnectSubmit);
  }

  // =========================================================================
  // Initialization
  // =========================================================================
  document.addEventListener("DOMContentLoaded", () => {
    initElements();
    bindEvents();
    loadSession();

    // Check if user is already logged in
    if (state.currentUser) {
      switchView("search");
    } else {
      switchView("login");
    }
  });
})();
