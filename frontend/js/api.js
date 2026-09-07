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
  }
};
