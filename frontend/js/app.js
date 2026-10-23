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
    els.openDirectScheduleBtn = document.getElementById("openDirectScheduleBtn");
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

    // Session History Elements (Team B & C)
    els.tabHistoryBtn = document.getElementById("tabHistoryBtn");
    els.historyView = document.getElementById("historyView");
    els.refreshHistoryBtn = document.getElementById("refreshHistoryBtn");
    els.historySortSelect = document.getElementById("historySortSelect");
    els.historyStatusFilter = document.getElementById("historyStatusFilter");
    els.historyTelemetryBadge = document.getElementById("historyTelemetryBadge");
    els.historyItemCount = document.getElementById("historyItemCount");
    els.historyExecTime = document.getElementById("historyExecTime");
    els.historyAlert = document.getElementById("historyAlert");
    els.historyListContainer = document.getElementById("historyListContainer");
    els.historyEmptyState = document.getElementById("historyEmptyState");

    // Admin Console Elements
    els.tabAdminBtn = document.getElementById("tabAdminBtn");
    els.adminView = document.getElementById("adminView");
    els.refreshAdminBtn = document.getElementById("refreshAdminBtn");
    els.adminAlert = document.getElementById("adminAlert");
    els.adminTotalUsers = document.getElementById("adminTotalUsers");
    els.adminTotalStudents = document.getElementById("adminTotalStudents");
    els.adminTotalAlumni = document.getElementById("adminTotalAlumni");
    els.adminTotalTeachers = document.getElementById("adminTotalTeachers");
    els.adminTotalInactive = document.getElementById("adminTotalInactive");
    els.adminRoleFilter = document.getElementById("adminRoleFilter");
    els.adminStatusFilter = document.getElementById("adminStatusFilter");
    els.adminSearchInput = document.getElementById("adminSearchInput");
    els.adminUsersTableBody = document.getElementById("adminUsersTableBody");

    // Admin Subtabs & Panels (Team C & A)
    els.adminSubtabUsersBtn = document.getElementById("adminSubtabUsersBtn");
    els.adminSubtabPendingBtn = document.getElementById("adminSubtabPendingBtn");
    els.adminSubtabDuplicatesBtn = document.getElementById("adminSubtabDuplicatesBtn");
    els.adminSubtabFeedbackBtn = document.getElementById("adminSubtabFeedbackBtn");
    els.adminSubtabEventsBtn = document.getElementById("adminSubtabEventsBtn");
    els.adminEventsBadge = document.getElementById("adminEventsBadge");

    els.adminPanelUsers = document.getElementById("adminPanelUsers");
    els.adminPanelPending = document.getElementById("adminPanelPending");
    els.adminPanelDuplicates = document.getElementById("adminPanelDuplicates");
    els.adminPanelFeedback = document.getElementById("adminPanelFeedback");
    els.adminPanelEvents = document.getElementById("adminPanelEvents");
    els.refreshAdminEventsBtn = document.getElementById("refreshAdminEventsBtn");
    els.adminEventsTableBody = document.getElementById("adminEventsTableBody");

    // Student Public Meets Section
    els.studentPublicMeetsSection = document.getElementById("studentPublicMeetsSection");
    els.refreshStudentPublicMeetsBtn = document.getElementById("refreshStudentPublicMeetsBtn");
    els.studentPublicMeetsList = document.getElementById("studentPublicMeetsList");

    // Teacher / Faculty Hub Elements
    els.tabTeacherBtn = document.getElementById("tabTeacherBtn");
    els.teacherView = document.getElementById("teacherView");
    els.teacherAlert = document.getElementById("teacherAlert");
    els.teacherSubtabSchedulesBtn = document.getElementById("teacherSubtabSchedulesBtn");
    els.teacherSubtabOneOnOneBtn = document.getElementById("teacherSubtabOneOnOneBtn");
    els.teacherSubtabPublicMeetBtn = document.getElementById("teacherSubtabPublicMeetBtn");
    els.teacherSubtabNewMentorBtn = document.getElementById("teacherSubtabNewMentorBtn");
    els.teacherSubtabDirectoryBtn = document.getElementById("teacherSubtabDirectoryBtn");
    els.teacherPanelSchedules = document.getElementById("teacherPanelSchedules");
    els.teacherPanelOneOnOne = document.getElementById("teacherPanelOneOnOne");
    els.teacherPanelPublicMeet = document.getElementById("teacherPanelPublicMeet");
    els.teacherPanelNewMentor = document.getElementById("teacherPanelNewMentor");
    els.teacherPanelDirectory = document.getElementById("teacherPanelDirectory");

    // Teacher Schedules Toolbar & Table
    els.filterMeetType = document.getElementById("filterMeetType");
    els.filterMeetStatus = document.getElementById("filterMeetStatus");
    els.refreshTeacherMeetsBtn = document.getElementById("refreshTeacherMeetsBtn");
    els.quickOneOnOneBtn = document.getElementById("quickOneOnOneBtn");
    els.quickPublicMeetBtn = document.getElementById("quickPublicMeetBtn");
    els.teacherMeetsTableBody = document.getElementById("teacherMeetsTableBody");

    // Teacher 1-on-1 Meet Form
    els.scheduleOneOnOneForm = document.getElementById("scheduleOneOnOneForm");
    els.scheduleOneOnOneAlert = document.getElementById("scheduleOneOnOneAlert");
    els.meetMentorSelect = document.getElementById("meetMentorSelect");
    els.meetStudentSelect = document.getElementById("meetStudentSelect");
    els.meetTitle = document.getElementById("meetTitle");
    els.meetDescription = document.getElementById("meetDescription");
    els.meetDate = document.getElementById("meetDate");
    els.meetStartTime = document.getElementById("meetStartTime");
    els.meetEndTime = document.getElementById("meetEndTime");
    els.meetingLink = document.getElementById("meetingLink");
    els.meetVenue = document.getElementById("meetVenue");
    els.submitOneOnOneBtn = document.getElementById("submitOneOnOneBtn");

    // Teacher Public Meet Form
    els.schedulePublicMeetForm = document.getElementById("schedulePublicMeetForm");
    els.schedulePublicMeetAlert = document.getElementById("schedulePublicMeetAlert");
    els.publicMentorSelect = document.getElementById("publicMentorSelect");
    els.publicMaxCapacity = document.getElementById("publicMaxCapacity");
    els.publicMeetTitle = document.getElementById("publicMeetTitle");
    els.publicMeetDesc = document.getElementById("publicMeetDesc");
    els.publicMeetDate = document.getElementById("publicMeetDate");
    els.publicStartTime = document.getElementById("publicStartTime");
    els.publicEndTime = document.getElementById("publicEndTime");
    els.publicMeetingLink = document.getElementById("publicMeetingLink");
    els.publicVenue = document.getElementById("publicVenue");
    els.submitPublicMeetBtn = document.getElementById("submitPublicMeetBtn");

    // Teacher Onboard Mentor Form
    els.onboardMentorForm = document.getElementById("onboardMentorForm");
    els.onboardMentorAlert = document.getElementById("onboardMentorAlert");
    els.newMentorName = document.getElementById("newMentorName");
    els.newMentorEmail = document.getElementById("newMentorEmail");
    els.newMentorMobile = document.getElementById("newMentorMobile");
    els.newMentorPassword = document.getElementById("newMentorPassword");
    els.newMentorCompany = document.getElementById("newMentorCompany");
    els.newMentorDesignation = document.getElementById("newMentorDesignation");
    els.newMentorDept = document.getElementById("newMentorDept");
    els.newMentorGradYear = document.getElementById("newMentorGradYear");
    els.newMentorExp = document.getElementById("newMentorExp");
    els.newMentorSkills = document.getElementById("newMentorSkills");
    els.newMentorCapacity = document.getElementById("newMentorCapacity");
    els.newMentorBio = document.getElementById("newMentorBio");
    els.submitOnboardMentorBtn = document.getElementById("submitOnboardMentorBtn");

    // Teacher Directory
    els.refreshTeacherDirectoryBtn = document.getElementById("refreshTeacherDirectoryBtn");
    els.teacherDirectoryTableBody = document.getElementById("teacherDirectoryTableBody");

    // Public Meet Attendees Modal
    els.meetAttendeesModal = document.getElementById("meetAttendeesModal");
    els.attendeesModalTitle = document.getElementById("attendeesModalTitle");
    els.meetAttendeesCloseBtn = document.getElementById("meetAttendeesCloseBtn");
    els.meetAttendeesDoneBtn = document.getElementById("meetAttendeesDoneBtn");
    els.attendeesModalSubtitle = document.getElementById("attendeesModalSubtitle");
    els.attendeesTableBody = document.getElementById("attendeesTableBody");

    els.adminPendingBadge = document.getElementById("adminPendingBadge");
    els.adminDuplicatesBadge = document.getElementById("adminDuplicatesBadge");

    els.adminPendingTableBody = document.getElementById("adminPendingTableBody");
    els.refreshPendingBtn = document.getElementById("refreshPendingBtn");

    els.runDuplicateScanBtn = document.getElementById("runDuplicateScanBtn");
    els.adminDuplicatesContainer = document.getElementById("adminDuplicatesContainer");
    els.manualDupName = document.getElementById("manualDupName");
    els.manualDupEmail = document.getElementById("manualDupEmail");
    els.manualDupMobile = document.getElementById("manualDupMobile");
    els.manualDupStudentId = document.getElementById("manualDupStudentId");
    els.checkCandidateDupBtn = document.getElementById("checkCandidateDupBtn");
    els.manualDupResultBox = document.getElementById("manualDupResultBox");

    els.adminAvgRatingScore = document.getElementById("adminAvgRatingScore");
    els.adminTotalFeedbackCount = document.getElementById("adminTotalFeedbackCount");
    els.adminFiveStarPercent = document.getElementById("adminFiveStarPercent");
    els.adminFeedbackListContainer = document.getElementById("adminFeedbackListContainer");
    els.refreshFeedbackBtn = document.getElementById("refreshFeedbackBtn");

    // Delete User Modal (Team A)
    els.deleteUserModal = document.getElementById("deleteUserModal");
    els.deleteUserModalCloseBtn = document.getElementById("deleteUserModalCloseBtn");
    els.deleteUserCancelBtn = document.getElementById("deleteUserCancelBtn");
    els.confirmDeleteUserBtn = document.getElementById("confirmDeleteUserBtn");
    els.deleteUserDetailsBox = document.getElementById("deleteUserDetailsBox");

    // Feedback Modal & Rating Elements (Team C)
    els.feedbackModal = document.getElementById("feedbackModal");
    els.feedbackModalCloseBtn = document.getElementById("feedbackModalCloseBtn");
    els.feedbackCancelBtn = document.getElementById("feedbackCancelBtn");
    els.feedbackForm = document.getElementById("feedbackForm");
    els.feedbackSubmitBtn = document.getElementById("feedbackSubmitBtn");
    els.feedbackAlert = document.getElementById("feedbackAlert");
    els.feedbackSessionId = document.getElementById("feedbackSessionId");
    els.feedbackSessionInfo = document.getElementById("feedbackSessionInfo");
    els.starRatingWidget = document.getElementById("starRatingWidget");
    els.starRatingLabel = document.getElementById("starRatingLabel");
    els.feedbackRatingValue = document.getElementById("feedbackRatingValue");
    els.feedbackComments = document.getElementById("feedbackComments");
    els.feedbackCharCount = document.getElementById("feedbackCharCount");

    // Session Scheduling Elements (Team 1)
    els.scheduledSessionsContainer = document.getElementById("scheduledSessionsContainer");
    els.scheduledSessionsList = document.getElementById("scheduledSessionsList");
    els.scheduleModal = document.getElementById("scheduleModal");
    els.scheduleModalTitle = document.getElementById("scheduleModalTitle");
    els.scheduleModalCloseBtn = document.getElementById("scheduleModalCloseBtn");
    els.scheduleAlert = document.getElementById("scheduleAlert");
    els.scheduleForm = document.getElementById("scheduleForm");
    els.scheduleRequestId = document.getElementById("scheduleRequestId");
    els.scheduleMentorId = document.getElementById("scheduleMentorId");
    els.scheduleStudentId = document.getElementById("scheduleStudentId");
    els.schedulePartnerAvatar = document.getElementById("schedulePartnerAvatar");
    els.schedulePartnerName = document.getElementById("schedulePartnerName");
    els.schedulePartnerSub = document.getElementById("schedulePartnerSub");
    els.scheduleGoalText = document.getElementById("scheduleGoalText");
    els.scheduleMentorSelectGroup = document.getElementById("scheduleMentorSelectGroup");
    els.scheduleMentorSelect = document.getElementById("scheduleMentorSelect");
    els.scheduleDate = document.getElementById("scheduleDate");
    els.scheduleTime = document.getElementById("scheduleTime");
    els.scheduleDuration = document.getElementById("scheduleDuration");
    els.scheduleMode = document.getElementById("scheduleMode");
    els.scheduleMeetingLink = document.getElementById("scheduleMeetingLink");
    els.scheduleNotes = document.getElementById("scheduleNotes");
    els.scheduleCancelBtn = document.getElementById("scheduleCancelBtn");
    els.scheduleSubmitBtn = document.getElementById("scheduleSubmitBtn");
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
    if (state.currentUser && state.currentUser.token && !user.token) {
      user.token = state.currentUser.token;
    }
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

  function handleUnauthorized(msg) {
    if (typeof closeConnectModal === "function") closeConnectModal();
    if (typeof closeDecisionModal === "function") closeDecisionModal();
    if (typeof closeScheduleModal === "function") closeScheduleModal();
    clearSession();
    switchView("login");
    setLoginAlert("error", msg || "Your session has expired. Please sign in again.");
  }

  ApiClient.onUnauthorized = (data) => {
    handleUnauthorized((data && data.message) ? data.message : "Your session has expired. Please sign in again.");
  };

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
      const isAdmin = role === "admin";
      const isTeacher = role === "teacher";
      const isStudent = role === "student";
      els.navUserRoleBadge.textContent = isAdmin ? "Admin" : (isTeacher ? "Teacher" : (isAlumni ? "Mentor" : "Student"));
      els.navUserRoleBadge.className = `user-role-badge ${role}`;

      // Navigation Visibility:
      if (els.tabSearchBtn) {
        els.tabSearchBtn.style.display = (isAdmin) ? "none" : "inline-flex";
      }
      if (els.tabRequestsBtn) {
        els.tabRequestsBtn.style.display = (isAlumni || isStudent) ? "inline-flex" : "none";
        const iconSpan = els.tabRequestsBtn.querySelector(".tab-icon");
        if (iconSpan && iconSpan.nextSibling) {
          iconSpan.nextSibling.textContent = isStudent ? " My Requests" : " Received Requests";
        }
      }
      if (els.tabHistoryBtn) {
        els.tabHistoryBtn.style.display = isAdmin ? "none" : "inline-flex";
      }
      if (els.tabTeacherBtn) {
        els.tabTeacherBtn.style.display = (isTeacher || isAdmin) ? "inline-flex" : "none";
      }
      if (els.tabAdminBtn) {
        els.tabAdminBtn.style.display = isAdmin ? "inline-flex" : "none";
      }

      // Fetch active pending requests count for mentors and students
      if (isAlumni || isStudent) {
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
      if (els.tabHistoryBtn) els.tabHistoryBtn.style.display = "none";
      if (els.tabTeacherBtn) els.tabTeacherBtn.style.display = "none";
      if (els.tabAdminBtn) els.tabAdminBtn.style.display = "none";
      if (els.navRequestsBadge) els.navRequestsBadge.style.display = "none";
    }
  }

  // =========================================================================
  // View Routing & Switching
  // =========================================================================
  function switchView(viewName) {
    // If user is not authenticated and tries to open protected views, redirect to login
    if (!state.currentUser && (viewName === "search" || viewName === "profile" || viewName === "requests" || viewName === "history" || viewName === "admin" || viewName === "teacher")) {
      viewName = "login";
      setLoginAlert("error", "Please sign in to access your portal account.");
    }

    const role = state.currentUser ? (state.currentUser.role || "").toLowerCase() : "";
    const isAlumni = role === "alumni";
    const isAdmin = role === "admin";
    const isTeacher = role === "teacher";

    // Admin redirect if requesting student/alumni only panels
    if (isAdmin && (viewName === "search" || viewName === "requests" || viewName === "history")) {
      viewName = "admin";
    }

    // Alumni mentors default to requests view rather than explore mentors
    if (isAlumni && viewName === "search") {
      viewName = "requests";
    }

    // Non-teachers/non-admins cannot access teacher portal
    if (viewName === "teacher" && !isTeacher && !isAdmin) {
      viewName = "search";
    }

    state.activeView = viewName;

    // Hide all views
    const allViews = [
      els.loginView,
      els.studentRegisterView,
      els.registerView,
      els.searchView,
      els.profileView,
      els.requestsView,
      els.historyView,
      els.adminView,
      els.teacherView
    ];
    allViews.forEach(v => {
      if (v) {
        v.classList.remove("active");
        v.style.display = "none";
      }
    });

    // Deactivate guest & auth nav buttons
    els.navLoginBtn.classList.remove("active");
    els.navStudentRegBtn.classList.remove("active");
    els.navAlumniRegBtn.classList.remove("active");
    els.tabSearchBtn.classList.remove("active");
    if (els.tabProfileBtn) els.tabProfileBtn.classList.remove("active");
    if (els.tabRequestsBtn) els.tabRequestsBtn.classList.remove("active");
    if (els.tabHistoryBtn) els.tabHistoryBtn.classList.remove("active");
    if (els.tabTeacherBtn) els.tabTeacherBtn.classList.remove("active");
    if (els.tabAdminBtn) els.tabAdminBtn.classList.remove("active");

    if (viewName === "login") {
      els.loginView.classList.add("active");
      els.loginView.style.display = "block";
      els.navLoginBtn.classList.add("active");
    } else if (viewName === "studentRegister") {
      els.studentRegisterView.classList.add("active");
      els.studentRegisterView.style.display = "block";
      els.navStudentRegBtn.classList.add("active");
    } else if (viewName === "alumniRegister") {
      els.registerView.classList.add("active");
      els.registerView.style.display = "block";
      els.navAlumniRegBtn.classList.add("active");
    } else if (viewName === "search") {
      els.searchView.classList.add("active");
      els.searchView.style.display = "block";
      els.tabSearchBtn.classList.add("active");
      performSearch();
      loadStudentPublicMeets();
    } else if (viewName === "profile") {
      if (els.profileView) {
        els.profileView.classList.add("active");
        els.profileView.style.display = "block";
      }
      if (els.tabProfileBtn) els.tabProfileBtn.classList.add("active");
      loadAndRenderProfile();
    } else if (viewName === "requests") {
      if (els.requestsView) {
        els.requestsView.classList.add("active");
        els.requestsView.style.display = "block";
      }
      if (els.tabRequestsBtn) els.tabRequestsBtn.classList.add("active");
      loadAndRenderRequests();
    } else if (viewName === "history") {
      if (els.historyView) {
        els.historyView.classList.add("active");
        els.historyView.style.display = "block";
      }
      if (els.tabHistoryBtn) els.tabHistoryBtn.classList.add("active");
      loadAndRenderHistory();
    } else if (viewName === "teacher") {
      if (els.teacherView) {
        els.teacherView.classList.add("active");
        els.teacherView.style.display = "block";
      }
      if (els.tabTeacherBtn) els.tabTeacherBtn.classList.add("active");
      loadTeacherPortal();
    } else if (viewName === "admin") {
      if (els.adminView) {
        els.adminView.classList.add("active");
        els.adminView.style.display = "block";
      }
      if (els.tabAdminBtn) els.tabAdminBtn.classList.add("active");
      if (els.adminPanelUsers) els.adminPanelUsers.style.display = "block";
      loadAndRenderAdminUsers();
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
        const role = (res.data.user.role || "").toLowerCase();
        const isAdmin = role === "admin";
        const isAlumni = role === "alumni";
        const redirectMsg = isAdmin
          ? "Login successful! Redirecting to admin console..."
          : (isAlumni ? "Login successful! Redirecting to mentorship requests..." : "Login successful! Redirecting to mentors directory...");
        setLoginAlert("success", redirectMsg);
        const userData = { ...res.data.user, token: res.data.token };
        saveSession(userData);

        setTimeout(() => {
          els.loginForm.reset();
          setLoginAlert(null);
          if (isAdmin) {
            switchView("admin");
          } else if (isAlumni) {
            switchView("requests");
          } else {
            switchView("search");
          }
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
      matchEl.textContent = "Passwords match";
    } else {
      matchEl.className = "password-match-msg mismatch";
      matchEl.textContent = "Passwords do not match";
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
          saveSession({ ...loginRes.data.user, token: loginRes.data.token });
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
          saveSession({ ...loginRes.data.user, token: loginRes.data.token });
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
      if (res.status === 401) {
        clearSession();
        switchView("login");
        setLoginAlert("error", "Your session has expired or requires authentication. Please sign in again.");
        return;
      }
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
      if (els.pe_avatar_preview) els.pe_avatar_preview.innerHTML = `<span class="default-avatar-placeholder">Photo</span>`;
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
      els.editProfileToggleBtn.innerHTML = `View Profile`;
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
      els.editProfileToggleBtn.innerHTML = `Edit Profile`;
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
      setProfileAlert("info", "Saving image directly to MySQL / MariaDB database...");
      const dataUrl = await resizeImageToDataUrl(file, 400, 400);

      const res = await ApiClient.uploadAvatar(state.currentUser.id, dataUrl, "image/jpeg");
      if (res.ok && res.data.success) {
        const cacheBustedUrl = `${res.data.avatarUrl}?t=${Date.now()}`;
        state.currentUser.avatarUrl = cacheBustedUrl;
        saveSession(state.currentUser);

        els.profileAvatarLg.innerHTML = `<img src="${cacheBustedUrl}" alt="Avatar" />`;
        if (els.removeAvatarBtn) els.removeAvatarBtn.style.display = "flex";
        updateAuthHeader();

        setProfileAlert("success", "Profile picture saved directly in MySQL / MariaDB BLOB storage.");
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
        if (els.pe_avatar_preview) els.pe_avatar_preview.innerHTML = `<span class="default-avatar-placeholder">Photo</span>`;
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
          previewEl.innerHTML = `<span class="default-avatar-placeholder">Photo</span>`;
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
        <div class="company-badge">${escapeHtml(company)}</div>
      </div>
    `;
    card.appendChild(cardTop);

    // Badges
    const badges = document.createElement("div");
    badges.className = "card-badges";
    let badgesHtml = `<span class="badge badge-dept" title="Department">${escapeHtml(department)}</span>`;
    if (gradYear) {
      badgesHtml += `<span class="badge badge-year" title="Graduation Year">Class of '${String(gradYear).slice(-2)}</span>`;
    }
    if (exp !== null && exp !== undefined) {
      badgesHtml += `<span class="badge badge-exp" title="Experience">${exp} yrs exp</span>`;
    }
    if (industry) {
      badgesHtml += `<span class="badge badge-industry" title="Industry">${escapeHtml(industry)}</span>`;
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
        statusPill.innerHTML = `Request Pending`;
      } else if (latestReq.status === "ACCEPTED") {
        statusPill.innerHTML = `Approved · Connected`;
      } else if (latestReq.status === "REJECTED") {
        statusPill.innerHTML = `Request Declined`;
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
            <div class="modal-status-title">Mentorship Request Under Review</div>
            <div>Your previous request sent on <strong>${escapeHtml(latestReq.createdAt || "recently")}</strong> is currently waiting for <strong>${escapeHtml(mentor.fullName)}</strong> to review and respond.</div>
          `;
          if (els.modalSubmitBtn) {
            els.modalSubmitBtn.disabled = true;
            els.modalSubmitBtn.textContent = "Request Already Pending";
          }
        } else if (latestReq.status === "ACCEPTED") {
          els.connectPreviousStatus.innerHTML = `
            <div class="modal-status-title">Previous Request Approved</div>
            <div><strong>${escapeHtml(mentor.fullName)}</strong> accepted your mentorship connection!</div>
            ${latestReq.mentorResponse ? `<div class="modal-status-note"><strong>Mentor's Response:</strong> "${escapeHtml(latestReq.mentorResponse)}"</div>` : ""}
            <div style="margin-top: 8px; font-size: 12px; opacity: 0.9;">You can submit another request below to schedule your next session or discuss a new topic.</div>
          `;
          if (els.modalSubmitBtn) {
            els.modalSubmitBtn.disabled = false;
            els.modalSubmitBtn.textContent = "Send Follow-up Request";
          }
        } else if (latestReq.status === "REJECTED") {
          els.connectPreviousStatus.innerHTML = `
            <div class="modal-status-title">Previous Request Declined</div>
            <div><strong>${escapeHtml(mentor.fullName)}</strong> was unable to accept your previous request.</div>
            ${latestReq.mentorResponse ? `<div class="modal-status-note"><strong>Mentor's Note:</strong> "${escapeHtml(latestReq.mentorResponse)}"</div>` : ""}
            <div style="margin-top: 8px; font-size: 12px; opacity: 0.9;">You are welcome to submit a revised request below with an updated topic or questions.</div>
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

    if (connectMsg.length < 10) {
      if (els.connectAlert) {
        els.connectAlert.className = "form-status-alert error";
        els.connectAlert.textContent = "Message must be at least 10 characters.";
        els.connectAlert.style.display = "block";
      }
      return;
    }

    if (!state.currentUser || !state.currentUser.id) {
      handleUnauthorized("Please sign in to request a mentorship session.");
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

      if (res.status === 401) {
        handleUnauthorized("Your session has expired. Please sign in again.");
        return;
      }

      if (res.ok && res.data && res.data.success) {
        els.connectForm.style.display = "none";
        if (els.connectPreviousStatus) els.connectPreviousStatus.style.display = "none";
        const mentorName = state.selectedMentor ? state.selectedMentor.fullName : "your mentor";
        els.connectSuccess.innerHTML = `
          Mentorship request sent successfully!
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
        let errMsg = "Failed to send request";
        if (res.data) {
          if (res.data.errors && typeof res.data.errors === "object") {
            errMsg = Object.values(res.data.errors).join(". ");
          } else if (res.data.message) {
            errMsg = res.data.message;
          }
        } else if (res.status) {
          errMsg += ` (HTTP ${res.status})`;
        }
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
    const role = (state.currentUser.role || "").toLowerCase();
    const isAlumni = role === "alumni";
    const isStudent = role === "student";
    if (!isAlumni && !isStudent) {
      els.navRequestsBadge.style.display = "none";
      return;
    }
    try {
      const res = isAlumni
        ? await ApiClient.getRequestsByMentor(state.currentUser.id)
        : await ApiClient.getRequestsByStudent(state.currentUser.id);
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

    const role = (state.currentUser.role || "").toLowerCase();
    const isMentor = role === "alumni";
    const isStudent = role === "student";

    if (!isMentor && !isStudent) {
      switchView("search");
      return;
    }

    if (els.requestsAlert) els.requestsAlert.style.display = "none";
    els.requestsList.innerHTML = "<div style='text-align:center; padding: 40px; color:#64748b;'>Loading requests...</div>";
    els.requestsEmptyState.style.display = "none";

    if (isMentor) {
      els.requestsTitle.textContent = "Received Mentorship Requests";
      els.requestsSubtitle.textContent = "Review students who have requested to connect with you, accept sessions, or decline.";
      els.emptyRequestsTitle.textContent = "No Requests Received Yet";
      els.emptyRequestsMsg.textContent = "When students discover your profile and request mentorship, their applications will show up here.";
    } else {
      els.requestsTitle.textContent = "My Mentorship Requests";
      els.requestsSubtitle.textContent = "Track requests you have sent to alumni mentors, check statuses, and manage sessions.";
      els.emptyRequestsTitle.textContent = "No Mentorship Requests Sent Yet";
      els.emptyRequestsMsg.textContent = "Browse our alumni mentor directory and send connection requests to start your mentorship journey!";
    }

    try {
      const res = isMentor
        ? await ApiClient.getRequestsByMentor(state.currentUser.id)
        : await ApiClient.getRequestsByStudent(state.currentUser.id);

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

      // Load scheduled 1-on-1 sessions
      loadAndRenderScheduledSessions();
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
      PENDING: "Pending",
      ACCEPTED: "Accepted",
      REJECTED: "Declined",
      CANCELLED: "Cancelled"
    };
    statusBadge.textContent = statusIcons[st] || st;

    header.appendChild(personInfo);
    header.appendChild(statusBadge);
    card.appendChild(header);

    // Status Tracking Stepper (Team 1)
    card.appendChild(createStatusStepper(st));

    // Session Goal Pill
    const goalPill = document.createElement("div");
    goalPill.className = "request-goal-pill";
    goalPill.innerHTML = `<span>${escapeHtml(req.sessionGoal || "Mentorship")}</span>`;
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
    } else if (st === "ACCEPTED") {
      // Team 1: Schedule Session Action when request is accepted
      const scheduleBtn = document.createElement("button");
      scheduleBtn.type = "button";
      scheduleBtn.className = "btn-req-schedule";
      scheduleBtn.innerHTML = "Schedule Session";
      scheduleBtn.addEventListener("click", () => openScheduleModal(req, isMentor));
      actionBtns.appendChild(scheduleBtn);
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
            setProfileAlert("info", "Saving image directly to MySQL / MariaDB database...");
            const res = await ApiClient.uploadAvatar(state.currentUser.id, dataUrl, "image/jpeg");
            if (res.ok && res.data.success) {
              const url = `${res.data.avatarUrl}?t=${Date.now()}`;
              state.currentUser.avatarUrl = url;
              saveSession(state.currentUser);
              els.profileAvatarLg.innerHTML = `<img src="${url}" alt="Avatar" />`;
              if (els.removeAvatarBtn) els.removeAvatarBtn.style.display = "flex";
              if (els.pe_avatar_remove) els.pe_avatar_remove.style.display = "inline-flex";
              updateAuthHeader();
              setProfileAlert("success", "Profile picture saved in MySQL / MariaDB database.");
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
    if (els.openDirectScheduleBtn) {
      els.openDirectScheduleBtn.addEventListener("click", () => {
        openScheduleModal();
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

    // Session History Tab & Controls (Team B & C)
    if (els.tabHistoryBtn) {
      els.tabHistoryBtn.addEventListener("click", () => switchView("history"));
    }
    if (els.refreshHistoryBtn) {
      els.refreshHistoryBtn.addEventListener("click", () => loadAndRenderHistory());
    }
    if (els.historySortSelect) {
      els.historySortSelect.addEventListener("change", () => loadAndRenderHistory());
    }
    if (els.historyStatusFilter) {
      els.historyStatusFilter.addEventListener("change", (e) => {
        const filter = e.target.value;
        const allItems = state.historyItems || [];
        if (filter === "ALL") {
          renderHistoryItems(allItems);
        } else {
          renderHistoryItems(allItems.filter(it => (it.status || "").toUpperCase() === filter));
        }
      });
    }

    // Admin Console Controls
    if (els.tabAdminBtn) {
      els.tabAdminBtn.addEventListener("click", () => switchView("admin"));
    }
    if (els.refreshAdminBtn) {
      els.refreshAdminBtn.addEventListener("click", () => loadAndRenderAdminUsers());
    }
    if (els.adminRoleFilter) {
      els.adminRoleFilter.addEventListener("change", () => loadAndRenderAdminUsers());
    }
    if (els.adminStatusFilter) {
      els.adminStatusFilter.addEventListener("change", () => loadAndRenderAdminUsers());
    }
    if (els.adminSearchInput) {
      els.adminSearchInput.addEventListener("input", () => filterAdminUsersTable());
    }

    // Admin Subtabs (Team C & A)
    if (els.adminSubtabUsersBtn) {
      els.adminSubtabUsersBtn.addEventListener("click", () => switchAdminSubtab("users"));
    }
    if (els.adminSubtabPendingBtn) {
      els.adminSubtabPendingBtn.addEventListener("click", () => switchAdminSubtab("pending"));
    }
    if (els.adminSubtabDuplicatesBtn) {
      els.adminSubtabDuplicatesBtn.addEventListener("click", () => switchAdminSubtab("duplicates"));
    }
    if (els.adminSubtabFeedbackBtn) {
      els.adminSubtabFeedbackBtn.addEventListener("click", () => switchAdminSubtab("feedback"));
    }
    if (els.adminSubtabEventsBtn) {
      els.adminSubtabEventsBtn.addEventListener("click", () => switchAdminSubtab("events"));
    }
    if (els.refreshAdminEventsBtn) {
      els.refreshAdminEventsBtn.addEventListener("click", () => loadAndRenderAdminEvents());
    }

    // Student Public Meets Actions
    if (els.refreshStudentPublicMeetsBtn) {
      els.refreshStudentPublicMeetsBtn.addEventListener("click", loadStudentPublicMeets);
    }

    // Teacher / Faculty Hub Subtabs & Actions
    if (els.tabTeacherBtn) {
      els.tabTeacherBtn.addEventListener("click", () => switchView("teacher"));
    }
    if (els.teacherSubtabSchedulesBtn) {
      els.teacherSubtabSchedulesBtn.addEventListener("click", () => switchTeacherSubtab("schedules"));
    }
    if (els.teacherSubtabOneOnOneBtn) {
      els.teacherSubtabOneOnOneBtn.addEventListener("click", () => switchTeacherSubtab("oneOnOne"));
    }
    if (els.teacherSubtabPublicMeetBtn) {
      els.teacherSubtabPublicMeetBtn.addEventListener("click", () => switchTeacherSubtab("publicMeet"));
    }
    if (els.teacherSubtabNewMentorBtn) {
      els.teacherSubtabNewMentorBtn.addEventListener("click", () => switchTeacherSubtab("newMentor"));
    }
    if (els.teacherSubtabDirectoryBtn) {
      els.teacherSubtabDirectoryBtn.addEventListener("click", () => switchTeacherSubtab("directory"));
    }

    if (els.filterMeetType) {
      els.filterMeetType.addEventListener("change", loadTeacherMeets);
    }
    if (els.filterMeetStatus) {
      els.filterMeetStatus.addEventListener("change", loadTeacherMeets);
    }
    if (els.refreshTeacherMeetsBtn) {
      els.refreshTeacherMeetsBtn.addEventListener("click", loadTeacherMeets);
    }
    if (els.quickOneOnOneBtn) {
      els.quickOneOnOneBtn.addEventListener("click", () => switchTeacherSubtab("oneOnOne"));
    }
    if (els.quickPublicMeetBtn) {
      els.quickPublicMeetBtn.addEventListener("click", () => switchTeacherSubtab("publicMeet"));
    }

    if (els.scheduleOneOnOneForm) {
      els.scheduleOneOnOneForm.addEventListener("submit", handleScheduleOneOnOneSubmit);
    }
    if (els.schedulePublicMeetForm) {
      els.schedulePublicMeetForm.addEventListener("submit", handleSchedulePublicMeetSubmit);
    }
    if (els.onboardMentorForm) {
      els.onboardMentorForm.addEventListener("submit", handleOnboardMentorSubmit);
    }
    if (els.refreshTeacherDirectoryBtn) {
      els.refreshTeacherDirectoryBtn.addEventListener("click", loadTeacherDirectory);
    }

    if (els.meetAttendeesCloseBtn) {
      els.meetAttendeesCloseBtn.addEventListener("click", closeMeetAttendeesModal);
    }
    if (els.meetAttendeesDoneBtn) {
      els.meetAttendeesDoneBtn.addEventListener("click", closeMeetAttendeesModal);
    }
    if (els.meetAttendeesModal) {
      els.meetAttendeesModal.addEventListener("click", (e) => {
        if (e.target === els.meetAttendeesModal) closeMeetAttendeesModal();
      });
    }

    if (els.refreshPendingBtn) {
      els.refreshPendingBtn.addEventListener("click", () => loadAndRenderPendingUsers());
    }
    if (els.runDuplicateScanBtn) {
      els.runDuplicateScanBtn.addEventListener("click", () => loadAndRenderDuplicates());
    }
    if (els.checkCandidateDupBtn) {
      els.checkCandidateDupBtn.addEventListener("click", handleCandidateDuplicateCheck);
    }
    if (els.refreshFeedbackBtn) {
      els.refreshFeedbackBtn.addEventListener("click", () => loadAndRenderAdminFeedback());
    }

    // Delete User Modal Events
    if (els.deleteUserModalCloseBtn) {
      els.deleteUserModalCloseBtn.addEventListener("click", closeDeleteUserModal);
    }
    if (els.deleteUserCancelBtn) {
      els.deleteUserCancelBtn.addEventListener("click", closeDeleteUserModal);
    }
    if (els.deleteUserModal) {
      els.deleteUserModal.addEventListener("click", (e) => {
        if (e.target === els.deleteUserModal) closeDeleteUserModal();
      });
    }
    if (els.confirmDeleteUserBtn) {
      els.confirmDeleteUserBtn.addEventListener("click", handleConfirmDeleteUser);
    }

    // Feedback Form Modal Events (Team C)
    if (els.feedbackModalCloseBtn) {
      els.feedbackModalCloseBtn.addEventListener("click", closeFeedbackModal);
    }
    if (els.feedbackCancelBtn) {
      els.feedbackCancelBtn.addEventListener("click", closeFeedbackModal);
    }
    if (els.feedbackModal) {
      els.feedbackModal.addEventListener("click", (e) => {
        if (e.target === els.feedbackModal) closeFeedbackModal();
      });
    }
    if (els.feedbackForm) {
      els.feedbackForm.addEventListener("submit", handleFeedbackSubmit);
    }
    if (els.feedbackComments && els.feedbackCharCount) {
      els.feedbackComments.addEventListener("input", () => {
        const len = els.feedbackComments.value.trim().length;
        els.feedbackCharCount.textContent = `${len} chars`;
      });
    }

    // Star rating buttons inside feedback modal
    const starRatingLabels = {
      1: "★ Poor (1/5)",
      2: "★★ Fair (2/5)",
      3: "★★★ Good (3/5)",
      4: "★★★★ Very Good (4/5)",
      5: "★★★★★ Outstanding / Exceptional (5/5)"
    };
    if (els.starRatingWidget) {
      const starBtns = els.starRatingWidget.querySelectorAll(".star-rating-btn");
      starBtns.forEach(btn => {
        const val = parseInt(btn.getAttribute("data-value"), 10);
        btn.addEventListener("mouseenter", () => {
          starBtns.forEach(b => {
            const bVal = parseInt(b.getAttribute("data-value"), 10);
            b.classList.toggle("hover-active", bVal <= val);
          });
        });
        btn.addEventListener("mouseleave", () => {
          starBtns.forEach(b => b.classList.remove("hover-active"));
        });
        btn.addEventListener("click", () => {
          if (els.feedbackRatingValue) els.feedbackRatingValue.value = val;
          if (els.starRatingLabel) els.starRatingLabel.textContent = starRatingLabels[val] || `${val}/5 Stars`;
          starBtns.forEach(b => {
            const bVal = parseInt(b.getAttribute("data-value"), 10);
            b.classList.toggle("active", bVal <= val);
          });
        });
      });
    }

    // Session Scheduling Modal (Team 1)
    if (els.scheduleModalCloseBtn) {
      els.scheduleModalCloseBtn.addEventListener("click", closeScheduleModal);
    }
    if (els.scheduleCancelBtn) {
      els.scheduleCancelBtn.addEventListener("click", closeScheduleModal);
    }
    if (els.scheduleModal) {
      els.scheduleModal.addEventListener("click", (e) => {
        if (e.target === els.scheduleModal) closeScheduleModal();
      });
    }
    if (els.scheduleForm) {
      els.scheduleForm.addEventListener("submit", handleScheduleSubmit);
    }

    // Time chip presets
    document.querySelectorAll(".btn-time-chip").forEach(chip => {
      chip.addEventListener("click", (e) => {
        document.querySelectorAll(".btn-time-chip").forEach(c => c.classList.remove("selected"));
        e.target.classList.add("selected");
        if (els.scheduleTime) {
          els.scheduleTime.value = e.target.getAttribute("data-time") || "";
        }
      });
    });
  }

  // =========================================================================
  // Team 1: Status Tracking Flow Stepper Generator
  // =========================================================================
  function createStatusStepper(status) {
    const box = document.createElement("div");
    box.className = "tracker-steps";
    box.style.padding = "10px 14px";
    box.style.background = "#f8fafc";
    box.style.borderRadius = "8px";
    box.style.marginBottom = "10px";

    const s = (status || "PENDING").toUpperCase();
    const isAccepted = s === "ACCEPTED" || s === "SCHEDULED" || s === "COMPLETED";
    const isScheduled = s === "SCHEDULED" || s === "COMPLETED";
    const isCompleted = s === "COMPLETED";
    const isDeclined = s === "REJECTED" || s === "CANCELLED";

    box.innerHTML = `
      <div class="tracker-step done">
        <span class="step-num">✓</span>
        <span class="step-title">Sent</span>
      </div>
      <div class="tracker-line ${s === 'PENDING' ? 'active' : (isAccepted ? 'done' : '')}"></div>
      <div class="tracker-step ${s === 'PENDING' ? 'active' : (isAccepted ? 'done' : (isDeclined ? 'declined' : ''))}">
        <span class="step-num">${isAccepted ? '✓' : (isDeclined ? '✕' : '2')}</span>
        <span class="step-title">${isDeclined ? 'Declined' : 'Review'}</span>
      </div>
      <div class="tracker-line ${isScheduled ? 'done' : (s === 'ACCEPTED' ? 'active' : '')}"></div>
      <div class="tracker-step ${isAccepted ? (isScheduled ? 'done' : 'active') : ''}">
        <span class="step-num">${isScheduled ? '✓' : '3'}</span>
        <span class="step-title">Approved</span>
      </div>
      <div class="tracker-line ${isCompleted ? 'done' : (s === 'SCHEDULED' ? 'active' : '')}"></div>
      <div class="tracker-step ${isScheduled ? (isCompleted ? 'done' : 'active') : ''}">
        <span class="step-num">${isCompleted ? '✓' : '4'}</span>
        <span class="step-title">${isCompleted ? 'Completed' : 'Schedule'}</span>
      </div>
    `;
    return box;
  }

  // =========================================================================
  // Team 1: Session Scheduling Modal & Session Management
  // =========================================================================
  function openScheduleModal(req, isMentor) {
    if (!els.scheduleModal) return;

    const partnerCard = els.scheduleModal.querySelector(".schedule-partner-card");

    if (req) {
      if (els.scheduleMentorSelectGroup) els.scheduleMentorSelectGroup.style.display = "none";
      if (partnerCard) partnerCard.style.display = "flex";

      els.scheduleRequestId.value = req.id || "";
      els.scheduleMentorId.value = req.mentorId || "";
      els.scheduleStudentId.value = req.studentId || "";

      const partnerName = isMentor ? (req.studentName || "Student") : (req.mentorName || "Alumni Mentor");
      const partnerSub = isMentor
        ? `${req.studentDepartment || "Engineering"}`
        : `${req.mentorDesignation || "Engineer"} at ${req.mentorCompany || "Company"}`;

      els.schedulePartnerName.textContent = partnerName;
      els.schedulePartnerSub.textContent = partnerSub;
      els.scheduleGoalText.textContent = `Goal: ${req.sessionGoal || "Career Mentorship"}`;
      els.schedulePartnerAvatar.textContent = partnerName.charAt(0).toUpperCase();
    } else {
      // Direct scheduling: show mentor dropdown
      if (els.scheduleMentorSelectGroup) els.scheduleMentorSelectGroup.style.display = "block";
      if (partnerCard) partnerCard.style.display = "none";

      els.scheduleRequestId.value = "";
      els.scheduleMentorId.value = "";
      els.scheduleStudentId.value = state.currentUser ? state.currentUser.id : "";

      // Populate mentor dropdown from state.mentors
      if (els.scheduleMentorSelect) {
        els.scheduleMentorSelect.innerHTML = '<option value="">-- Choose an alumni mentor --</option>';
        if (state.mentors && state.mentors.length > 0) {
          state.mentors.forEach(m => {
            const opt = document.createElement("option");
            opt.value = m.id;
            opt.textContent = `${m.fullName} — ${m.designation || 'Engineer'} (${m.company || 'Tech'})`;
            els.scheduleMentorSelect.appendChild(opt);
          });
        }
      }
    }

    // Default date to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const yyyy = tomorrow.getFullYear();
    const mm = String(tomorrow.getMonth() + 1).padStart(2, "0");
    const dd = String(tomorrow.getDate()).padStart(2, "0");
    els.scheduleDate.min = `${yyyy}-${mm}-${dd}`;
    els.scheduleDate.value = `${yyyy}-${mm}-${dd}`;
    els.scheduleTime.value = "14:00";
    els.scheduleMeetingLink.value = "https://meet.google.com/xyz-uvwx-rst";

    setScheduleAlert(null);
    els.scheduleModal.classList.add("active");
  }

  function closeScheduleModal() {
    if (els.scheduleModal) els.scheduleModal.classList.remove("active");
  }

  function setScheduleAlert(type, msg) {
    if (!els.scheduleAlert) return;
    if (!type || !msg) {
      els.scheduleAlert.style.display = "none";
      els.scheduleAlert.textContent = "";
      els.scheduleAlert.className = "form-status-alert";
      return;
    }
    els.scheduleAlert.className = `form-status-alert alert-${type}`;
    els.scheduleAlert.textContent = msg;
    els.scheduleAlert.style.display = "block";
  }

  async function handleScheduleSubmit(e) {
    e.preventDefault();
    setScheduleAlert(null);

    const requestId = els.scheduleRequestId.value;
    let mentorId = els.scheduleMentorId.value;
    const date = els.scheduleDate.value;
    const time = els.scheduleTime.value;
    const duration = parseInt(els.scheduleDuration.value, 10) || 60;
    const mode = els.scheduleMode.value;
    const meetingLink = els.scheduleMeetingLink.value.trim();
    const notes = els.scheduleNotes.value.trim();

    let mentorName = els.schedulePartnerName.textContent;
    let mentorCompany = els.schedulePartnerSub.textContent;

    if (!mentorId && els.scheduleMentorSelect && els.scheduleMentorSelect.value) {
      mentorId = els.scheduleMentorSelect.value;
      const opt = els.scheduleMentorSelect.options[els.scheduleMentorSelect.selectedIndex];
      if (opt) {
        mentorName = opt.textContent.split("—")[0].trim();
        mentorCompany = opt.textContent.includes("(") ? opt.textContent.split("(")[1].replace(")", "").trim() : "Alumni";
      }
    }

    if (!mentorId) {
      setScheduleAlert("error", "Please select a mentor to schedule the session with.");
      return;
    }

    if (!date || !time) {
      setScheduleAlert("error", "Please select a valid session date and time.");
      return;
    }

    els.scheduleSubmitBtn.disabled = true;
    els.scheduleSubmitBtn.textContent = "Scheduling...";

    const newSession = {
      id: Date.now(),
      requestId: requestId ? parseInt(requestId, 10) : null,
      mentorId: parseInt(mentorId, 10),
      mentorName: mentorName || "Alumni Mentor",
      mentorCompany: mentorCompany || "Partner Company",
      date,
      time,
      duration,
      mode,
      meetingLink,
      notes,
      status: "SCHEDULED",
      createdAt: new Date().toISOString()
    };

    try {
      // Save locally and in session_history
      const stored = JSON.parse(localStorage.getItem("alumniConnectSessions") || "[]");
      stored.unshift(newSession);
      localStorage.setItem("alumniConnectSessions", JSON.stringify(stored));

      setScheduleAlert("success", "Mentoring session scheduled successfully.");

      setTimeout(() => {
        closeScheduleModal();
        loadAndRenderScheduledSessions();
        if (state.activeView === "requests") {
          loadAndRenderRequests();
        }
      }, 700);

    } catch (err) {
      setScheduleAlert("error", "Failed to schedule session: " + err.message);
    } finally {
      els.scheduleSubmitBtn.disabled = false;
      els.scheduleSubmitBtn.textContent = "Confirm & Schedule";
    }
  }

  function loadAndRenderScheduledSessions() {
    if (!els.scheduledSessionsContainer || !els.scheduledSessionsList) return;
    const stored = JSON.parse(localStorage.getItem("alumniConnectSessions") || "[]");

    if (stored.length === 0) {
      els.scheduledSessionsContainer.style.display = "none";
      return;
    }

    els.scheduledSessionsContainer.style.display = "block";
    els.scheduledSessionsList.innerHTML = "";

    stored.forEach(sess => {
      const card = document.createElement("div");
      card.className = "session-card";
      card.innerHTML = `
        <div class="session-card-header">
          <div class="session-date-badge">
            <strong>Date:</strong> ${escapeHtml(sess.date)} at ${escapeHtml(sess.time)}
          </div>
          <span class="session-status-badge">Confirmed</span>
        </div>
        <div class="session-meta-row">
          <strong>Mentor:</strong> ${escapeHtml(sess.mentorName || "Mentor")} (${escapeHtml(sess.mentorCompany || "")})
        </div>
        <div class="session-meta-row">
          Duration: ${sess.duration} mins · Mode: ${escapeHtml(sess.mode || "Online")}
        </div>
        ${sess.notes ? `<div style="font-size:12px;color:#64748b;font-style:italic;">"${escapeHtml(sess.notes)}"</div>` : ""}
        ${sess.meetingLink ? `
          <a href="${escapeHtml(sess.meetingLink)}" target="_blank" rel="noopener noreferrer" class="session-link-btn">
            Join Meeting Room
          </a>
        ` : ""}
      `;
      els.scheduledSessionsList.appendChild(card);
    });
  }

  // =========================================================================
  // Team B & C: Session History View & Sorting Controllers
  // =========================================================================
  async function loadAndRenderHistory() {
    if (!state.currentUser) return;
    setHistoryAlert(null);

    const sortMetric = els.historySortSelect ? els.historySortSelect.value : "chronological";

    if (els.historyListContainer) {
      els.historyListContainer.innerHTML = `<div style="text-align:center;padding:24px;color:#64748b;">Loading session history via manual ${escapeHtml(sortMetric)} sort...</div>`;
    }

    try {
      const res = await ApiClient.getSessionHistory({ sort: sortMetric });

      if (res.ok && Array.isArray(res.data)) {
        state.historyItems = res.data;

        if (els.historyItemCount) {
          els.historyItemCount.textContent = `${res.data.length} Records`;
        }
        if (els.historyExecTime) {
          els.historyExecTime.textContent = `< 2ms`;
        }

        renderHistoryItems(res.data);
      } else {
        setHistoryAlert("error", res.data?.message || "Failed to load session history.");
        if (els.historyEmptyState) els.historyEmptyState.style.display = "block";
      }
    } catch (err) {
      console.error("Session history fetch error:", err);
      setHistoryAlert("error", "Unable to connect to Java backend to retrieve session history.");
    }
  }

  function renderHistoryItems(items) {
    if (!els.historyListContainer) return;
    els.historyListContainer.innerHTML = "";

    if (!items || items.length === 0) {
      if (els.historyEmptyState) els.historyEmptyState.style.display = "block";
      return;
    }

    if (els.historyEmptyState) els.historyEmptyState.style.display = "none";

    items.forEach(it => {
      const card = document.createElement("div");
      card.className = "history-card";

      const isCompleted = (status === "completed");

      card.innerHTML = `
        <div class="history-card-header">
          <div class="history-title">
            Session #${it.id} · Student ID ${it.studentId} & Alumni Mentor ${it.alumniId}
          </div>
          <div class="history-badges">
            <span class="history-badge priority-${escapeHtml(priority)}">${escapeHtml(it.priority || "MEDIUM")} Priority</span>
            <span class="history-badge status-${escapeHtml(status)}">${escapeHtml(it.status || "STARTED")}</span>
          </div>
        </div>
        <div class="history-meta-grid">
          <div class="history-meta-item">
            <strong>Duration:</strong> ${it.durationMinutes || 0} minutes
          </div>
          <div class="history-meta-item">
            <strong>Started:</strong> ${it.startedAt || it.createdAt || "Recorded"}
          </div>
          <div class="history-meta-item">
            <strong>Completed:</strong> ${it.completedAt || "In progress / Pending"}
          </div>
          ${it.failureReason ? `<div class="history-meta-item" style="color:#b91c1c;"><strong>Reason:</strong> ${escapeHtml(it.failureReason)}</div>` : ""}
        </div>
        ${isCompleted ? `
          <div style="margin-top:12px; padding-top:10px; border-top:1px solid #f1f5f9; display:flex; justify-content:space-between; align-items:center;">
            <span style="font-size:12px; color:#64748b;">Session completed</span>
            <button type="button" class="btn-give-feedback" data-sessionid="${it.id}">
              Rate & Review Session
            </button>
          </div>
        ` : ""}
      `;

      if (isCompleted) {
        const fbBtn = card.querySelector(".btn-give-feedback");
        if (fbBtn) {
          fbBtn.addEventListener("click", () => openFeedbackModal(it));
        }
      }

      els.historyListContainer.appendChild(card);
    });
  }

  function openFeedbackModal(session) {
    if (!els.feedbackModal) return;
    setFeedbackAlert(null);

    if (els.feedbackSessionId) els.feedbackSessionId.value = session.id;
    if (els.feedbackSessionInfo) {
      els.feedbackSessionInfo.innerHTML = `
        <span><strong>Session #${session.id}</strong> with Mentor #${session.alumniId} · Completed Duration: ${session.durationMinutes || 0}m</span>
      `;
    }

    // Default to 5 stars
    if (els.feedbackRatingValue) els.feedbackRatingValue.value = "5";
    if (els.starRatingLabel) els.starRatingLabel.textContent = "Outstanding / Exceptional (5/5)";
    if (els.starRatingWidget) {
      els.starRatingWidget.querySelectorAll(".star-rating-btn").forEach(btn => btn.classList.add("active"));
    }

    if (els.feedbackComments) {
      els.feedbackComments.value = "";
    }
    if (els.feedbackCharCount) {
      els.feedbackCharCount.textContent = "0 chars";
    }

    els.feedbackModal.style.display = "flex";
  }

  function closeFeedbackModal() {
    if (els.feedbackModal) {
      els.feedbackModal.style.display = "none";
    }
  }

  async function handleFeedbackSubmit(e) {
    e.preventDefault();
    setFeedbackAlert(null);

    const sessionId = parseInt(els.feedbackSessionId.value, 10);
    const rating = parseInt(els.feedbackRatingValue.value, 10);
    const comments = (els.feedbackComments ? els.feedbackComments.value : "").trim();

    if (!sessionId) {
      setFeedbackAlert("error", "Invalid session reference.");
      return;
    }
    if (isNaN(rating) || rating < 1 || rating > 5) {
      setFeedbackAlert("error", "Please select a rating between 1 and 5 stars.");
      return;
    }
    if (comments.length < 5) {
      setFeedbackAlert("error", "Please enter comments of at least 5 characters.");
      return;
    }

    const btn = els.feedbackSubmitBtn;
    if (btn) {
      btn.disabled = true;
      btn.textContent = "Submitting...";
    }

    try {
      const res = await ApiClient.submitFeedback({ sessionId, rating, comments });
      if (res.ok) {
        closeFeedbackModal();
        setHistoryAlert("success", "Thank you! Your feedback has been recorded successfully.");
        loadAndRenderHistory();
      } else {
        setFeedbackAlert("error", res.data?.message || "Failed to submit feedback.");
      }
    } catch (err) {
      setFeedbackAlert("error", "Network error submitting feedback: " + err.message);
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.textContent = "Submit Feedback";
      }
    }
  }

  function setFeedbackAlert(type, msg) {
    if (!els.feedbackAlert) return;
    if (!type || !msg) {
      els.feedbackAlert.style.display = "none";
      els.feedbackAlert.textContent = "";
      els.feedbackAlert.className = "form-status-alert";
      return;
    }
    els.feedbackAlert.className = `form-status-alert alert-${type}`;
    els.feedbackAlert.textContent = msg;
    els.feedbackAlert.style.display = "block";
  }

  function setHistoryAlert(type, msg) {
    if (!els.historyAlert) return;
    if (!type || !msg) {
      els.historyAlert.style.display = "none";
      els.historyAlert.textContent = "";
      els.historyAlert.className = "form-status-alert";
      return;
    }
    els.historyAlert.className = `form-status-alert alert-${type}`;
    els.historyAlert.textContent = msg;
    els.historyAlert.style.display = "block";
  }

  // =========================================================================
  // Admin Console Controller
  // =========================================================================
  async function loadAndRenderAdminUsers() {
    if (!state.currentUser || (state.currentUser.role || "").toLowerCase() !== "admin") return;
    setAdminAlert(null);

    const role = els.adminRoleFilter ? els.adminRoleFilter.value : "";
    const active = els.adminStatusFilter ? els.adminStatusFilter.value : "";

    if (els.adminUsersTableBody) {
      els.adminUsersTableBody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;color:#64748b;">Loading platform users from database...</td></tr>`;
    }

    try {
      const res = await ApiClient.getAdminUsers({ role, active });
      if (res.ok && res.data && Array.isArray(res.data.users)) {
        state.adminUsers = res.data.users;
        updateAdminStats(res.data.users);
        renderAdminUsers(res.data.users);
      } else {
        setAdminAlert("error", res.data?.message || "Failed to load platform users.");
      }
    } catch (err) {
      console.error("Admin users fetch error:", err);
      setAdminAlert("error", "Unable to connect to Java backend to retrieve users.");
    }
  }

  function updateAdminStats(users) {
    if (!users) return;
    const total = users.length;
    let students = 0, alumni = 0, teachers = 0, inactive = 0;
    users.forEach(u => {
      const r = (u.role || "").toLowerCase();
      if (r === "student") students++;
      else if (r === "alumni") alumni++;
      else if (r === "teacher") teachers++;
      if (!u.active) inactive++;
    });

    if (els.adminTotalUsers) els.adminTotalUsers.textContent = total;
    if (els.adminTotalStudents) els.adminTotalStudents.textContent = students;
    if (els.adminTotalAlumni) els.adminTotalAlumni.textContent = alumni;
    if (els.adminTotalTeachers) els.adminTotalTeachers.textContent = teachers;
    if (els.adminTotalInactive) els.adminTotalInactive.textContent = inactive;
  }

  // =========================================================================
  // Admin Subtabs & Panels Controller (Team C & A)
  // =========================================================================
  function switchAdminSubtab(tab) {
    state.activeAdminSubtab = "users";
    if (els.adminPanelUsers) els.adminPanelUsers.style.display = "block";
    loadAndRenderAdminUsers();
  }

  function renderAdminUsers(users) {
    if (!els.adminUsersTableBody) return;
    els.adminUsersTableBody.innerHTML = "";

    if (!users || users.length === 0) {
      els.adminUsersTableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:24px;color:#64748b;">No matching users found in database.</td></tr>`;
      return;
    }

    users.forEach(u => {
      const tr = document.createElement("tr");
      const r = (u.role || "").toLowerCase();
      const isActive = !!u.active;
      const approval = (u.approvalStatus || "APPROVED").toUpperCase();

      tr.innerHTML = `
        <td style="font-weight:700; color:#64748b;">#${u.id}</td>
        <td>
          <div style="font-weight:700; color:#0f172a;">${escapeHtml(u.fullName || "User")}</div>
          <div style="font-size:12px; color:#64748b;">${escapeHtml(u.email || "")}</div>
        </td>
        <td>
          <span class="role-badge role-${escapeHtml(r)}">${escapeHtml(u.role || "student")}</span>
        </td>
        <td>${escapeHtml(u.mobileNumber || "—")}</td>
        <td>
          <span class="approval-badge approval-${escapeHtml(approval)}">
            ${escapeHtml(approval)}
          </span>
        </td>
        <td>
          <span class="status-indicator-badge ${isActive ? 'is-active' : 'is-inactive'}">
            <span class="status-dot"></span>
            ${isActive ? 'Active' : 'Deactivated'}
          </span>
        </td>
        <td style="font-size:12px; color:#64748b;">${escapeHtml(u.createdAt ? u.createdAt.split(" ")[0] : "—")}</td>
        <td style="text-align:right;">
          ${u.id === state.currentUser.id 
            ? '<span style="font-size:11px; color:#94a3b8; font-weight:600;">Current Admin</span>'
            : `<div class="admin-action-btn-group">
                <button type="button" class="btn-toggle-status ${isActive ? 'btn-deactivate' : 'btn-activate'}" data-userid="${u.id}">
                  ${isActive ? 'Deactivate' : 'Activate'}
                </button>
                <button type="button" class="btn-delete-user" data-userid="${u.id}" title="Permanently delete user">
                  Delete
                </button>
              </div>`
          }
        </td>
      `;

      const toggleBtn = tr.querySelector(".btn-toggle-status");
      if (toggleBtn) {
        toggleBtn.addEventListener("click", async () => {
          const newStatus = !isActive;
          toggleBtn.disabled = true;
          toggleBtn.textContent = "...";
          try {
            const res = await ApiClient.setUserActiveStatus(u.id, newStatus);
            if (res.ok) {
              setAdminAlert("success", `User #${u.id} (${u.fullName}) has been ${newStatus ? 'activated' : 'deactivated'}.`);
              loadAndRenderAdminUsers();
            } else {
              setAdminAlert("error", res.data?.message || "Failed to update user status.");
              toggleBtn.disabled = false;
            }
          } catch (e) {
            setAdminAlert("error", "Error updating status: " + e.message);
            toggleBtn.disabled = false;
          }
        });
      }

      const delBtn = tr.querySelector(".btn-delete-user");
      if (delBtn) {
        delBtn.addEventListener("click", () => openDeleteUserModal(u));
      }

      els.adminUsersTableBody.appendChild(tr);
    });
  }

  // Delete User Modal Functions (Team A)
  function openDeleteUserModal(user) {
    if (!els.deleteUserModal) return;
    state.userToDelete = user;
    if (els.deleteUserDetailsBox) {
      els.deleteUserDetailsBox.innerHTML = `
        <div style="font-weight:700; font-size:14px; margin-bottom:4px;">#${user.id} — ${escapeHtml(user.fullName)}</div>
        <div><strong>Email:</strong> ${escapeHtml(user.email)}</div>
        <div><strong>Role:</strong> ${escapeHtml(user.role)} · <strong>Mobile:</strong> ${escapeHtml(user.mobileNumber || "—")}</div>
      `;
    }
    els.deleteUserModal.style.display = "flex";
  }

  function closeDeleteUserModal() {
    if (els.deleteUserModal) els.deleteUserModal.style.display = "none";
    state.userToDelete = null;
  }

  async function handleConfirmDeleteUser() {
    if (!state.userToDelete) return;
    const u = state.userToDelete;
    const btn = els.confirmDeleteUserBtn;
    if (btn) {
      btn.disabled = true;
      btn.textContent = "Deleting...";
    }
    try {
      const res = await ApiClient.deleteAdminUser(u.id);
      if (res.ok) {
        closeDeleteUserModal();
        setAdminAlert("success", `User #${u.id} (${u.fullName}) has been permanently deleted.`);
        loadAndRenderAdminUsers();
      } else {
        setAdminAlert("error", res.data?.message || "Failed to delete user.");
      }
    } catch (e) {
      setAdminAlert("error", "Error deleting user: " + e.message);
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.textContent = "Yes, Permanently Delete";
      }
    }
  }

  // Registration Approvals Controller (Team A)
  async function loadAndRenderPendingUsers() {
    if (!els.adminPendingTableBody) return;
    els.adminPendingTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:24px;color:#64748b;">Checking pending user registrations...</td></tr>`;

    try {
      const res = await ApiClient.getAdminUsers({ approval: "PENDING" });
      if (res.ok && res.data && Array.isArray(res.data.users)) {
        const pendingUsers = res.data.users;
        if (els.adminPendingBadge) {
          els.adminPendingBadge.textContent = pendingUsers.length;
          els.adminPendingBadge.style.display = pendingUsers.length > 0 ? "inline-block" : "none";
        }
        renderPendingUsers(pendingUsers);
      } else {
        els.adminPendingTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:24px;color:#dc2626;">Failed to load pending users.</td></tr>`;
      }
    } catch (err) {
      console.error("Pending users error:", err);
      els.adminPendingTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:24px;color:#dc2626;">Network error loading pending registrations.</td></tr>`;
    }
  }

  function renderPendingUsers(users) {
    if (!els.adminPendingTableBody) return;
    els.adminPendingTableBody.innerHTML = "";

    if (!users || users.length === 0) {
      els.adminPendingTableBody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align:center;padding:32px;color:#64748b;">
            No pending registrations. All user accounts are approved and verified.
          </td>
        </tr>
      `;
      return;
    }

    users.forEach(u => {
      const tr = document.createElement("tr");
      const r = (u.role || "").toLowerCase();

      tr.innerHTML = `
        <td style="font-weight:700; color:#64748b;">#${u.id}</td>
        <td>
          <div style="font-weight:700; color:#0f172a;">${escapeHtml(u.fullName || "Applicant")}</div>
          <div style="font-size:12px; color:#64748b;">${escapeHtml(u.email || "")}</div>
        </td>
        <td>
          <span class="role-badge role-${escapeHtml(r)}">${escapeHtml(u.role || "student")}</span>
        </td>
        <td>${escapeHtml(u.mobileNumber || "—")}</td>
        <td style="font-size:12px; color:#64748b;">${escapeHtml(u.createdAt ? u.createdAt.split(" ")[0] : "—")}</td>
        <td style="text-align:right;">
          <div class="admin-action-btn-group">
            <button type="button" class="btn-approve-user" data-userid="${u.id}">
              ✓ Approve
            </button>
            <button type="button" class="btn-reject-user" data-userid="${u.id}">
              ✕ Reject
            </button>
            <button type="button" class="btn-toggle-status" data-userid="${u.id}" style="background:#f3e8ff; color:#7e22ce; border-color:#e9d5ff;" title="Check if candidate is duplicate">
              Audit
            </button>
          </div>
        </td>
      `;

      const appBtn = tr.querySelector(".btn-approve-user");
      if (appBtn) {
        appBtn.addEventListener("click", async () => {
          appBtn.disabled = true;
          appBtn.textContent = "...";
          try {
            const res = await ApiClient.approveAdminUser(u.id, true);
            if (res.ok) {
              setAdminAlert("success", `Registration for ${u.fullName} (#${u.id}) has been APPROVED.`);
              loadAndRenderPendingUsers();
              loadAndRenderAdminUsers();
            } else {
              setAdminAlert("error", res.data?.message || "Failed to approve registration.");
              appBtn.disabled = false;
            }
          } catch (e) {
            setAdminAlert("error", "Approval error: " + e.message);
            appBtn.disabled = false;
          }
        });
      }

      const rejBtn = tr.querySelector(".btn-reject-user");
      if (rejBtn) {
        rejBtn.addEventListener("click", async () => {
          rejBtn.disabled = true;
          rejBtn.textContent = "...";
          try {
            const res = await ApiClient.approveAdminUser(u.id, false);
            if (res.ok) {
              setAdminAlert("success", `Registration for ${u.fullName} (#${u.id}) has been REJECTED.`);
              loadAndRenderPendingUsers();
              loadAndRenderAdminUsers();
            } else {
              setAdminAlert("error", res.data?.message || "Failed to reject registration.");
              rejBtn.disabled = false;
            }
          } catch (e) {
            setAdminAlert("error", "Rejection error: " + e.message);
            rejBtn.disabled = false;
          }
        });
      }

      const auditBtn = tr.querySelector("button[title*='duplicate']");
      if (auditBtn) {
        auditBtn.addEventListener("click", () => {
          switchAdminSubtab("duplicates");
          if (els.manualDupName) els.manualDupName.value = u.fullName || "";
          if (els.manualDupEmail) els.manualDupEmail.value = u.email || "";
          if (els.manualDupMobile) els.manualDupMobile.value = u.mobileNumber || "";
          handleCandidateDuplicateCheck();
        });
      }

      els.adminPendingTableBody.appendChild(tr);
    });
  }

  // Duplicate Detection Controller (Team D)
  async function loadAndRenderDuplicates() {
    if (!els.adminDuplicatesContainer) return;
    els.adminDuplicatesContainer.innerHTML = `<div style="text-align:center;padding:28px;color:#64748b;">Running duplicate algorithms across normalized phone numbers, Student IDs, and Levenshtein name similarity...</div>`;

    const btn = els.runDuplicateScanBtn;
    if (btn) {
      btn.disabled = true;
      btn.textContent = "Auditing Duplicates...";
    }

    try {
      const res = await ApiClient.getAdminDuplicates();
      if (res.ok && res.data && Array.isArray(res.data.duplicates)) {
        const groups = res.data.duplicates;
        if (els.adminDuplicatesBadge) {
          els.adminDuplicatesBadge.textContent = groups.length;
          els.adminDuplicatesBadge.style.display = groups.length > 0 ? "inline-block" : "none";
        }
        renderDuplicateClusters(groups);
      } else {
        els.adminDuplicatesContainer.innerHTML = `<div style="text-align:center;padding:24px;color:#dc2626;">Failed to execute duplicate scan.</div>`;
      }
    } catch (err) {
      console.error("Duplicate scan error:", err);
      els.adminDuplicatesContainer.innerHTML = `<div style="text-align:center;padding:24px;color:#dc2626;">Network error running duplicate scan.</div>`;
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.textContent = "Run System Duplicate Audit";
      }
    }
  }

  function renderDuplicateClusters(clusters) {
    if (!els.adminDuplicatesContainer) return;
    els.adminDuplicatesContainer.innerHTML = "";

    if (!clusters || clusters.length === 0) {
      els.adminDuplicatesContainer.innerHTML = `
        <div style="text-align:center; padding:32px; color:#15803d; background:#f0fdf4; border:1px solid #bbf7d0; border-radius:12px;">
          
          <div style="font-weight:700; font-size:15px;">Zero Duplicate Records Detected!</div>
          <div style="font-size:13px; color:#166534; margin-top:4px;">All phone numbers, student IDs, and user names are strictly unique.</div>
        </div>
      `;
      return;
    }

    clusters.forEach((g, idx) => {
      const card = document.createElement("div");
      card.className = "duplicate-cluster-card";

      const conf = (g.confidence || "MEDIUM").toUpperCase();

      let usersHtml = "";
      (g.users || []).forEach(u => {
        usersHtml += `
          <div class="duplicate-user-box">
            <div class="dup-user-header">
              <span>#${u.id} · ${escapeHtml(u.fullName || "User")}</span>
              <span class="role-badge role-${escapeHtml((u.role || "student").toLowerCase())}">${escapeHtml(u.role || "student")}</span>
            </div>
            <div>Email: ${escapeHtml(u.email || "—")}</div>
            <div>Mobile: ${escapeHtml(u.mobileNumber || "—")}</div>
            ${u.studentId ? `<div>ID: ${escapeHtml(u.studentId)}</div>` : ""}
            <div style="font-size:11px; color:#64748b; margin-top:4px;">Joined: ${escapeHtml(u.createdAt ? u.createdAt.split(" ")[0] : "—")}</div>
            <div style="margin-top:6px; display:flex; gap:6px;">
              <button type="button" class="btn-delete-user" data-userid="${u.id}" style="padding:4px 8px; font-size:11px;">
                Delete
              </button>
            </div>
          </div>
        `;
      });

      card.innerHTML = `
        <div class="duplicate-cluster-header">
          <div class="duplicate-type-title">
            <span>Cluster #${idx + 1}: ${escapeHtml(g.duplicateType || "Duplicate Detected")}</span>
          </div>
          <span class="confidence-badge confidence-${escapeHtml(conf)}">${escapeHtml(conf)} Confidence</span>
        </div>
        <div class="duplicate-reason-text">
          ${escapeHtml(g.reason || "Potential collision detected.")}
        </div>
        <div class="duplicate-users-grid">
          ${usersHtml}
        </div>
      `;

      // Attach delete listeners inside cluster card
      card.querySelectorAll(".btn-delete-user").forEach(btn => {
        btn.addEventListener("click", () => {
          const uid = parseInt(btn.getAttribute("data-userid"), 10);
          const found = (g.users || []).find(it => it.id === uid);
          if (found) openDeleteUserModal(found);
        });
      });

      els.adminDuplicatesContainer.appendChild(card);
    });
  }

  async function handleCandidateDuplicateCheck() {
    if (!els.manualDupResultBox) return;

    const name = (els.manualDupName ? els.manualDupName.value : "").trim();
    const email = (els.manualDupEmail ? els.manualDupEmail.value : "").trim();
    const mobileNumber = (els.manualDupMobile ? els.manualDupMobile.value : "").trim();
    const studentId = (els.manualDupStudentId ? els.manualDupStudentId.value : "").trim();

    if (!name && !email && !mobileNumber && !studentId) {
      els.manualDupResultBox.style.display = "block";
      els.manualDupResultBox.innerHTML = `<div style="padding:10px; background:#fee2e2; color:#b91c1c; border-radius:6px; font-size:13px;">Please enter at least one field to check for duplicates.</div>`;
      return;
    }

    els.manualDupResultBox.style.display = "block";
    els.manualDupResultBox.innerHTML = `<div style="padding:10px; background:#eff6ff; color:#1d4ed8; border-radius:6px; font-size:13px;">Checking candidate details against existing user base...</div>`;

    try {
      const res = await ApiClient.checkAdminDuplicates({ name, email, mobileNumber, studentId });
      if (res.ok && res.data) {
        if (!res.data.hasDuplicates) {
          els.manualDupResultBox.innerHTML = `
            <div style="padding:12px; background:#f0fdf4; border:1px solid #bbf7d0; border-radius:6px; color:#15803d; font-size:13px;">
              <strong>No Duplicates Found:</strong> No matching or conflicting accounts exist with this name, email, or mobile.
            </div>
          `;
        } else {
          let matchesHtml = "";
          (res.data.matches || []).forEach(m => {
            const reasons = (m.matchReasons || []).join(", ");
            matchesHtml += `
              <div style="background:#fff; border:1px solid #fecaca; border-radius:6px; padding:10px; margin-top:8px;">
                <div style="font-weight:700; color:#991b1b;">#${m.id} — ${escapeHtml(m.fullName)} (${escapeHtml(m.email)})</div>
                <div style="font-size:12px; color:#475569; margin:4px 0;"><strong>Match Trigger:</strong> ${escapeHtml(reasons)}</div>
                <div style="font-size:11px; color:#64748b;">Role: ${escapeHtml(m.role)} · Mobile: ${escapeHtml(m.mobileNumber)}</div>
              </div>
            `;
          });
          els.manualDupResultBox.innerHTML = `
            <div style="padding:12px; background:#fef2f2; border:1px solid #fecaca; border-radius:6px; color:#991b1b; font-size:13px;">
              <strong>${res.data.duplicateCount} Potential Duplicate(s) Found:</strong>
              ${matchesHtml}
            </div>
          `;
        }
      } else {
        els.manualDupResultBox.innerHTML = `<div style="padding:10px; background:#fee2e2; color:#b91c1c; border-radius:6px; font-size:13px;">Candidate duplicate check failed.</div>`;
      }
    } catch (e) {
      els.manualDupResultBox.innerHTML = `<div style="padding:10px; background:#fee2e2; color:#b91c1c; border-radius:6px; font-size:13px;">Network error: ${escapeHtml(e.message)}</div>`;
    }
  }

  // Platform Feedback Controller (Team C & B)
  async function loadAndRenderAdminFeedback() {
    if (!els.adminFeedbackListContainer) return;
    els.adminFeedbackListContainer.innerHTML = `<div style="text-align:center;padding:24px;color:#64748b;">Loading student reviews and rating metrics...</div>`;

    try {
      const res = await ApiClient.getAllFeedback();
      if (res.ok && res.data && Array.isArray(res.data.feedback)) {
        const feedbackList = res.data.feedback;
        const total = feedbackList.length;

        let sumRating = 0;
        let fiveStarCount = 0;
        feedbackList.forEach(fb => {
          sumRating += (fb.rating || 0);
          if (fb.rating === 5) fiveStarCount++;
        });

        const avg = total > 0 ? (sumRating / total).toFixed(1) : "0.0";
        const fiveStarPct = total > 0 ? Math.round((fiveStarCount / total) * 100) : 0;

        if (els.adminAvgRatingScore) els.adminAvgRatingScore.textContent = `${avg} / 5.0`;
        if (els.adminTotalFeedbackCount) els.adminTotalFeedbackCount.textContent = total;
        if (els.adminFiveStarPercent) els.adminFiveStarPercent.textContent = `${fiveStarPct}% (${fiveStarCount})`;

        renderAdminFeedbackList(feedbackList);
      } else {
        els.adminFeedbackListContainer.innerHTML = `<div style="text-align:center;padding:20px;color:#dc2626;">Failed to load feedback records.</div>`;
      }
    } catch (err) {
      console.error("Feedback fetch error:", err);
      els.adminFeedbackListContainer.innerHTML = `<div style="text-align:center;padding:20px;color:#dc2626;">Network error retrieving feedback.</div>`;
    }
  }

  function renderAdminFeedbackList(feedbackList) {
    if (!els.adminFeedbackListContainer) return;
    els.adminFeedbackListContainer.innerHTML = "";

    if (!feedbackList || feedbackList.length === 0) {
      els.adminFeedbackListContainer.innerHTML = `<div style="text-align:center;padding:32px;color:#64748b;">No feedback reviews recorded yet.</div>`;
      return;
    }

    feedbackList.forEach(fb => {
      const card = document.createElement("div");
      card.className = "feedback-review-card";

      const stars = "★".repeat(fb.rating || 5) + "☆".repeat(Math.max(0, 5 - (fb.rating || 5)));

      card.innerHTML = `
        <div class="feedback-review-header">
          <div>
            <strong style="color:#0f172a; font-size:14px;">${escapeHtml(fb.studentName || "Student")}</strong>
            <span style="font-size:12px; color:#64748b; margin:0 6px;">reviewed mentor</span>
            <strong style="color:#2563eb; font-size:14px;">${escapeHtml(fb.mentorName || "Mentor")}</strong>
            <span style="font-size:11px; color:#94a3b8; margin-left:8px;">(Session #${fb.sessionId})</span>
          </div>
          <div class="feedback-review-stars">${stars}</div>
        </div>
        <div class="feedback-review-comments">
          "${escapeHtml(fb.comments || "No comments provided.")}"
        </div>
        <div style="font-size:11px; color:#94a3b8; margin-top:8px; text-align:right;">
          ${escapeHtml(fb.createdAt ? fb.createdAt.split(".")[0] : "")}
        </div>
      `;
      els.adminFeedbackListContainer.appendChild(card);
    });
  }

  function filterAdminUsersTable() {
    if (!state.adminUsers) return;
    const q = (els.adminSearchInput ? els.adminSearchInput.value : "").toLowerCase().trim();
    if (!q) {
      renderAdminUsers(state.adminUsers);
      return;
    }
    const filtered = state.adminUsers.filter(u => {
      const name = (u.fullName || "").toLowerCase();
      const email = (u.email || "").toLowerCase();
      const mobile = (u.mobileNumber || "").toLowerCase();
      const idStr = String(u.id);
      return name.includes(q) || email.includes(q) || mobile.includes(q) || idStr.includes(q);
    });
    renderAdminUsers(filtered);
  }

  function setAdminAlert(type, msg) {
    if (!els.adminAlert) return;
    if (!type || !msg) {
      els.adminAlert.style.display = "none";
      els.adminAlert.textContent = "";
      els.adminAlert.className = "form-status-alert";
      return;
    }
    els.adminAlert.className = `form-status-alert alert-${type}`;
    els.adminAlert.textContent = msg;
    els.adminAlert.style.display = "block";
  }

  // =========================================================================
  // Admin Events Approvals Controller (System Architecture)
  // =========================================================================
  async function loadAndRenderAdminEvents() {
    if (!els.adminEventsTableBody) return;
    els.adminEventsTableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:24px;color:#64748b;">Loading institutional events...</td></tr>`;

    try {
      const res = await ApiClient.getAdminEvents();
      if (!res.ok || !Array.isArray(res.data)) {
        throw new Error(res.data?.message || "Failed to fetch events.");
      }

      const events = res.data;
      const pendingCount = events.filter(e => (e.status || "").toUpperCase() === "PENDING").length;
      if (els.adminEventsBadge) {
        if (pendingCount > 0) {
          els.adminEventsBadge.textContent = pendingCount;
          els.adminEventsBadge.style.display = "inline-flex";
        } else {
          els.adminEventsBadge.style.display = "none";
        }
      }

      if (events.length === 0) {
        els.adminEventsTableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:24px;color:#64748b;">No events recorded in system.</td></tr>`;
        return;
      }

      els.adminEventsTableBody.innerHTML = "";
      events.forEach(ev => {
        const tr = document.createElement("tr");
        const st = (ev.status || "PENDING").toUpperCase();
        const isPending = st === "PENDING";
        const isApproved = st === "APPROVED";

        tr.innerHTML = `
          <td style="font-weight:700; color:#64748b;">#${ev.id}</td>
          <td>
            <div style="font-weight:700; color:#0f172a;">${escapeHtml(ev.title || "Untitled Event")}</div>
            <div style="font-size:12px; color:#64748b; max-width:320px; text-overflow:ellipsis; overflow:hidden; white-space:nowrap;">
              ${escapeHtml(ev.description || "")}
            </div>
          </td>
          <td><span class="role-badge role-alumni">${escapeHtml(ev.eventType || ev.event_type || "GENERAL")}</span></td>
          <td><span style="font-size:12px; font-weight:600; color:#334155;">Mentor #${ev.organizerAlumniId || ev.organizer_alumni_id || "—"}</span></td>
          <td>
            <div style="font-weight:600; color:#0f172a; font-size:12px;">${escapeHtml(ev.date || "")}</div>
            <div style="font-size:11px; color:#64748b;">${escapeHtml(ev.startTime || ev.start_time || "")} - ${escapeHtml(ev.endTime || ev.end_time || "")}</div>
          </td>
          <td style="font-size:12px; color:#64748b;">${escapeHtml(ev.venue || "Campus")}</td>
          <td>
            <span class="approval-badge approval-${escapeHtml(st)}">
              ${isApproved ? 'APPROVED' : isPending ? 'PENDING' : 'REJECTED'}
            </span>
          </td>
          <td style="text-align:right;">
            ${isPending ? `
              <div class="admin-action-btn-group" style="justify-content:flex-end;">
                <button type="button" class="btn-approve-user" data-eventid="${ev.id}" style="background:#15803d; color:#fff; border:none; padding:5px 10px; border-radius:6px; font-size:11px; font-weight:600; cursor:pointer;">
                  ✓ Approve
                </button>
                <button type="button" class="btn-reject-user" data-eventid="${ev.id}" style="background:#dc2626; color:#fff; border:none; padding:5px 10px; border-radius:6px; font-size:11px; font-weight:600; cursor:pointer;">
                  ✕ Reject
                </button>
              </div>
            ` : `<span style="font-size:12px; color:#94a3b8; font-weight:600;">Decided</span>`}
          </td>
        `;

        const approveBtn = tr.querySelector(".btn-approve-user");
        if (approveBtn) {
          approveBtn.addEventListener("click", () => handleAdminEventDecision(ev.id, "APPROVED"));
        }
        const rejectBtn = tr.querySelector(".btn-reject-user");
        if (rejectBtn) {
          rejectBtn.addEventListener("click", () => handleAdminEventDecision(ev.id, "REJECTED"));
        }

        els.adminEventsTableBody.appendChild(tr);
      });
    } catch (err) {
      els.adminEventsTableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:24px;color:#dc2626;">Error: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  async function handleAdminEventDecision(eventId, status) {
    if (!confirm(`Are you sure you want to mark Event #${eventId} as ${status}?`)) return;
    try {
      const res = await ApiClient.updateAdminEventStatus(eventId, status);
      if (res.ok) {
        setAdminAlert("success", `Event #${eventId} was successfully updated to ${status}.`);
        loadAndRenderAdminEvents();
      } else {
        setAdminAlert("error", res.data?.message || "Failed to update event status.");
      }
    } catch (err) {
      setAdminAlert("error", err.message || "Failed to update event status.");
    }
  }

  // =========================================================================
  // Teacher / Faculty Hub Controller
  // =========================================================================
  let cachedMentorsList = [];
  let cachedStudentsList = [];

  function setTeacherAlert(type, msg) {
    if (!els.teacherAlert) return;
    if (!type || !msg) {
      els.teacherAlert.style.display = "none";
      els.teacherAlert.textContent = "";
      els.teacherAlert.className = "form-status-alert";
      return;
    }
    els.teacherAlert.className = `form-status-alert alert-${type}`;
    els.teacherAlert.textContent = msg;
    els.teacherAlert.style.display = "block";
    els.teacherAlert.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  function setHubAlert(alertEl, type, msg) {
    if (!alertEl) return;
    if (!type || !msg) {
      alertEl.style.display = "none";
      alertEl.textContent = "";
      alertEl.className = "form-status-alert";
      return;
    }
    alertEl.className = `form-status-alert alert-${type}`;
    alertEl.textContent = msg;
    alertEl.style.display = "block";
  }

  function switchTeacherSubtab(tab) {
    const tabs = [
      { id: "schedules", btn: els.teacherSubtabSchedulesBtn, panel: els.teacherPanelSchedules },
      { id: "oneOnOne", btn: els.teacherSubtabOneOnOneBtn, panel: els.teacherPanelOneOnOne },
      { id: "publicMeet", btn: els.teacherSubtabPublicMeetBtn, panel: els.teacherPanelPublicMeet },
      { id: "newMentor", btn: els.teacherSubtabNewMentorBtn, panel: els.teacherPanelNewMentor }
    ];

    tabs.forEach(t => {
      const isActive = (t.id === tab);
      if (t.btn) t.btn.classList.toggle("active", isActive);
      if (t.panel) t.panel.style.display = isActive ? "block" : "none";
    });

    if (tab === "schedules") {
      loadTeacherMeets();
    } else if (tab === "oneOnOne") {
      loadTeacherMeetDropdowns();
      if (els.scheduleOneOnOneForm) els.scheduleOneOnOneForm.reset();
      setHubAlert(els.scheduleOneOnOneAlert, null, "");
      // default date to tomorrow
      if (els.meetDate) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        els.meetDate.value = tomorrow.toISOString().split("T")[0];
      }
      if (els.meetStartTime) els.meetStartTime.value = "10:00";
      if (els.meetEndTime) els.meetEndTime.value = "11:00";
    } else if (tab === "publicMeet") {
      loadTeacherMeetDropdowns();
      if (els.schedulePublicMeetForm) els.schedulePublicMeetForm.reset();
      setHubAlert(els.schedulePublicMeetAlert, null, "");
      if (els.publicMeetDate) {
        const nextWeek = new Date();
        nextWeek.setDate(nextWeek.getDate() + 3);
        els.publicMeetDate.value = nextWeek.toISOString().split("T")[0];
      }
      if (els.publicStartTime) els.publicStartTime.value = "14:00";
      if (els.publicEndTime) els.publicEndTime.value = "15:30";
      if (els.publicVenue) els.publicVenue.value = "Google Meet";
    } else if (tab === "newMentor") {
      if (els.onboardMentorForm) els.onboardMentorForm.reset();
      setHubAlert(els.onboardMentorAlert, null, "");
      if (els.newMentorPassword) els.newMentorPassword.value = "Alumni@123";
      if (els.newMentorGradYear) els.newMentorGradYear.value = "2020";
      if (els.newMentorExp) els.newMentorExp.value = "4";
      if (els.newMentorCapacity) els.newMentorCapacity.value = "5";
    }
  }

  async function loadTeacherPortal() {
    switchTeacherSubtab("schedules");
  }

  async function loadTeacherMeetDropdowns() {
    try {
      const [mentorsRes, studentsRes] = await Promise.all([
        ApiClient.getTeacherMentors(),
        ApiClient.getTeacherStudents()
      ]);

      if (mentorsRes.ok && Array.isArray(mentorsRes.data)) {
        cachedMentorsList = mentorsRes.data;
        const optionsHtml = `<option value="">Select Mentor...</option>` + cachedMentorsList.map(m => 
          `<option value="${m.id}">${escapeHtml(m.fullName)} — ${escapeHtml(m.company || 'Alumni')} (${escapeHtml(m.designation || m.department || 'Mentor')})</option>`
        ).join("");

        if (els.meetMentorSelect) els.meetMentorSelect.innerHTML = optionsHtml;
        if (els.publicMentorSelect) els.publicMentorSelect.innerHTML = optionsHtml;
      }

      if (studentsRes.ok && Array.isArray(studentsRes.data)) {
        cachedStudentsList = studentsRes.data;
        const studentsHtml = `<option value="">Select Student...</option>` + cachedStudentsList.map(s => 
          `<option value="${s.id}">${escapeHtml(s.fullName)} (${s.rollNumber ? s.rollNumber + ' · ' : ''}${escapeHtml(s.department || 'Student')})</option>`
        ).join("");

        if (els.meetStudentSelect) els.meetStudentSelect.innerHTML = studentsHtml;
      }
    } catch (err) {
      console.error("Failed to load mentor/student dropdowns:", err);
    }
  }

  async function loadTeacherMeets() {
    if (!els.teacherMeetsTableBody) return;
    els.teacherMeetsTableBody.innerHTML = `<tr><td colspan="9" style="text-align:center;padding:24px;color:#64748b;">Loading session schedules...</td></tr>`;

    const type = els.filterMeetType ? els.filterMeetType.value : "";
    const status = els.filterMeetStatus ? els.filterMeetStatus.value : "";

    try {
      const res = await ApiClient.getTeacherMeets(type, status);
      if (!res.ok || !Array.isArray(res.data)) {
        throw new Error(res.data?.message || "Failed to load schedules.");
      }

      const meets = res.data;
      if (meets.length === 0) {
        els.teacherMeetsTableBody.innerHTML = `<tr><td colspan="9" style="text-align:center;padding:28px;color:#64748b;">No scheduled meets found matching criteria. Click <strong>"New 1-on-1 Meet"</strong> or <strong>"New Public Meet"</strong> to schedule one.</td></tr>`;
        return;
      }

      els.teacherMeetsTableBody.innerHTML = "";
      meets.forEach(m => {
        const tr = document.createElement("tr");
        const isPublic = m.meetType === "PUBLIC";
        const st = (m.status || "SCHEDULED").toUpperCase();
        const isScheduled = st === "SCHEDULED";

        tr.innerHTML = `
          <td style="font-weight:700; color:#64748b;">#${m.id}</td>
          <td>
            <span class="meet-type-badge ${isPublic ? 'type-public' : 'type-one-on-one'}">
              ${isPublic ? 'Public' : '1-on-1'}
            </span>
          </td>
          <td>
            <div style="font-weight:700; color:#0f172a; max-width:220px; line-height:1.3;">${escapeHtml(m.title)}</div>
            ${m.description ? `<div style="font-size:12px; color:#64748b; max-width:220px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(m.description)}</div>` : ''}
          </td>
          <td>
            <div style="font-weight:700; color:#0f172a;">${escapeHtml(m.mentorName || `Mentor #${m.mentorId}`)}</div>
            <div style="font-size:11px; color:#64748b;">${escapeHtml(m.mentorCompany ? m.mentorCompany + (m.mentorDesignation ? ' · ' + m.mentorDesignation : '') : '')}</div>
          </td>
          <td>
            ${isPublic ? `
              <div>
                <span style="font-weight:700; color:#4338ca; font-size:13px;">${m.attendeeCount || 0} enrolled</span>
                ${m.maxParticipants ? `<span style="font-size:11px; color:#64748b;"> / max ${m.maxParticipants}</span>` : ''}
              </div>
              <button type="button" class="btn-view-attendees" data-meetid="${m.id}" data-meettitle="${escapeHtml(m.title)}" style="font-size:11px; padding:2px 8px; margin-top:4px; border-radius:4px; border:1px solid #cbd5e1; background:#f8fafc; color:#1e293b; cursor:pointer;">
                View Attendees
              </button>
            ` : `
              <div style="font-weight:700; color:#0f172a;">${escapeHtml(m.studentName || (m.studentId ? `Student #${m.studentId}` : '—'))}</div>
              <div style="font-size:11px; color:#64748b;">${escapeHtml(m.studentDepartment || '')}</div>
            `}
          </td>
          <td>
            <div style="font-weight:600; color:#0f172a; font-size:12px;">${escapeHtml(m.meetDate || '')}</div>
            <div style="font-size:11px; color:#64748b;">${escapeHtml(m.startTime || '')} - ${escapeHtml(m.endTime || '')}</div>
          </td>
          <td>
            <div style="font-size:12px; font-weight:600; color:#1e293b;">${escapeHtml(m.venue || 'Online')}</div>
            ${m.meetingLink ? `
              <a href="${escapeHtml(m.meetingLink)}" target="_blank" rel="noopener noreferrer" style="font-size:11px; color:#2563eb; text-decoration:none; display:inline-flex; align-items:center; gap:3px;">
                Join Link
              </a>
            ` : ''}
          </td>
          <td>
            <span class="meet-status-badge status-${st.toLowerCase()}">
              ${st === 'SCHEDULED' ? 'SCHEDULED' : st === 'COMPLETED' ? 'COMPLETED' : 'CANCELLED'}
            </span>
          </td>
          <td style="text-align:right;">
            <div style="display:flex; justify-content:flex-end; gap:6px;">
              ${isScheduled ? `
                <button type="button" class="btn-meet-done" data-meetid="${m.id}" title="Mark Session Completed" style="background:#15803d; color:#fff; border:none; padding:4px 8px; border-radius:6px; font-size:11px; font-weight:600; cursor:pointer;">
                  Done
                </button>
                <button type="button" class="btn-meet-cancel" data-meetid="${m.id}" title="Cancel Session" style="background:#dc2626; color:#fff; border:none; padding:4px 8px; border-radius:6px; font-size:11px; font-weight:600; cursor:pointer;">
                  Cancel
                </button>
              ` : `
                <span style="font-size:11px; color:#94a3b8; font-weight:600;">${st}</span>
              `}
            </div>
          </td>
        `;

        const viewAttBtn = tr.querySelector(".btn-view-attendees");
        if (viewAttBtn) {
          viewAttBtn.addEventListener("click", () => openMeetAttendeesModal(m.id, m.title));
        }

        const doneBtn = tr.querySelector(".btn-meet-done");
        if (doneBtn) {
          doneBtn.addEventListener("click", () => handleTeacherMeetStatus(m.id, "COMPLETED"));
        }

        const cancelBtn = tr.querySelector(".btn-meet-cancel");
        if (cancelBtn) {
          cancelBtn.addEventListener("click", () => handleTeacherMeetStatus(m.id, "CANCELLED"));
        }

        els.teacherMeetsTableBody.appendChild(tr);
      });
    } catch (err) {
      els.teacherMeetsTableBody.innerHTML = `<tr><td colspan="9" style="text-align:center;padding:24px;color:#dc2626;">Error: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  async function handleScheduleOneOnOneSubmit(e) {
    e.preventDefault();
    const mentorId = els.meetMentorSelect.value;
    const studentId = els.meetStudentSelect.value;
    const title = els.meetTitle.value.trim();
    const description = els.meetDescription.value.trim();
    const meetDate = els.meetDate.value;
    const startTime = els.meetStartTime.value;
    const endTime = els.meetEndTime.value;
    const meetingLink = els.meetingLink.value.trim();
    const venue = els.meetVenue.value.trim();

    if (!mentorId || !studentId || !title || !meetDate || !startTime || !endTime) {
      setHubAlert(els.scheduleOneOnOneAlert, "error", "Please fill in all required fields marked with *.");
      return;
    }

    if (endTime <= startTime) {
      setHubAlert(els.scheduleOneOnOneAlert, "error", "End time must be after start time.");
      return;
    }

    try {
      if (els.submitOneOnOneBtn) els.submitOneOnOneBtn.disabled = true;

      const payload = {
        title,
        description: description || null,
        meetType: "ONE_ON_ONE",
        mentorId: Number(mentorId),
        studentId: Number(studentId),
        meetDate,
        startTime,
        endTime,
        meetingLink: meetingLink || null,
        venue: venue || "Online"
      };

      const res = await ApiClient.createTeacherMeet(payload);
      if (res.ok) {
        setTeacherAlert("success", "1-on-1 Mentorship meet scheduled successfully!");
        switchTeacherSubtab("schedules");
      } else {
        setHubAlert(els.scheduleOneOnOneAlert, "error", res.data?.message || "Failed to schedule meet.");
      }
    } catch (err) {
      setHubAlert(els.scheduleOneOnOneAlert, "error", err.message || "Failed to schedule meet.");
    } finally {
      if (els.submitOneOnOneBtn) els.submitOneOnOneBtn.disabled = false;
    }
  }

  async function handleSchedulePublicMeetSubmit(e) {
    e.preventDefault();
    const mentorId = els.publicMentorSelect.value;
    const title = els.publicMeetTitle.value.trim();
    const description = els.publicMeetDesc.value.trim();
    const maxCapacity = els.publicMaxCapacity.value ? Number(els.publicMaxCapacity.value) : null;
    const meetDate = els.publicMeetDate.value;
    const startTime = els.publicStartTime.value;
    const endTime = els.publicEndTime.value;
    const meetingLink = els.publicMeetingLink.value.trim();
    const venue = els.publicVenue.value.trim();

    if (!mentorId || !title || !description || !meetDate || !startTime || !endTime || !meetingLink) {
      setHubAlert(els.schedulePublicMeetAlert, "error", "Please fill in all required fields marked with *.");
      return;
    }

    if (endTime <= startTime) {
      setHubAlert(els.schedulePublicMeetAlert, "error", "End time must be after start time.");
      return;
    }

    try {
      if (els.submitPublicMeetBtn) els.submitPublicMeetBtn.disabled = true;

      const payload = {
        title,
        description,
        meetType: "PUBLIC",
        mentorId: Number(mentorId),
        meetDate,
        startTime,
        endTime,
        meetingLink,
        venue: venue || "Online",
        maxParticipants: maxCapacity
      };

      const res = await ApiClient.createTeacherMeet(payload);
      if (res.ok) {
        setTeacherAlert("success", "Public Masterclass session published! Students can now view and join.");
        switchTeacherSubtab("schedules");
      } else {
        setHubAlert(els.schedulePublicMeetAlert, "error", res.data?.message || "Failed to publish public session.");
      }
    } catch (err) {
      setHubAlert(els.schedulePublicMeetAlert, "error", err.message || "Failed to publish public session.");
    } finally {
      if (els.submitPublicMeetBtn) els.submitPublicMeetBtn.disabled = false;
    }
  }

  async function handleOnboardMentorSubmit(e) {
    e.preventDefault();
    const fullName = els.newMentorName.value.trim();
    const email = els.newMentorEmail.value.trim();
    const mobileNumber = els.newMentorMobile.value.trim();
    const password = els.newMentorPassword.value.trim() || "Alumni@123";
    const company = els.newMentorCompany.value.trim();
    const designation = els.newMentorDesignation.value.trim();
    const department = els.newMentorDept.value;
    const graduationYear = els.newMentorGradYear.value ? Number(els.newMentorGradYear.value) : 2020;
    const experienceYears = els.newMentorExp.value ? Number(els.newMentorExp.value) : 4;
    const skills = els.newMentorSkills.value.trim();
    const maxMentees = els.newMentorCapacity.value ? Number(els.newMentorCapacity.value) : 5;
    const bio = els.newMentorBio.value.trim();

    if (!fullName || !email || !mobileNumber || !company || !designation || !skills) {
      setHubAlert(els.onboardMentorAlert, "error", "Please provide all required mentor details.");
      return;
    }

    try {
      if (els.submitOnboardMentorBtn) els.submitOnboardMentorBtn.disabled = true;

      const payload = {
        fullName,
        email,
        mobileNumber,
        password,
        company,
        designation,
        department,
        graduationYear,
        experienceYears,
        skills,
        maxMentees,
        bio: bio || `Alumni mentor at ${company}`
      };

      const res = await ApiClient.createTeacherMentor(payload);
      if (res.ok) {
        setTeacherAlert("success", `Mentor ${fullName} onboarded successfully! They can log in with ${email} / ${password}.`);
        els.onboardMentorForm.reset();
        setHubAlert(els.onboardMentorAlert, "success", `Mentor account created! ID: #${res.data?.mentorId}`);
        loadTeacherMeetDropdowns();
      } else {
        setHubAlert(els.onboardMentorAlert, "error", res.data?.message || "Failed to onboard mentor.");
      }
    } catch (err) {
      setHubAlert(els.onboardMentorAlert, "error", err.message || "Failed to onboard mentor.");
    } finally {
      if (els.submitOnboardMentorBtn) els.submitOnboardMentorBtn.disabled = false;
    }
  }

  async function openMeetAttendeesModal(meetId, title) {
    if (!els.meetAttendeesModal) return;
    if (els.attendeesModalTitle) els.attendeesModalTitle.textContent = `Attendees: ${title}`;
    if (els.attendeesModalSubtitle) els.attendeesModalSubtitle.textContent = `Registered students for Meet #${meetId}`;
    if (els.attendeesTableBody) {
      els.attendeesTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:16px;">Loading attendees list...</td></tr>`;
    }

    els.meetAttendeesModal.style.display = "flex";

    try {
      const res = await ApiClient.getMeetAttendees(meetId);
      if (!res.ok || !Array.isArray(res.data)) {
        throw new Error(res.data?.message || "Failed to load attendees.");
      }

      const attendees = res.data;
      if (attendees.length === 0) {
        els.attendeesTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:20px;color:#64748b;">No students have registered for this session yet.</td></tr>`;
        return;
      }

      els.attendeesTableBody.innerHTML = "";
      attendees.forEach(a => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td style="font-weight:700; color:#64748b;">${escapeHtml(a.rollNumber || `STU #${a.studentId}`)}</td>
          <td style="font-weight:700; color:#0f172a;">${escapeHtml(a.fullName)}</td>
          <td>${escapeHtml(a.department || '—')}</td>
          <td><a href="mailto:${escapeHtml(a.email)}" style="color:#2563eb; text-decoration:none;">${escapeHtml(a.email)}</a></td>
          <td style="font-size:12px; color:#64748b;">${escapeHtml(a.registeredAt ? a.registeredAt.split(" ")[0] : '—')}</td>
        `;
        els.attendeesTableBody.appendChild(tr);
      });
    } catch (err) {
      els.attendeesTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:16px;color:#dc2626;">Error loading attendees: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  function closeMeetAttendeesModal() {
    if (els.meetAttendeesModal) els.meetAttendeesModal.style.display = "none";
  }

  async function handleTeacherMeetStatus(meetId, status) {
    if (!confirm(`Are you sure you want to mark Meet #${meetId} as ${status}?`)) return;
    try {
      const res = await ApiClient.updateTeacherMeetStatus(meetId, status);
      if (res.ok) {
        setTeacherAlert("success", `Meet #${meetId} marked as ${status}.`);
        loadTeacherMeets();
      } else {
        setTeacherAlert("error", res.data?.message || "Failed to update meet status.");
      }
    } catch (err) {
      setTeacherAlert("error", err.message || "Failed to update meet status.");
    }
  }

  async function loadTeacherDirectory() {
    if (!els.teacherDirectoryTableBody) return;
    els.teacherDirectoryTableBody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;color:#64748b;">Loading directory...</td></tr>`;

    try {
      const res = await ApiClient.getAdminUsers();
      if (!res.ok || !Array.isArray(res.data)) {
        throw new Error(res.data?.message || "Failed to load directory.");
      }

      const users = res.data.filter(u => {
        const r = (u.role || "").toLowerCase();
        return r === "student" || r === "alumni";
      });

      if (users.length === 0) {
        els.teacherDirectoryTableBody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;color:#64748b;">No students or alumni found.</td></tr>`;
        return;
      }

      els.teacherDirectoryTableBody.innerHTML = "";
      users.forEach(u => {
        const tr = document.createElement("tr");
        const r = (u.role || "").toLowerCase();
        const approval = (u.approvalStatus || "APPROVED").toUpperCase();
        const isActive = !!u.active;

        tr.innerHTML = `
          <td style="font-weight:700; color:#64748b;">#${u.id}</td>
          <td>
            <div style="font-weight:700; color:#0f172a;">${escapeHtml(u.fullName || "User")}</div>
            <div style="font-size:12px; color:#64748b;">${escapeHtml(u.email || "")}</div>
          </td>
          <td><span class="role-badge role-${escapeHtml(r)}">${escapeHtml(u.role || "student")}</span></td>
          <td style="font-size:12px; color:#334155;">${escapeHtml(u.department || u.company || "—")}</td>
          <td>${escapeHtml(u.mobileNumber || "—")}</td>
          <td>
            <span class="approval-badge approval-${escapeHtml(approval)}">
              ${escapeHtml(approval)}
            </span>
          </td>
          <td>
            <span class="status-indicator-badge ${isActive ? 'is-active' : 'is-inactive'}">
              <span class="status-dot"></span>
              ${isActive ? 'Active' : 'Inactive'}
            </span>
          </td>
        `;

        els.teacherDirectoryTableBody.appendChild(tr);
      });
    } catch (err) {
      els.teacherDirectoryTableBody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:24px;color:#dc2626;">Error loading directory: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  // =========================================================================
  // Student Public Meets Integration
  // =========================================================================
  async function loadStudentPublicMeets() {
    if (!els.studentPublicMeetsSection || !els.studentPublicMeetsList) return;

    // Show public meets section for students or logged in users
    els.studentPublicMeetsSection.style.display = "block";
    els.studentPublicMeetsList.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:18px; color:#64748b;">Loading upcoming masterclasses...</div>`;

    try {
      const res = await ApiClient.getPublicMeets();
      if (!res.ok || !Array.isArray(res.data)) {
        els.studentPublicMeetsSection.style.display = "none";
        return;
      }

      const meets = res.data;
      if (meets.length === 0) {
        els.studentPublicMeetsList.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:18px; color:#64748b; font-size:13px;">No public masterclasses scheduled right now. Check back soon!</div>`;
        return;
      }

      els.studentPublicMeetsList.innerHTML = "";
      meets.forEach(m => {
        const card = document.createElement("div");
        card.className = "public-meet-card";

        const isRegistered = !!m.isRegistered;
        const capacityReached = m.maxParticipants && (m.attendeeCount >= m.maxParticipants);
        const isStudent = state.currentUser && (state.currentUser.role || "").toLowerCase() === "student";

        card.innerHTML = `
          <div>
            <div class="public-meet-card-header">
              <span class="meet-type-badge type-public">Public Masterclass</span>
              <span style="font-size:11px; font-weight:700; color:#4338ca; background:#e0e7ff; padding:2px 8px; border-radius:999px;">
                ${m.attendeeCount || 0} Attending ${m.maxParticipants ? `(Max ${m.maxParticipants})` : ''}
              </span>
            </div>

            <h4 class="public-meet-card-title">${escapeHtml(m.title)}</h4>

            <div class="public-meet-mentor-info">
              <div class="user-avatar-sm" style="width:28px; height:28px; font-size:12px;">${escapeHtml((m.mentorName || 'M').charAt(0))}</div>
              <div>
                <strong>${escapeHtml(m.mentorName)}</strong>
                <span style="font-size:11px; color:#64748b; display:block;">${escapeHtml(m.mentorCompany ? m.mentorCompany + (m.mentorDesignation ? ' · ' + m.mentorDesignation : '') : 'Alumni Mentor')}</span>
              </div>
            </div>

            ${m.description ? `<p style="font-size:12px; color:#475569; margin:0 0 12px 0; line-height:1.4;">${escapeHtml(m.description)}</p>` : ''}

            <div class="public-meet-meta-list">
              <div class="public-meet-meta-item">
                <strong>Date:</strong> ${escapeHtml(m.meetDate)} &nbsp;•&nbsp; <strong>Time:</strong> ${escapeHtml(m.startTime)} - ${escapeHtml(m.endTime)}
              </div>
              <div class="public-meet-meta-item">
                <span>${escapeHtml(m.venue || 'Online Platform')}</span>
              </div>
            </div>
          </div>

          <div class="public-meet-actions">
            ${isStudent ? `
              ${isRegistered ? `
                <div style="display:flex; align-items:center; justify-content:space-between; width:100%; gap:8px;">
                  <span style="font-size:12px; font-weight:700; color:#15803d; display:flex; align-items:center; gap:4px;">
                    Enrolled
                  </span>
                  <div style="display:flex; gap:6px;">
                    ${m.meetingLink ? `
                      <a href="${escapeHtml(m.meetingLink)}" target="_blank" rel="noopener noreferrer" class="modal-submit-btn" style="padding:6px 12px; font-size:12px; text-decoration:none;">
                        Join Live
                      </a>
                    ` : ''}
                    <button type="button" class="btn-leave-public-meet modal-cancel-btn" data-meetid="${m.id}" style="padding:6px 10px; font-size:11px;">
                      Cancel RSVP
                    </button>
                  </div>
                </div>
              ` : `
                <div style="display:flex; align-items:center; justify-content:space-between; width:100%; gap:8px;">
                  <span style="font-size:12px; color:#64748b;">
                    ${capacityReached ? 'Capacity full' : 'Free admission for students'}
                  </span>
                  <button type="button" class="btn-join-public-meet modal-submit-btn" data-meetid="${m.id}" ${capacityReached ? 'disabled style="opacity:0.5; cursor:not-allowed;"' : 'style="padding:7px 16px; font-size:12px; background:#4338ca;"'}>
                    ${capacityReached ? 'Full' : 'RSVP & Join'}
                  </button>
                </div>
              `}
            ` : `
              <div style="display:flex; justify-content:space-between; width:100%; align-items:center;">
                <span style="font-size:12px; color:#64748b;">Open to all students</span>
                ${m.meetingLink ? `
                  <a href="${escapeHtml(m.meetingLink)}" target="_blank" rel="noopener noreferrer" style="font-size:12px; color:#2563eb; font-weight:600; text-decoration:none;">
                    Meeting Link
                  </a>
                ` : ''}
              </div>
            `}
          </div>
        `;

        const joinBtn = card.querySelector(".btn-join-public-meet");
        if (joinBtn) {
          joinBtn.addEventListener("click", () => handleJoinPublicMeet(m.id));
        }

        const leaveBtn = card.querySelector(".btn-leave-public-meet");
        if (leaveBtn) {
          leaveBtn.addEventListener("click", () => handleLeavePublicMeet(m.id));
        }

        els.studentPublicMeetsList.appendChild(card);
      });
    } catch (err) {
      els.studentPublicMeetsList.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:18px; color:#dc2626;">Error: ${escapeHtml(err.message)}</div>`;
    }
  }

  async function handleJoinPublicMeet(meetId) {
    try {
      const res = await ApiClient.registerPublicMeet(meetId);
      if (res.ok) {
        alert("You have successfully registered for this public masterclass session.");
        loadStudentPublicMeets();
      } else {
        alert(res.data?.message || "Failed to register for session.");
      }
    } catch (err) {
      alert("Error: " + err.message);
    }
  }

  async function handleLeavePublicMeet(meetId) {
    if (!confirm("Are you sure you want to cancel your registration for this session?")) return;
    try {
      const res = await ApiClient.unregisterPublicMeet(meetId);
      if (res.ok) {
        loadStudentPublicMeets();
      } else {
        alert(res.data?.message || "Failed to cancel registration.");
      }
    } catch (err) {
      alert("Error: " + err.message);
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
      if (!state.currentUser.token) {
        handleUnauthorized("Your session has expired. Please sign in again.");
        return;
      }
      if (state.currentUser.id) {
        ApiClient.getProfile(state.currentUser.id).then(res => {
          if (res.status === 401) {
            handleUnauthorized("Your session has expired. Please sign in again.");
          }
        }).catch(() => {});
      }

      const role = (state.currentUser.role || "").toLowerCase();
      if (role === "admin") {
        switchView("admin");
      } else if (role === "teacher") {
        switchView("teacher");
      } else if (role === "alumni") {
        switchView("requests");
      } else {
        switchView("search");
      }
    } else {
      switchView("login");
    }
  });
})();
