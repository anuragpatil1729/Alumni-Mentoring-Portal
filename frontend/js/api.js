/**
 * Alumni Mentoring Portal - Vanilla JS API Client
 * Connects directly to Java Backend running on http://localhost:5001
 */

const API_BASE_URL = "http://localhost:5001";

const ApiClient = {
  /**
   * Searches alumni mentors using linear or binary search algorithms.
   */
  async searchMentors(params = {}) {
    const urlParams = new URLSearchParams();
    if (params.query) urlParams.append("query", params.query);
    urlParams.append("algorithm", params.algorithm || "linear");

    if (params.algorithm === "binary") {
      urlParams.append("key", params.key || "fullName");
      if (params.exact) urlParams.append("exact", "true");
    }

    if (params.department && params.department !== "All") {
      urlParams.append("department", params.department);
    }
    if (params.industry && params.industry !== "All") {
      urlParams.append("industry", params.industry);
    }
    if (params.minExperience) {
      urlParams.append("minExperience", params.minExperience);
    }

    const response = await fetch(`${API_BASE_URL}/api/mentors/search?${urlParams.toString()}`, {
      method: "GET",
      headers: {
        "Accept": "application/json"
      }
    });

    if (!response.ok) {
      const errData = await response.json().catch(() => ({}));
      throw new Error(errData.message || `Server error: HTTP ${response.status}`);
    }

    return await response.json();
  },

  /**
   * Retrieves single mentor by ID.
   */
  async getMentorById(id) {
    const response = await fetch(`${API_BASE_URL}/api/mentors/${id}`, {
      method: "GET",
      headers: { "Accept": "application/json" }
    });
    if (!response.ok) {
      throw new Error(`Mentor lookup failed: HTTP ${response.status}`);
    }
    return await response.json();
  },

  /**
   * Submits alumni registration to Java backend.
   */
  async registerAlumni(payload) {
    const response = await fetch(`${API_BASE_URL}/api/auth/register/alumni`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Submits student registration to Java backend.
   */
  async registerStudent(payload) {
    const response = await fetch(`${API_BASE_URL}/api/auth/register/student`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Authenticates student or alumni with email and password.
   */
  async login(credentials) {
    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify(credentials)
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Checks whether email and/or mobile number are available or already in use.
   */
  async checkAvailability({ email, mobileNumber } = {}) {
    const params = new URLSearchParams();
    if (email) params.append("email", email);
    if (mobileNumber) params.append("mobileNumber", mobileNumber);

    const response = await fetch(`${API_BASE_URL}/api/auth/check-availability?${params.toString()}`, {
      method: "GET",
      headers: { "Accept": "application/json" }
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Helper to get Authorization header from stored session.
   */
  getAuthHeader() {
    try {
      const stored = localStorage.getItem("alumniConnectUser");
      if (stored) {
        const u = JSON.parse(stored);
        if (u && u.token) {
          return { "Authorization": `Bearer ${u.token}` };
        }
      }
    } catch (e) {}
    return {};
  },

  /**
   * Retrieves full profile of current authenticated user.
   */
  async getProfile(userId) {
    const response = await fetch(`${API_BASE_URL}/api/profile?id=${userId}`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Updates profile fields for the given user.
   */
  async updateProfile(userId, payload) {
    const response = await fetch(`${API_BASE_URL}/api/profile?id=${userId}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Uploads base64 avatar image to MySQL / MariaDB BLOB (Approach 2).
   */
  async uploadAvatar(userId, base64Data, mimeType = "image/jpeg") {
    const response = await fetch(`${API_BASE_URL}/api/users/avatar?userId=${userId}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify({
        userId,
        imageData: base64Data,
        mimeType
      })
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Deletes avatar image from MySQL / MariaDB BLOB.
   */
  async deleteAvatar(userId) {
    const response = await fetch(`${API_BASE_URL}/api/users/avatar?userId=${userId}`, {
      method: "DELETE",
      headers: { "Accept": "application/json" }
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Creates a new mentorship request (Student -> Mentor)
   */
  async createMentorshipRequest(payload) {
    const response = await fetch(`${API_BASE_URL}/api/requests`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Retrieves mentorship requests sent by a student
   */
  async getRequestsByStudent(studentId) {
    const response = await fetch(`${API_BASE_URL}/api/requests?studentId=${studentId}`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Retrieves mentorship requests received by a mentor (alumni)
   */
  async getRequestsByMentor(mentorId) {
    const response = await fetch(`${API_BASE_URL}/api/requests?mentorId=${mentorId}`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Updates mentorship request status (ACCEPTED, REJECTED, CANCELLED)
   */
  async updateRequestStatus(requestId, payload) {
    const response = await fetch(`${API_BASE_URL}/api/requests?id=${requestId}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Retrieves session history with algorithmic sorting (Team C)
   */
  async getSessionHistory(params = {}) {
    const urlParams = new URLSearchParams();
    if (params.sort) urlParams.append("sort", params.sort);
    if (params.order) urlParams.append("order", params.order);

    const response = await fetch(`${API_BASE_URL}/api/session-history?${urlParams.toString()}`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Schedules a confirmed 1-on-1 session (Team 1)
   */
  async scheduleSession(payload) {
    const response = await fetch(`${API_BASE_URL}/api/sessions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  },

  /**
   * Admin: Fetch all users with optional role, active status, or approval filter
   */
  async getAdminUsers(params = {}) {
    const qp = new URLSearchParams();
    if (params.role) qp.append("role", params.role);
    if (params.active !== undefined && params.active !== "") qp.append("active", params.active);
    if (params.approval) qp.append("approval", params.approval);
    const response = await fetch(`${API_BASE_URL}/api/admin/users?${qp.toString()}`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Admin: Activate or deactivate a user account
   */
  async setUserActiveStatus(userId, active) {
    const response = await fetch(`${API_BASE_URL}/api/admin/users/${userId}/status`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify({ active })
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Admin: Approve or Reject a user registration
   */
  async approveAdminUser(userId, approved = true) {
    const response = await fetch(`${API_BASE_URL}/api/admin/users/${userId}/approve`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify({ approved })
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Admin: Permanently delete a user account
   */
  async deleteAdminUser(userId) {
    const response = await fetch(`${API_BASE_URL}/api/admin/users/${userId}`, {
      method: "DELETE",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Admin: Scan all users for potential duplicates
   */
  async getAdminDuplicates() {
    const response = await fetch(`${API_BASE_URL}/api/admin/duplicates`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Admin: Check specific candidate details for duplicates
   */
  async checkAdminDuplicates(payload) {
    const response = await fetch(`${API_BASE_URL}/api/admin/duplicates/check`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify(payload)
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Admin: Fetch institutional events
   */
  async getAdminEvents() {
    const response = await fetch(`${API_BASE_URL}/api/admin/events`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Admin: Approve or Reject event
   */
  async updateAdminEventStatus(eventId, status) {
    const response = await fetch(`${API_BASE_URL}/api/admin/events/${eventId}/status`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify({ status })
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  // ==========================================
  // Feedback Module APIs (Team B & C)
  // ==========================================

  /**
   * Submit feedback for a completed session
   */
  async submitFeedback({ sessionId, rating, comments }) {
    const response = await fetch(`${API_BASE_URL}/api/feedback`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...this.getAuthHeader()
      },
      body: JSON.stringify({ sessionId, rating, comments })
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Fetch feedback for a specific session
   */
  async getSessionFeedback(sessionId) {
    const response = await fetch(`${API_BASE_URL}/api/feedback/session/${sessionId}`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Fetch reviews & rating summary for a mentor
   */
  async getMentorFeedback(mentorId) {
    const response = await fetch(`${API_BASE_URL}/api/feedback/mentor/${mentorId}`, {
      method: "GET",
      headers: {
        "Accept": "application/json"
      }
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  },

  /**
   * Admin: Fetch all platform feedback
   */
  async getAllFeedback() {
    const response = await fetch(`${API_BASE_URL}/api/feedback`, {
      method: "GET",
      headers: {
        "Accept": "application/json",
        ...this.getAuthHeader()
      }
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, ok: response.ok, data };
  }
};


