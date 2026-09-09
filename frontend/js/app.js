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
    selectedMentor: null,
    profileData: null,
    isEditingProfile: false,
    studentRequests: [],
    pendingAvatars: {
      student: null,
      alumni: null
    }
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

  // Helper to ensure relative avatar paths point to backend server
  function resolveAvatarUrl(url) {
    if (!url) return "";
    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
      return url;
    }
    const base = (typeof API_BASE_URL !== "undefined" && API_BASE_URL) ? API_BASE_URL : "http://localhost:5001";
    return `${base}${url.startsWith("/") ? "" : "/"}${url}`;
  }

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
    els.tabProfileBtn = document.getElementById("tabProfileBtn");
    els.navUserSessionPill = document.getElementById("navUserSessionPill");
    els.logoutBtn = document.getElementById("logoutBtn");
    els.navUserName = document.getElementById("navUserName");
    els.navUserAvatar = document.getElementById("navUserAvatar");
    els.navUserRoleBadge = document.getElementById("navUserRoleBadge");

    // Views
    els.loginView = document.getElementById("loginView");
    els.studentRegisterView = document.getElementById("studentRegisterView");
    els.registerView = document.getElementById("registerView");
    els.searchView = document.getElementById("searchView");
    els.profileView = document.getElementById("profileView");

    // Profile Header Elements
    els.profileAvatarLg = document.getElementById("profileAvatarLg");
    els.avatarFileInput = document.getElementById("avatarFileInput");
    els.removeAvatarBtn = document.getElementById("removeAvatarBtn");
    els.profileHeaderName = document.getElementById("profileHeaderName");

    // Registration & Edit Form Avatar Pickers
    els.sreg_avatar_preview = document.getElementById("sreg_avatar_preview");
    els.sreg_avatar_input = document.getElementById("sreg_avatar_input");
    els.sreg_avatar_remove = document.getElementById("sreg_avatar_remove");

    els.reg_avatar_preview = document.getElementById("reg_avatar_preview");
    els.reg_avatar_input = document.getElementById("reg_avatar_input");
    els.reg_avatar_remove = document.getElementById("reg_avatar_remove");

    els.pe_avatar_preview = document.getElementById("pe_avatar_preview");
    els.pe_avatar_input = document.getElementById("pe_avatar_input");
    els.pe_avatar_remove = document.getElementById("pe_avatar_remove");
    els.profileHeaderBadge = document.getElementById("profileHeaderBadge");
    els.profileHeaderEmail = document.getElementById("profileHeaderEmail");
    els.profileMemberSince = document.getElementById("profileMemberSince");
    els.editProfileToggleBtn = document.getElementById("editProfileToggleBtn");
    els.profileStatusAlert = document.getElementById("profileStatusAlert");

    // Profile View Mode
    els.profileViewMode = document.getElementById("profileViewMode");
    els.pv_fullName = document.getElementById("pv_fullName");
    els.pv_email = document.getElementById("pv_email");
    els.pv_mobile = document.getElementById("pv_mobile");
    els.pv_studentIdRow = document.getElementById("pv_studentIdRow");
    els.pv_studentId = document.getElementById("pv_studentId");
    els.pv_department = document.getElementById("pv_department");
    els.pv_graduationYear = document.getElementById("pv_graduationYear");
    els.pv_academicTitle = document.getElementById("pv_academicTitle");
    els.pv_professionalCard = document.getElementById("pv_professionalCard");
    els.pv_company = document.getElementById("pv_company");
    els.pv_designation = document.getElementById("pv_designation");
    els.pv_industry = document.getElementById("pv_industry");
    els.pv_experience = document.getElementById("pv_experience");
    els.pv_linkedin = document.getElementById("pv_linkedin");
    els.pv_mentorshipCard = document.getElementById("pv_mentorshipCard");
    els.pv_skillsTags = document.getElementById("pv_skillsTags");
    els.pv_maxMentees = document.getElementById("pv_maxMentees");
    els.pv_bio = document.getElementById("pv_bio");

    // Profile Edit Mode
    els.profileEditMode = document.getElementById("profileEditMode");
    els.profileEditForm = document.getElementById("profileEditForm");
    els.pe_fullName = document.getElementById("pe_fullName");
    els.pe_email = document.getElementById("pe_email");
    els.pe_mobileNumber = document.getElementById("pe_mobileNumber");
    els.pe_studentFields = document.getElementById("pe_studentFields");
    els.pe_studentId = document.getElementById("pe_studentId");
    els.pe_s_department = document.getElementById("pe_s_department");
    els.pe_s_graduationYear = document.getElementById("pe_s_graduationYear");
    els.pe_alumniFields = document.getElementById("pe_alumniFields");
    els.pe_a_department = document.getElementById("pe_a_department");
    els.pe_a_graduationYear = document.getElementById("pe_a_graduationYear");
    els.pe_company = document.getElementById("pe_company");
    els.pe_designation = document.getElementById("pe_designation");
    els.pe_experience = document.getElementById("pe_experience");
    els.pe_industry = document.getElementById("pe_industry");
    els.pe_linkedin = document.getElementById("pe_linkedin");
    els.pe_skills = document.getElementById("pe_skills");
    els.pe_bio = document.getElementById("pe_bio");
    els.pe_maxMentees = document.getElementById("pe_maxMentees");
    els.cancelProfileEditBtn = document.getElementById("cancelProfileEditBtn");
    els.saveProfileBtn = document.getElementById("saveProfileBtn");

    // Login Form Elements
    els.loginForm = document.getElementById("loginForm");
    els.loginEmail = document.getElementById("loginEmail");
    els.loginPassword = document.getElementById("loginPassword");
    els.loginSubmitBtn = document.getElementById("loginSubmitBtn");
    els.loginStatusAlert = document.getElementById("loginStatusAlert");
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
    els.connectAlert = document.getElementById("connectAlert");
    els.connectSuccess = document.getElementById("connectSuccess");
    els.connectPreviousStatus = document.getElementById("connectPreviousStatus");
    els.connectMsg = document.getElementById("connectMsg");
    els.connectMsgLen = document.getElementById("connectMsgLen");
    els.modalSubmitBtn = document.getElementById("modalSubmitBtn");

    // Mentorship Requests View & Dashboard
    els.tabRequestsBtn = document.getElementById("tabRequestsBtn");
    els.navRequestsBadge = document.getElementById("navRequestsBadge");
    els.requestsView = document.getElementById("requestsView");
    els.requestsTitle = document.getElementById("requestsTitle");
    els.requestsSubtitle = document.getElementById("requestsSubtitle");
    els.refreshRequestsBtn = document.getElementById("refreshRequestsBtn");
    els.requestsAlert = document.getElementById("requestsAlert");
    els.requestsStatsBar = document.getElementById("requestsStatsBar");
    els.statTotalRequests = document.getElementById("statTotalRequests");
    els.statPendingRequests = document.getElementById("statPendingRequests");
    els.statAcceptedRequests = document.getElementById("statAcceptedRequests");
    els.statRejectedRequests = document.getElementById("statRejectedRequests");
    els.requestsList = document.getElementById("requestsList");
    els.requestsEmptyState = document.getElementById("requestsEmptyState");
    els.emptyRequestsTitle = document.getElementById("emptyRequestsTitle");
    els.emptyRequestsMsg = document.getElementById("emptyRequestsMsg");

    // Decision Modal Elements
    els.decisionModal = document.getElementById("decisionModal");
    els.decisionModalTitle = document.getElementById("decisionModalTitle");
    els.decisionModalCloseBtn = document.getElementById("decisionModalCloseBtn");
    els.decisionStudentName = document.getElementById("decisionStudentName");
    els.decisionSessionGoal = document.getElementById("decisionSessionGoal");
    els.decisionStudentMessage = document.getElementById("decisionStudentMessage");
    els.decisionAlert = document.getElementById("decisionAlert");
    els.decisionForm = document.getElementById("decisionForm");
    els.decisionRequestId = document.getElementById("decisionRequestId");
    els.decisionAction = document.getElementById("decisionAction");
    els.decisionNote = document.getElementById("decisionNote");
    els.decisionCancelBtn = document.getElementById("decisionCancelBtn");
    els.decisionConfirmBtn = document.getElementById("decisionConfirmBtn");
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
      if (state.currentUser.avatarUrl) {
        const fullAvatar = resolveAvatarUrl(state.currentUser.avatarUrl);
        els.navUserAvatar.innerHTML = `<img src="${fullAvatar}" alt="${escapeHtml(name)}" onerror="this.outerHTML='${escapeHtml(name.charAt(0).toUpperCase())}'" />`;
      } else {
        els.navUserAvatar.textContent = name.charAt(0).toUpperCase();
      }

      const role = (state.currentUser.role || "student").toLowerCase();
      const isAlumni = role === "alumni";
      els.navUserRoleBadge.textContent = isAlumni ? "Mentor" : "Student";
      els.navUserRoleBadge.className = `user-role-badge ${role}`;

      // Navigation Visibility:
      // Students see: "Explore Mentors" and "My Profile" (no requests tab)
      // Mentors (Alumni) see: "Mentorship Requests" and "My Profile" (no explore mentors tab)
      if (els.tabSearchBtn) {
        els.tabSearchBtn.style.display = isAlumni ? "none" : "inline-flex";
      }
      if (els.tabRequestsBtn) {
        els.tabRequestsBtn.style.display = isAlumni ? "inline-flex" : "none";
      }

      // Fetch active pending requests count for mentors
      if (isAlumni) {
        updatePendingBadge();
      } else if (els.navRequestsBadge) {
        els.navRequestsBadge.style.display = "none";
      }
    } else {
      // User is guest
      els.guestNav.style.display = "flex";
      els.authNav.style.display = "none";
      if (els.tabSearchBtn) els.tabSearchBtn.style.display = "inline-flex";
      if (els.tabRequestsBtn) els.tabRequestsBtn.style.display = "none";
      if (els.navRequestsBadge) els.navRequestsBadge.style.display = "none";
    }
  }

  // =========================================================================
  // View Routing & Switching
  // =========================================================================
  function switchView(viewName) {
    // If user is not authenticated and tries to open protected views, redirect to login
    if (!state.currentUser && (viewName === "search" || viewName === "profile" || viewName === "requests")) {
      viewName = "login";
      setLoginAlert("error", "Please sign in to access your portal account.");
    }

    const isAlumni = state.currentUser && (state.currentUser.role || "").toLowerCase() === "alumni";

    // Alumni mentors only access "requests" and "profile" (never explore mentors)
    if (isAlumni && viewName === "search") {
      viewName = "requests";
    }

    // Students only access "search" and "profile" (never the mentor requests dashboard)
    if (state.currentUser && !isAlumni && viewName === "requests") {
      viewName = "search";
    }

    state.activeView = viewName;

    // Hide all views
    els.loginView.classList.remove("active");
    els.studentRegisterView.classList.remove("active");
    els.registerView.classList.remove("active");
    els.searchView.classList.remove("active");
    if (els.profileView) els.profileView.classList.remove("active");
    if (els.requestsView) els.requestsView.classList.remove("active");

    // Deactivate guest & auth nav buttons
    els.navLoginBtn.classList.remove("active");
    els.navStudentRegBtn.classList.remove("active");
    els.navAlumniRegBtn.classList.remove("active");
    els.tabSearchBtn.classList.remove("active");
    if (els.tabProfileBtn) els.tabProfileBtn.classList.remove("active");
    if (els.tabRequestsBtn) els.tabRequestsBtn.classList.remove("active");

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
    } else if (viewName === "profile") {
      if (els.profileView) els.profileView.classList.add("active");
      if (els.tabProfileBtn) els.tabProfileBtn.classList.add("active");
      loadAndRenderProfile();
    } else if (viewName === "requests") {
      if (els.requestsView) els.requestsView.classList.add("active");
      if (els.tabRequestsBtn) els.tabRequestsBtn.classList.add("active");
      loadAndRenderRequests();
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
        const isAlumni = (res.data.user.role || "").toLowerCase() === "alumni";
        const redirectMsg = isAlumni
          ? "Login successful! Redirecting to mentorship requests..."
          : "Login successful! Redirecting to mentors directory...";
        setLoginAlert("success", redirectMsg);
        saveSession(res.data.user);

        setTimeout(() => {
          els.loginForm.reset();
          setLoginAlert(null);
          switchView(isAlumni ? "requests" : "search");
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

    // Validate Profile Picture (Compulsory)
    const sregCard = document.getElementById("sreg_avatar_card");
    if (!state.pendingAvatars.student) {
      clientErrors.s_avatar = "Profile picture is required. Please choose a photo.";
      if (sregCard) sregCard.classList.add("is-invalid");
    } else {
      if (sregCard) sregCard.classList.remove("is-invalid");
    }

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
    if (state.pendingAvatars.student) {
      payload.avatarData = state.pendingAvatars.student;
    }

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

    // Validate Profile Picture (Compulsory)
    const regCard = document.getElementById("reg_avatar_card");
    if (!state.pendingAvatars.alumni) {
      clientErrors.avatar = "Profile picture is required. Please choose a photo.";
      if (regCard) regCard.classList.add("is-invalid");
    } else {
      if (regCard) regCard.classList.remove("is-invalid");
    }

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
    if (state.pendingAvatars.alumni) {
      payload.avatarData = state.pendingAvatars.alumni;
    }

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
            switchView("requests");
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

    let input = null;
    if (field.startsWith("s_")) {
      const suffix = field.substring(2);
      input = document.getElementById(`sreg_${suffix}`) || document.getElementById(`sreg_mobile`) || document.querySelector(`[name="${suffix}"]`);
    } else {
      input = document.getElementById(`reg_${field}`) || document.getElementById(`reg_mobile`) || document.querySelector(`[name="${field}"]`);
    }
    if (input) {
      input.classList.add("is-invalid");
      input.classList.remove("is-valid");
    }
  }

  function clearFieldErrors() {
    document.querySelectorAll(".field-error-msg").forEach((el) => {
      el.textContent = "";
      el.classList.remove("visible");
    });
    document.querySelectorAll("input.is-invalid, select.is-invalid, textarea.is-invalid").forEach((el) => {
      el.classList.remove("is-invalid");
    });
    document.querySelectorAll("input.is-valid, select.is-valid, textarea.is-valid").forEach((el) => {
      el.classList.remove("is-valid");
    });
    document.querySelectorAll(".form-avatar-picker-card.is-invalid").forEach((el) => {
      el.classList.remove("is-invalid");
    });
  }

  // =========================================================================
  // Profile Management Workflow (Week 3)
  // =========================================================================
  async function loadAndRenderProfile() {
    if (!state.currentUser || !state.currentUser.id) return;
    setProfileAlert(null);

    try {
      const res = await ApiClient.getProfile(state.currentUser.id);
      if (res.ok && res.data.success && res.data.profile) {
        state.profileData = res.data.profile;
        if (state.profileData.avatarUrl) {
          state.currentUser.avatarUrl = state.profileData.avatarUrl;
          saveSession(state.currentUser);
        }
        renderProfileView(state.profileData);
      } else {
        setProfileAlert("error", res.data.message || "Failed to load profile details.");
      }
    } catch (err) {
      console.error("Profile load error:", err);
      setProfileAlert("error", "Unable to connect to Java backend to retrieve profile.");
    }
  }

  function renderProfileView(p) {
    if (!p) return;
    const name = p.fullName || "User";
    const role = (p.role || "student").toLowerCase();

    // Header card
    els.profileHeaderName.textContent = name;
    els.profileHeaderEmail.textContent = p.email || "";
    if (p.avatarUrl) {
      const fullAvatar = resolveAvatarUrl(p.avatarUrl);
      els.profileAvatarLg.innerHTML = `<img src="${fullAvatar}" alt="${escapeHtml(name)}" onerror="this.outerHTML='<span>${escapeHtml(name.charAt(0).toUpperCase())}</span>'" />`;
      if (els.removeAvatarBtn) els.removeAvatarBtn.style.display = "flex";
      if (els.pe_avatar_preview) els.pe_avatar_preview.innerHTML = `<img src="${fullAvatar}" alt="${escapeHtml(name)}" />`;
      if (els.pe_avatar_remove) els.pe_avatar_remove.style.display = "inline-flex";
    } else {
      els.profileAvatarLg.textContent = name.charAt(0).toUpperCase();
      if (els.removeAvatarBtn) els.removeAvatarBtn.style.display = "none";
      if (els.pe_avatar_preview) els.pe_avatar_preview.innerHTML = `<span>👤</span>`;
      if (els.pe_avatar_remove) els.pe_avatar_remove.style.display = "none";
    }
    els.profileHeaderBadge.textContent = role === "alumni" ? "Mentor" : "Student";
    els.profileHeaderBadge.className = `user-role-badge ${role}`;

    if (p.createdAt) {
      const yr = p.createdAt.substring(0, 4);
      els.profileMemberSince.textContent = yr;
    }

    // Contact info
    els.pv_fullName.textContent = name;
    els.pv_email.textContent = p.email || "—";
    els.pv_mobile.textContent = p.mobileNumber || "—";

    // Academic
    els.pv_department.textContent = p.department || "—";
    els.pv_graduationYear.textContent = p.graduationYear ? `Class of ${p.graduationYear}` : "—";

    if (role === "student") {
      els.pv_studentIdRow.style.display = "flex";
      els.pv_studentId.textContent = p.studentId || "—";
      els.pv_academicTitle.textContent = "Academic Details";
      els.pv_professionalCard.style.display = "none";
      els.pv_mentorshipCard.style.display = "none";
    } else {
      els.pv_studentIdRow.style.display = "none";
      els.pv_academicTitle.textContent = "Alumni Education Background";
      els.pv_professionalCard.style.display = "block";
      els.pv_mentorshipCard.style.display = "block";

      els.pv_company.textContent = p.company || "—";
      els.pv_designation.textContent = p.designation || "—";
      els.pv_industry.textContent = p.industry || "—";
      els.pv_experience.textContent = (p.experienceYears !== null && p.experienceYears !== undefined) ? `${p.experienceYears} Years` : "—";
      
      if (p.linkedinProfile) {
        els.pv_linkedin.innerHTML = `<a href="${escapeHtml(p.linkedinProfile)}" target="_blank" rel="noopener noreferrer" style="color:#0284c7; font-weight:600; text-decoration:underline;">${escapeHtml(p.linkedinProfile)}</a>`;
      } else {
        els.pv_linkedin.textContent = "Not specified";
      }

      // Skills tags
      els.pv_skillsTags.innerHTML = "";
      if (p.skills) {
        p.skills.split(",").map(s => s.trim()).filter(Boolean).forEach(skill => {
          const badge = document.createElement("span");
          badge.className = "skill-tag-badge";
          badge.textContent = skill;
          els.pv_skillsTags.appendChild(badge);
        });
      } else {
        els.pv_skillsTags.textContent = "No skills specified.";
      }

      els.pv_maxMentees.textContent = p.maxMentees ? `${p.maxMentees} Students` : "Flexible";
      els.pv_bio.textContent = p.bio || "No biography provided yet.";
    }

    // Default to view mode
    toggleProfileEditMode(false);
  }

  function toggleProfileEditMode(isEdit) {
    state.isEditingProfile = isEdit;
    setProfileAlert(null);

    if (isEdit) {
      els.profileViewMode.style.display = "none";
      els.profileEditMode.style.display = "block";
      els.editProfileToggleBtn.innerHTML = `<span class="btn-icon">👁️</span> View Profile`;
      els.editProfileToggleBtn.classList.add("active-editing");

      // Prefill edit form
      const p = state.profileData || state.currentUser;
      if (!p) return;

      els.pe_fullName.value = p.fullName || "";
      els.pe_email.value = p.email || "";
      els.pe_mobileNumber.value = p.mobileNumber || "";

      const role = (p.role || "student").toLowerCase();
      if (role === "student") {
        els.pe_studentFields.style.display = "block";
        els.pe_alumniFields.style.display = "none";

        els.pe_studentId.value = p.studentId || "";
        els.pe_s_department.value = p.department || "Computer Engineering";
        els.pe_s_graduationYear.value = p.graduationYear || 2026;
      } else {
        els.pe_studentFields.style.display = "none";
        els.pe_alumniFields.style.display = "block";

        els.pe_a_department.value = p.department || "";
        els.pe_a_graduationYear.value = p.graduationYear || "";
        els.pe_company.value = p.company || "";
        els.pe_designation.value = p.designation || "";
        els.pe_experience.value = (p.experienceYears !== null && p.experienceYears !== undefined) ? p.experienceYears : "";
        els.pe_industry.value = p.industry || "Information Technology";
        els.pe_linkedin.value = p.linkedinProfile || "";
        els.pe_skills.value = p.skills || "";
        els.pe_bio.value = p.bio || "";
        els.pe_maxMentees.value = p.maxMentees || "";
      }
    } else {
      els.profileViewMode.style.display = "block";
      els.profileEditMode.style.display = "none";
      els.editProfileToggleBtn.innerHTML = `<span class="btn-icon">✏️</span> Edit Profile`;
      els.editProfileToggleBtn.classList.remove("active-editing");
    }
  }

  async function handleProfileEditSubmit(e) {
    e.preventDefault();
    setProfileAlert(null);
    clearFieldErrors();

    if (!state.currentUser || !state.currentUser.id) return;
    const role = (state.currentUser.role || "student").toLowerCase();

    const fullName = els.pe_fullName.value.trim();
    const mobileNumber = els.pe_mobileNumber.value.trim();

    let clientErrors = {};

    if (!fullName) {
      clientErrors.pe_fullName = "Full name is required.";
    } else if (fullName.length < 2 || fullName.length > 50) {
      clientErrors.pe_fullName = "Full name must be between 2 and 50 characters.";
    } else if (/[0-9]/.test(fullName)) {
      clientErrors.pe_fullName = "Full name cannot contain numbers.";
    } else if (fullName.includes("  ")) {
      clientErrors.pe_fullName = "Full name cannot contain consecutive spaces.";
    }

    if (!mobileNumber) {
      clientErrors.pe_mobileNumber = "Mobile number is required.";
    } else if (!/^[6-9]\d{9}$/.test(mobileNumber)) {
      clientErrors.pe_mobileNumber = "Enter a valid 10-digit mobile number starting with 6-9.";
    }

    let payload = {
      fullName,
      mobileNumber
    };

    if (role === "student") {
      const department = els.pe_s_department.value;
      const gradYear = parseInt(els.pe_s_graduationYear.value, 10);

      if (!department) clientErrors.pe_s_department = "Department is required.";
      if (!gradYear || isNaN(gradYear) || gradYear < 2024 || gradYear > 2035) {
        clientErrors.pe_s_graduationYear = "Graduation year must be between 2024 and 2035.";
      }

      payload.department = department;
      payload.graduationYear = gradYear;

    } else {
      const department = els.pe_a_department.value.trim();
      const gradYear = parseInt(els.pe_a_graduationYear.value, 10);
      const company = els.pe_company.value.trim();
      const designation = els.pe_designation.value.trim();
      const exp = parseInt(els.pe_experience.value, 10);
      const industry = els.pe_industry.value;
      const linkedin = els.pe_linkedin.value.trim();
      const skills = els.pe_skills.value.trim();
      const bio = els.pe_bio.value.trim();
      const maxMentees = parseInt(els.pe_maxMentees.value, 10);

      const curYear = new Date().getFullYear();
      if (!department) clientErrors.pe_a_department = "Department is required.";
      if (!gradYear || isNaN(gradYear) || gradYear < 1950 || gradYear > curYear) {
        clientErrors.pe_a_graduationYear = `Graduation year must be between 1950 and ${curYear}.`;
      }
      if (!company) clientErrors.pe_company = "Current organization is required.";
      if (!designation) clientErrors.pe_designation = "Designation is required.";
      if (linkedin && !/^https?:\/\/(www\.)?linkedin\.com\/.*$/i.test(linkedin)) {
        clientErrors.pe_linkedInProfile = "Please enter a valid LinkedIn URL.";
      }

      payload.department = department;
      payload.graduationYear = gradYear;
      payload.company = company;
      payload.designation = designation;
      payload.experienceYears = isNaN(exp) ? null : exp;
      payload.industry = industry;
      payload.linkedInProfile = linkedin || null;
      payload.skills = skills;
      payload.bio = bio;
      payload.maxMentees = isNaN(maxMentees) ? null : maxMentees;
    }

    if (Object.keys(clientErrors).length > 0) {
      Object.entries(clientErrors).forEach(([field, msg]) => {
        showFieldError(field, msg);
      });
      setProfileAlert("error", "Please correct the highlighted fields before saving.");
      return;
    }

    els.saveProfileBtn.disabled = true;
    els.saveProfileBtn.textContent = "Saving...";

    try {
      const res = await ApiClient.updateProfile(state.currentUser.id, payload);
      if (res.ok && res.data.success && res.data.profile) {
        setProfileAlert("success", "Profile updated successfully!");
        state.profileData = res.data.profile;

        // Sync with state.currentUser & localStorage
        state.currentUser.fullName = res.data.profile.fullName;
        state.currentUser.mobileNumber = res.data.profile.mobileNumber;
        state.currentUser.department = res.data.profile.department;
        state.currentUser.graduationYear = res.data.profile.graduationYear;
        if (role === "alumni") {
          state.currentUser.company = res.data.profile.company;
          state.currentUser.designation = res.data.profile.designation;
        }
        saveSession(state.currentUser);

        setTimeout(() => {
          renderProfileView(state.profileData);
          toggleProfileEditMode(false);
        }, 500);

      } else {
        if (res.data.errors) {
          Object.entries(res.data.errors).forEach(([field, msg]) => {
            showFieldError(`pe_${field}`, msg);
          });
        }
        setProfileAlert("error", res.data.message || "Failed to update profile. Please try again.");
      }
    } catch (err) {
      console.error("Profile update error:", err);
      setProfileAlert("error", "Could not connect to Java backend to save changes.");
    } finally {
      els.saveProfileBtn.disabled = false;
      els.saveProfileBtn.textContent = "Save Changes";
    }
  }

  function setProfileAlert(type, msg) {
    if (!els.profileStatusAlert) return;
    if (!type || !msg) {
      els.profileStatusAlert.className = "form-status-alert";
      els.profileStatusAlert.textContent = "";
      return;
    }
    els.profileStatusAlert.className = `form-status-alert ${type}`;
    els.profileStatusAlert.textContent = msg;
  }

  // =========================================================================
  // Avatar BLOB Upload & Removal (Approach 2)
  // =========================================================================
  async function handleAvatarFileUpload(e) {
    const file = e.target.files && e.target.files[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setProfileAlert("error", "Please select a valid image file (JPEG, PNG, WebP).");
      e.target.value = "";
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setProfileAlert("error", "Image is too large. Please select a file under 10MB.");
      e.target.value = "";
      return;
    }

    try {
      setProfileAlert("info", "Saving image directly to MySQL database...");
      const dataUrl = await resizeImageToDataUrl(file, 400, 400);

      const res = await ApiClient.uploadAvatar(state.currentUser.id, dataUrl, "image/jpeg");
      if (res.ok && res.data.success) {
        const cacheBustedUrl = `${res.data.avatarUrl}?t=${Date.now()}`;
        state.currentUser.avatarUrl = cacheBustedUrl;
        saveSession(state.currentUser);

        els.profileAvatarLg.innerHTML = `<img src="${cacheBustedUrl}" alt="Avatar" />`;
        if (els.removeAvatarBtn) els.removeAvatarBtn.style.display = "flex";
        updateAuthHeader();

        setProfileAlert("success", "✅ Profile picture saved directly in MySQL BLOB storage!");
      } else {
        setProfileAlert("error", res.data.message || "Failed to save profile picture.");
      }
    } catch (err) {
      console.error("Avatar upload error:", err);
      setProfileAlert("error", "Error uploading picture: " + err.message);
    } finally {
      e.target.value = "";
    }
  }

  function resizeImageToDataUrl(file, maxW, maxH) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (re) => {
        const img = new Image();
        img.onload = () => {
          let w = img.width;
          let h = img.height;
          if (w > maxW || h > maxH) {
            if (w > h) {
              h = Math.round((h * maxW) / w);
              w = maxW;
            } else {
              w = Math.round((w * maxH) / h);
              h = maxH;
            }
          }
          const canvas = document.createElement("canvas");
          canvas.width = w;
          canvas.height = h;
          const ctx = canvas.getContext("2d");
          ctx.drawImage(img, 0, 0, w, h);
          resolve(canvas.toDataURL("image/jpeg", 0.88));
        };
        img.onerror = () => reject(new Error("Unable to decode image file."));
        img.src = re.target.result;
      };
      reader.onerror = () => reject(new Error("Unable to read image file."));
      reader.readAsDataURL(file);
    });
  }

  async function handleAvatarRemove() {
    if (!state.currentUser || !state.currentUser.id) return;
    if (!confirm("Are you sure you want to remove your profile picture?")) return;

    try {
      const res = await ApiClient.deleteAvatar(state.currentUser.id);
      if (res.ok && res.data.success) {
        state.currentUser.avatarUrl = null;
        saveSession(state.currentUser);

        const name = state.currentUser.fullName || "User";
        els.profileAvatarLg.textContent = name.charAt(0).toUpperCase();
        if (els.removeAvatarBtn) els.removeAvatarBtn.style.display = "none";
        if (els.pe_avatar_preview) els.pe_avatar_preview.innerHTML = `<span>👤</span>`;
        if (els.pe_avatar_remove) els.pe_avatar_remove.style.display = "none";
        updateAuthHeader();

        setProfileAlert("success", "Profile picture removed.");
      } else {
        setProfileAlert("error", res.data.message || "Failed to remove avatar.");
      }
    } catch (err) {
      console.error("Avatar delete error:", err);
      setProfileAlert("error", "Error removing avatar: " + err.message);
    }
  }

  function setupFormAvatarPicker(inputEl, previewEl, removeBtnEl, onSelect, onRemove) {
    if (!inputEl) return;

    inputEl.addEventListener("change", async (e) => {
      const file = e.target.files && e.target.files[0];
      if (!file) return;

      if (!file.type.startsWith("image/")) {
        alert("Please select a valid image file (PNG, JPG, WebP).");
        e.target.value = "";
        return;
      }

      if (file.size > 10 * 1024 * 1024) {
        alert("Image is too large. Please choose a photo under 10MB.");
        e.target.value = "";
        return;
      }

      try {
        const dataUrl = await resizeImageToDataUrl(file, 400, 400);
        if (previewEl) {
          previewEl.innerHTML = `<img src="${dataUrl}" alt="Preview" />`;
        }
        if (removeBtnEl) {
          removeBtnEl.style.display = "inline-flex";
        }
        const cardEl = inputEl.closest(".form-avatar-picker-card");
        if (cardEl) cardEl.classList.remove("is-invalid");
        const errEl = cardEl ? cardEl.querySelector(".field-error-msg") : null;
        if (errEl) {
          errEl.textContent = "";
          errEl.classList.remove("visible");
        }
        if (typeof onSelect === "function") {
          onSelect(dataUrl);
        }
      } catch (err) {
        console.error("Avatar preview error:", err);
      } finally {
        e.target.value = "";
      }
    });

    if (removeBtnEl) {
      removeBtnEl.addEventListener("click", () => {
        if (previewEl) {
          previewEl.innerHTML = `<span>👤</span>`;
        }
        removeBtnEl.style.display = "none";
        if (typeof onRemove === "function") {
          onRemove();
        }
      });
    }
  }

  // =========================================================================
  // Search Execution & Mentors Directory
  // =========================================================================
  async function performSearch() {
    setLoading(true);
    hideError();

    try {
      // If student is logged in, fetch their latest requests to display status on cards
      if (state.currentUser && (state.currentUser.role || "").toLowerCase() === "student") {
        try {
          const reqRes = await ApiClient.getRequestsByStudent(state.currentUser.id);
          state.studentRequests = (reqRes.ok && reqRes.data && reqRes.data.requests) ? reqRes.data.requests : [];
        } catch (e) {
          state.studentRequests = [];
        }
      } else {
        state.studentRequests = [];
      }

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
    const avatarSrc = resolveAvatarUrl(mentor.avatarUrl);
    const avatarHtml = avatarSrc
      ? `<img src="${avatarSrc}" alt="${escapeHtml(fullName)}" class="avatar avatar-img" onerror="this.outerHTML='<div class=\\'avatar\\' style=\\'background: ${bgGradient}\\'>${initials}</div>'" />`
      : `<div class="avatar" style="background: ${bgGradient}">${initials}</div>`;
    cardTop.innerHTML = `
      ${avatarHtml}
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

    // Status badge if student has previously requested this mentor
    const latestReq = (state.studentRequests || []).find(r => r.mentorId === mentor.id);
    if (latestReq) {
      const statusPill = document.createElement("div");
      statusPill.className = `req-status-pill ${latestReq.status.toLowerCase()}`;
      if (latestReq.status === "PENDING") {
        statusPill.innerHTML = `<span>⏳</span> Request Pending`;
      } else if (latestReq.status === "ACCEPTED") {
        statusPill.innerHTML = `<span>✅</span> Approved · Connected`;
      } else if (latestReq.status === "REJECTED") {
        statusPill.innerHTML = `<span>❌</span> Request Declined`;
      }
      card.appendChild(statusPill);
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
    if (latestReq && latestReq.status === "PENDING") {
      connectBtn.textContent = "View Pending Status";
      connectBtn.style.background = "#d97706";
    } else if (latestReq && latestReq.status === "ACCEPTED") {
      connectBtn.textContent = "Request New Session";
      connectBtn.style.background = "#059669";
    } else if (latestReq && latestReq.status === "REJECTED") {
      connectBtn.textContent = "Request Again";
    } else {
      connectBtn.textContent = "Request Session";
    }
    connectBtn.addEventListener("click", () => openConnectModal(mentor));
    actions.appendChild(connectBtn);

    card.appendChild(actions);
    return card;
  }

  // =========================================================================
  // Connect Request Modal (Student -> Mentor)
  // =========================================================================
  function openConnectModal(mentor) {
    if (!state.currentUser) {
      switchView("login");
      setLoginAlert("error", "Please sign in to request a mentorship session.");
      return;
    }

    if ((state.currentUser.role || "").toLowerCase() === "alumni") {
      alert("Alumni mentors receive requests from students. To send a mentorship request, please use a student account.");
      return;
    }

    state.selectedMentor = mentor;
    els.summaryName.textContent = mentor.fullName || "Alumni Mentor";
    els.summaryRole.textContent = `${mentor.designation || "Professional"} at ${mentor.company || "Company"}`;
    els.summaryDept.textContent = `${mentor.department || "Engineering"} · ${mentor.experienceYears || 0} yrs experience`;

    if (els.connectAlert) {
      els.connectAlert.style.display = "none";
      els.connectAlert.className = "form-status-alert";
      els.connectAlert.textContent = "";
    }
    if (els.connectSuccess) {
      els.connectSuccess.style.display = "none";
      els.connectSuccess.classList.remove("visible");
      els.connectSuccess.textContent = "";
    }
    els.connectMsg.value = "";
    if (els.connectMsgLen) els.connectMsgLen.textContent = "0";
    els.connectForm.style.display = "block";

    // Check previous request status with this mentor
    const latestReq = (state.studentRequests || []).find(r => r.mentorId === mentor.id);
    if (els.connectPreviousStatus) {
      if (latestReq) {
        els.connectPreviousStatus.style.display = "block";
        els.connectPreviousStatus.className = `modal-status-box ${latestReq.status.toLowerCase()}`;

        if (latestReq.status === "PENDING") {
          els.connectPreviousStatus.innerHTML = `
            <div class="modal-status-title">⏳ Mentorship Request Under Review</div>
            <div>Your previous request sent on <strong>${escapeHtml(latestReq.createdAt || "recently")}</strong> is currently waiting for <strong>${escapeHtml(mentor.fullName)}</strong> to review and respond.</div>
          `;
          if (els.modalSubmitBtn) {
            els.modalSubmitBtn.disabled = true;
            els.modalSubmitBtn.textContent = "Request Already Pending";
          }
        } else if (latestReq.status === "ACCEPTED") {
          els.connectPreviousStatus.innerHTML = `
            <div class="modal-status-title">✅ Previous Request Approved!</div>
            <div><strong>${escapeHtml(mentor.fullName)}</strong> accepted your mentorship connection!</div>
            ${latestReq.mentorResponse ? `<div class="modal-status-note">💬 <strong>Mentor's Response:</strong> "${escapeHtml(latestReq.mentorResponse)}"</div>` : ""}
            <div style="margin-top: 8px; font-size: 12px; opacity: 0.9;">✨ You can submit another request below to schedule your next session or discuss a new topic.</div>
          `;
          if (els.modalSubmitBtn) {
            els.modalSubmitBtn.disabled = false;
            els.modalSubmitBtn.textContent = "Send Follow-up Request";
          }
        } else if (latestReq.status === "REJECTED") {
          els.connectPreviousStatus.innerHTML = `
            <div class="modal-status-title">❌ Previous Request Declined</div>
            <div><strong>${escapeHtml(mentor.fullName)}</strong> was unable to accept your previous request.</div>
            ${latestReq.mentorResponse ? `<div class="modal-status-note">💬 <strong>Mentor's Note:</strong> "${escapeHtml(latestReq.mentorResponse)}"</div>` : ""}
            <div style="margin-top: 8px; font-size: 12px; opacity: 0.9;">💡 You are welcome to submit a revised request below with an updated topic or questions.</div>
          `;
          if (els.modalSubmitBtn) {
            els.modalSubmitBtn.disabled = false;
            els.modalSubmitBtn.textContent = "Send New Request";
          }
        } else {
          els.connectPreviousStatus.style.display = "none";
          if (els.modalSubmitBtn) {
            els.modalSubmitBtn.disabled = false;
            els.modalSubmitBtn.textContent = "Send Request";
          }
        }
      } else {
        els.connectPreviousStatus.style.display = "none";
        if (els.modalSubmitBtn) {
          els.modalSubmitBtn.disabled = false;
          els.modalSubmitBtn.textContent = "Send Request";
        }
      }
    }

    els.connectModal.classList.add("active");
  }

  function closeConnectModal() {
    els.connectModal.classList.remove("active");
    state.selectedMentor = null;
    if (els.connectPreviousStatus) els.connectPreviousStatus.style.display = "none";
    if (els.modalSubmitBtn) {
      els.modalSubmitBtn.disabled = false;
      els.modalSubmitBtn.textContent = "Send Request";
    }
  }

  async function handleConnectSubmit(e) {
    e.preventDefault();
    const sessionGoal = document.getElementById("sessionGoal").value;
    const connectMsg = els.connectMsg.value.trim();

    if (!connectMsg) {
      if (els.connectAlert) {
        els.connectAlert.className = "form-status-alert error";
        els.connectAlert.textContent = "Please provide an introductory message explaining your goals.";
        els.connectAlert.style.display = "block";
      }
      return;
    }

    if (els.connectAlert) els.connectAlert.style.display = "none";
    if (els.modalSubmitBtn) {
      els.modalSubmitBtn.disabled = true;
      els.modalSubmitBtn.textContent = "Submitting...";
    }

    try {
      const studentId = state.currentUser.id;
      const mentorId = state.selectedMentor.userId || state.selectedMentor.id;

      const res = await ApiClient.createMentorshipRequest({
        studentId,
        mentorId,
        sessionGoal,
        message: connectMsg
      });

      if (res.ok && res.data && res.data.success) {
        els.connectForm.style.display = "none";
        if (els.connectPreviousStatus) els.connectPreviousStatus.style.display = "none";
        const mentorName = state.selectedMentor ? state.selectedMentor.fullName : "your mentor";
        els.connectSuccess.innerHTML = `
          ✅ Mentorship request sent successfully!
          <br/><br/>
          <small><strong>${escapeHtml(mentorName)}</strong> will review your request and connect with you once accepted.</small>
        `;
        els.connectSuccess.style.display = "block";
        els.connectSuccess.classList.add("visible");

        // Immediately refresh student's request cache and re-render mentor cards
        if (state.currentUser && (state.currentUser.role || "").toLowerCase() === "student") {
          ApiClient.getRequestsByStudent(state.currentUser.id).then(reqRes => {
            if (reqRes.ok && reqRes.data && reqRes.data.requests) {
              state.studentRequests = reqRes.data.requests;
              performSearch();
            }
          }).catch(() => {});
        }

        setTimeout(() => {
          closeConnectModal();
        }, 2200);
      } else {
        const errMsg = (res.data && res.data.message) ? res.data.message : `Failed to send request (HTTP ${res.status})`;
        if (els.connectAlert) {
          els.connectAlert.className = "form-status-alert error";
          els.connectAlert.textContent = errMsg;
          els.connectAlert.style.display = "block";
        }
      }
    } catch (err) {
      if (els.connectAlert) {
        els.connectAlert.className = "form-status-alert error";
        els.connectAlert.textContent = err.message || "Failed to submit request.";
        els.connectAlert.style.display = "block";
      }
    } finally {
      if (els.modalSubmitBtn) {
        els.modalSubmitBtn.disabled = false;
        els.modalSubmitBtn.textContent = "Send Request";
      }
    }
  }

  // =========================================================================
  // Mentorship Requests Dashboard & Actions (Mentors Only)
  // =========================================================================
  async function updatePendingBadge() {
    if (!state.currentUser || !els.navRequestsBadge) return;
    const isAlumni = (state.currentUser.role || "").toLowerCase() === "alumni";
    if (!isAlumni) {
      els.navRequestsBadge.style.display = "none";
      return;
    }
    try {
      const res = await ApiClient.getRequestsByMentor(state.currentUser.id);
      if (res.ok && res.data && res.data.requests) {
        const pendingCount = res.data.requests.filter(r => r.status === "PENDING").length;
        if (pendingCount > 0) {
          els.navRequestsBadge.textContent = pendingCount;
          els.navRequestsBadge.style.display = "inline-flex";
        } else {
          els.navRequestsBadge.style.display = "none";
        }
      }
    } catch (e) {
      // Ignore background badge check failure
    }
  }

  async function loadAndRenderRequests() {
    if (!state.currentUser) return;

    const isMentor = (state.currentUser.role || "").toLowerCase() === "alumni";
    if (!isMentor) {
      // Students cannot access the mentor dashboard
      switchView("search");
      return;
    }

    if (els.requestsAlert) els.requestsAlert.style.display = "none";
    els.requestsList.innerHTML = "<div style='text-align:center; padding: 40px; color:#64748b;'>Loading requests...</div>";
    els.requestsEmptyState.style.display = "none";

    els.requestsTitle.textContent = "Received Mentorship Requests";
    els.requestsSubtitle.textContent = "Review students who have requested to connect with you, accept sessions, or decline.";
    els.emptyRequestsTitle.textContent = "No Requests Received Yet";
    els.emptyRequestsMsg.textContent = "When students discover your profile and request mentorship, their applications will show up here.";

    try {
      const res = await ApiClient.getRequestsByMentor(state.currentUser.id);

      if (!res.ok || !res.data || !res.data.requests) {
        throw new Error(res.data?.message || `Failed to fetch requests (HTTP ${res.status})`);
      }

      const requests = res.data.requests;

      // Calculate Stats
      const total = requests.length;
      const pending = requests.filter(r => r.status === "PENDING").length;
      const accepted = requests.filter(r => r.status === "ACCEPTED").length;
      const rejected = requests.filter(r => r.status === "REJECTED" || r.status === "CANCELLED").length;

      els.statTotalRequests.textContent = total;
      els.statPendingRequests.textContent = pending;
      els.statAcceptedRequests.textContent = accepted;
      els.statRejectedRequests.textContent = rejected;

      // Update badge
      if (els.navRequestsBadge) {
        if (pending > 0) {
          els.navRequestsBadge.textContent = pending;
          els.navRequestsBadge.style.display = "inline-flex";
        } else {
          els.navRequestsBadge.style.display = "none";
        }
      }

      if (requests.length === 0) {
        els.requestsList.innerHTML = "";
        els.requestsEmptyState.style.display = "block";
        return;
      }

      els.requestsList.innerHTML = "";
      requests.forEach(req => {
        const card = renderRequestCard(req, isMentor);
        els.requestsList.appendChild(card);
      });
    } catch (err) {
      els.requestsList.innerHTML = "";
      if (els.requestsAlert) {
        els.requestsAlert.className = "form-status-alert error";
        els.requestsAlert.textContent = err.message || "Failed to load mentorship requests.";
        els.requestsAlert.style.display = "block";
      }
    }
  }

  function renderRequestCard(req, isMentor) {
    const card = document.createElement("div");
    card.className = "request-card";

    // Header: Avatar, Name, Status Badge
    const header = document.createElement("div");
    header.className = "request-card-header";

    const personInfo = document.createElement("div");
    personInfo.className = "request-person-info";

    const avatar = document.createElement("div");
    avatar.className = "request-avatar";

    const nameGroup = document.createElement("div");
    nameGroup.className = "request-name-group";

    const personName = document.createElement("div");
    personName.className = "request-person-name";

    const personSub = document.createElement("div");
    personSub.className = "request-person-sub";

    if (isMentor) {
      // Shown to Mentor: Student Info
      const sName = req.studentName || (req.student && req.student.name) || "Student";
      const sDept = req.studentDepartment || (req.student && req.student.department) || "Engineering";
      const sGrad = req.studentGraduationYear || (req.student && req.student.graduationYear);
      personName.textContent = sName;
      personSub.textContent = `${sDept}${sGrad ? ` · Class of ${sGrad}` : ""}`;

      const avatarSrc = resolveAvatarUrl(req.studentAvatar || (req.student && req.student.avatarUrl));
      if (avatarSrc) {
        avatar.innerHTML = `<img src="${avatarSrc}" alt="${escapeHtml(sName)}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;" onerror="this.outerHTML='${escapeHtml(sName.charAt(0).toUpperCase())}'" />`;
      } else {
        avatar.textContent = sName.charAt(0).toUpperCase();
      }
    } else {
      // Shown to Student: Mentor Info
      const mName = req.mentorName || (req.mentor && req.mentor.name) || "Alumni Mentor";
      const mCompany = req.mentorCompany || (req.mentor && req.mentor.company) || "Company";
      const mDesignation = req.mentorDesignation || (req.mentor && req.mentor.designation) || "Professional";
      personName.textContent = mName;
      personSub.textContent = `${mDesignation} at ${mCompany}`;

      const avatarSrc = resolveAvatarUrl(req.mentorAvatar || (req.mentor && req.mentor.avatarUrl));
      if (avatarSrc) {
        avatar.innerHTML = `<img src="${avatarSrc}" alt="${escapeHtml(mName)}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;" onerror="this.outerHTML='${escapeHtml(mName.charAt(0).toUpperCase())}'" />`;
      } else {
        avatar.textContent = mName.charAt(0).toUpperCase();
      }
    }

    personInfo.appendChild(avatar);
    nameGroup.appendChild(personName);
    nameGroup.appendChild(personSub);
    personInfo.appendChild(nameGroup);

    // Status Badge
    const statusBadge = document.createElement("span");
    const st = (req.status || "PENDING").toUpperCase();
    statusBadge.className = `req-status-badge status-badge-${st.toLowerCase()}`;
    const statusIcons = {
      PENDING: "⏳ Pending",
      ACCEPTED: "✅ Accepted",
      REJECTED: "❌ Declined",
      CANCELLED: "🚫 Cancelled"
    };
    statusBadge.textContent = statusIcons[st] || st;

    header.appendChild(personInfo);
    header.appendChild(statusBadge);
    card.appendChild(header);

    // Session Goal Pill
    const goalPill = document.createElement("div");
    goalPill.className = "request-goal-pill";
    goalPill.innerHTML = `<span>🎯</span> <span>${escapeHtml(req.sessionGoal || "Mentorship")}</span>`;
    card.appendChild(goalPill);

    // Message Body
    const msgBox = document.createElement("div");
    msgBox.className = "request-message-box";
    msgBox.textContent = `"${req.message || ""}"`;
    card.appendChild(msgBox);

    // Mentor Response (if exists)
    if (req.mentorResponse) {
      const respBox = document.createElement("div");
      respBox.className = `request-response-box ${st.toLowerCase()}`;
      respBox.innerHTML = `<strong>Mentor Response:</strong> ${escapeHtml(req.mentorResponse)}`;
      card.appendChild(respBox);
    }

    // Card Footer: Timestamp & Actions
    const footer = document.createElement("div");
    footer.className = "request-footer";

    const timeSpan = document.createElement("span");
    timeSpan.className = "request-time";
    timeSpan.textContent = req.createdAt ? new Date(req.createdAt).toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    }) : "";
    footer.appendChild(timeSpan);

    const actionBtns = document.createElement("div");
    actionBtns.className = "request-action-buttons";

    if (st === "PENDING") {
      if (isMentor) {
        // Mentor actions: Accept or Reject
        const acceptBtn = document.createElement("button");
        acceptBtn.type = "button";
        acceptBtn.className = "btn-req-action btn-req-accept";
        acceptBtn.textContent = "Accept";
        acceptBtn.addEventListener("click", () => {
          openDecisionModal(req.id, req.studentName || "Student", req.sessionGoal, req.message, "ACCEPTED");
        });

        const rejectBtn = document.createElement("button");
        rejectBtn.type = "button";
        rejectBtn.className = "btn-req-action btn-req-reject";
        rejectBtn.textContent = "Decline";
        rejectBtn.addEventListener("click", () => {
          openDecisionModal(req.id, req.studentName || "Student", req.sessionGoal, req.message, "REJECTED");
        });

        actionBtns.appendChild(rejectBtn);
        actionBtns.appendChild(acceptBtn);
      } else {
        // Student action: Cancel Request
        const cancelBtn = document.createElement("button");
        cancelBtn.type = "button";
        cancelBtn.className = "btn-req-action btn-req-cancel";
        cancelBtn.textContent = "Cancel Request";
        cancelBtn.addEventListener("click", () => handleCancelRequest(req.id));
        actionBtns.appendChild(cancelBtn);
      }
    }

    footer.appendChild(actionBtns);
    card.appendChild(footer);
    return card;
  }

  // Mentor Decision Modal
  function openDecisionModal(requestId, studentName, sessionGoal, studentMessage, action) {
    els.decisionRequestId.value = requestId;
    els.decisionAction.value = action;
    els.decisionStudentName.textContent = studentName;
    els.decisionSessionGoal.textContent = sessionGoal || "Mentorship Session";
    els.decisionStudentMessage.textContent = `"${studentMessage || ""}"`;
    els.decisionNote.value = "";

    if (els.decisionAlert) els.decisionAlert.style.display = "none";

    if (action === "ACCEPTED") {
      els.decisionModalTitle.textContent = "Accept Mentorship Request";
      els.decisionConfirmBtn.textContent = "Confirm Acceptance";
      els.decisionConfirmBtn.className = "modal-submit-btn btn-req-accept";
    } else {
      els.decisionModalTitle.textContent = "Decline Mentorship Request";
      els.decisionConfirmBtn.textContent = "Confirm Decline";
      els.decisionConfirmBtn.className = "modal-submit-btn btn-req-reject";
    }

    els.decisionModal.classList.add("active");
  }

  function closeDecisionModal() {
    els.decisionModal.classList.remove("active");
  }

  async function handleDecisionSubmit(e) {
    e.preventDefault();
    const requestId = els.decisionRequestId.value;
    const action = els.decisionAction.value;
    const note = els.decisionNote.value.trim();

    if (!requestId || !action) return;

    if (els.decisionConfirmBtn) {
      els.decisionConfirmBtn.disabled = true;
      els.decisionConfirmBtn.textContent = "Processing...";
    }

    try {
      const res = await ApiClient.updateRequestStatus(requestId, {
        mentorId: state.currentUser ? state.currentUser.id : undefined,
        status: action,
        mentorResponse: note
      });

      if (res.ok && res.data && res.data.success) {
        closeDecisionModal();
        loadAndRenderRequests();
      } else {
        const errMsg = res.data?.message || `Update failed (HTTP ${res.status})`;
        if (els.decisionAlert) {
          els.decisionAlert.className = "form-status-alert error";
          els.decisionAlert.textContent = errMsg;
          els.decisionAlert.style.display = "block";
        }
      }
    } catch (err) {
      if (els.decisionAlert) {
        els.decisionAlert.className = "form-status-alert error";
        els.decisionAlert.textContent = err.message || "Failed to update request status.";
        els.decisionAlert.style.display = "block";
      }
    } finally {
      if (els.decisionConfirmBtn) {
        els.decisionConfirmBtn.disabled = false;
        els.decisionConfirmBtn.textContent = action === "ACCEPTED" ? "Confirm Acceptance" : "Confirm Decline";
      }
    }
  }

  async function handleCancelRequest(requestId) {
    if (!confirm("Are you sure you want to cancel this mentorship request?")) return;

    try {
      const res = await ApiClient.updateRequestStatus(requestId, {
        studentId: state.currentUser ? state.currentUser.id : undefined,
        status: "CANCELLED"
      });

      if (res.ok && res.data && res.data.success) {
        loadAndRenderRequests();
      } else {
        alert(res.data?.message || "Failed to cancel request.");
      }
    } catch (err) {
      alert(err.message || "Failed to cancel request.");
    }
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
    if (els.tabRequestsBtn) {
      els.tabRequestsBtn.addEventListener("click", () => switchView("requests"));
    }
    if (els.tabProfileBtn) {
      els.tabProfileBtn.addEventListener("click", () => switchView("profile"));
    }
    if (els.navUserSessionPill) {
      els.navUserSessionPill.addEventListener("click", () => switchView("profile"));
      els.navUserSessionPill.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          switchView("profile");
        }
      });
    }

    // Profile View Actions
    if (els.editProfileToggleBtn) {
      els.editProfileToggleBtn.addEventListener("click", () => toggleProfileEditMode(!state.isEditingProfile));
    }
    if (els.cancelProfileEditBtn) {
      els.cancelProfileEditBtn.addEventListener("click", () => toggleProfileEditMode(false));
    }
    if (els.profileEditForm) {
      els.profileEditForm.addEventListener("submit", handleProfileEditSubmit);
    }
    if (els.avatarFileInput) {
      els.avatarFileInput.addEventListener("change", handleAvatarFileUpload);
    }
    if (els.removeAvatarBtn) {
      els.removeAvatarBtn.addEventListener("click", handleAvatarRemove);
    }
    if (els.pe_fullName) {
      els.pe_fullName.addEventListener("input", (e) => {
        validateFullNameLive(e.target.value, document.getElementById("err_pe_fullName"));
      });
    }

    // Form Avatar Pickers
    setupFormAvatarPicker(
      els.sreg_avatar_input,
      els.sreg_avatar_preview,
      els.sreg_avatar_remove,
      (dataUrl) => { state.pendingAvatars.student = dataUrl; },
      () => { state.pendingAvatars.student = null; }
    );

    setupFormAvatarPicker(
      els.reg_avatar_input,
      els.reg_avatar_preview,
      els.reg_avatar_remove,
      (dataUrl) => { state.pendingAvatars.alumni = dataUrl; },
      () => { state.pendingAvatars.alumni = null; }
    );

    setupFormAvatarPicker(
      els.pe_avatar_input,
      els.pe_avatar_preview,
      els.pe_avatar_remove,
      async (dataUrl) => {
        if (state.currentUser && state.currentUser.id) {
          try {
            setProfileAlert("info", "Saving image directly to MySQL database...");
            const res = await ApiClient.uploadAvatar(state.currentUser.id, dataUrl, "image/jpeg");
            if (res.ok && res.data.success) {
              const url = `${res.data.avatarUrl}?t=${Date.now()}`;
              state.currentUser.avatarUrl = url;
              saveSession(state.currentUser);
              els.profileAvatarLg.innerHTML = `<img src="${url}" alt="Avatar" />`;
              if (els.removeAvatarBtn) els.removeAvatarBtn.style.display = "flex";
              if (els.pe_avatar_remove) els.pe_avatar_remove.style.display = "inline-flex";
              updateAuthHeader();
              setProfileAlert("success", "✅ Profile picture saved in MySQL database!");
            }
          } catch (e) {
            setProfileAlert("error", "Error saving image: " + e.message);
          }
        }
      },
      () => {
        handleAvatarRemove();
      }
    );

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

    // Live Email & Mobile In-Use Availability Verification
    function setupAvailabilityCheck(inputEl, errEl, type) {
      if (!inputEl || !errEl) return;
      let timer = null;

      async function check() {
        const val = inputEl.value.trim();
        if (!val) {
          errEl.textContent = "";
          errEl.classList.remove("visible");
          inputEl.classList.remove("is-invalid", "is-valid");
          return;
        }

        if (type === "email") {
          if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) return;
          try {
            const res = await ApiClient.checkAvailability({ email: val });
            if (res.ok && res.data.success) {
              if (!res.data.emailAvailable) {
                errEl.textContent = "This email address is already in use.";
                errEl.classList.add("visible");
                inputEl.classList.add("is-invalid");
                inputEl.classList.remove("is-valid");
              } else {
                errEl.textContent = "";
                errEl.classList.remove("visible");
                inputEl.classList.remove("is-invalid");
                inputEl.classList.add("is-valid");
              }
            }
          } catch (err) {
            console.error("Email availability check failed:", err);
          }
        } else if (type === "mobile") {
          const digits = val.replace(/[^0-9]/g, "");
          if (digits.length < 10) return;
          try {
            const res = await ApiClient.checkAvailability({ mobileNumber: val });
            if (res.ok && res.data.success) {
              if (!res.data.mobileAvailable) {
                errEl.textContent = "This mobile number is already in use.";
                errEl.classList.add("visible");
                inputEl.classList.add("is-invalid");
                inputEl.classList.remove("is-valid");
              } else {
                errEl.textContent = "";
                errEl.classList.remove("visible");
                inputEl.classList.remove("is-invalid");
                inputEl.classList.add("is-valid");
              }
            }
          } catch (err) {
            console.error("Mobile availability check failed:", err);
          }
        }
      }

      inputEl.addEventListener("input", () => {
        inputEl.classList.remove("is-invalid", "is-valid");
        clearTimeout(timer);
        timer = setTimeout(check, 350);
      });

      inputEl.addEventListener("blur", () => {
        clearTimeout(timer);
        check();
      });
    }

    // Student Registration: Live Email & Mobile Checks
    const sregEmailInput = document.getElementById("sreg_email");
    const sregEmailErr = document.getElementById("err_s_email");
    const sregMobileInput = document.getElementById("sreg_mobile");
    const sregMobileErr = document.getElementById("err_s_mobileNumber");
    setupAvailabilityCheck(sregEmailInput, sregEmailErr, "email");
    setupAvailabilityCheck(sregMobileInput, sregMobileErr, "mobile");

    // Alumni Registration: Live Email & Mobile Checks
    const regEmailInput = document.getElementById("reg_email");
    const regEmailErr = document.getElementById("err_email");
    const regMobileInput = document.getElementById("reg_mobile");
    const regMobileErr = document.getElementById("err_mobileNumber");
    setupAvailabilityCheck(regEmailInput, regEmailErr, "email");
    setupAvailabilityCheck(regMobileInput, regMobileErr, "mobile");

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
    if (els.connectMsg && els.connectMsgLen) {
      els.connectMsg.addEventListener("input", (e) => {
        els.connectMsgLen.textContent = e.target.value.length;
      });
    }

    // Mentorship Requests View & Actions
    if (els.refreshRequestsBtn) {
      els.refreshRequestsBtn.addEventListener("click", () => {
        loadAndRenderRequests();
      });
    }

    // Decision Modal (Mentor Accept / Decline)
    if (els.decisionModalCloseBtn) {
      els.decisionModalCloseBtn.addEventListener("click", closeDecisionModal);
    }
    if (els.decisionCancelBtn) {
      els.decisionCancelBtn.addEventListener("click", closeDecisionModal);
    }
    if (els.decisionModal) {
      els.decisionModal.addEventListener("click", (e) => {
        if (e.target === els.decisionModal) closeDecisionModal();
      });
    }
    if (els.decisionForm) {
      els.decisionForm.addEventListener("submit", handleDecisionSubmit);
    }
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
      const isAlumni = (state.currentUser.role || "").toLowerCase() === "alumni";
      switchView(isAlumni ? "requests" : "search");
    } else {
      switchView("login");
    }
  });
})();
