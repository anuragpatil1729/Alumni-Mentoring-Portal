/**
 * App Controller (DOM View Manager & Event Orchestrator)
 */
let authService;

// Initialize App on DOM Content Loaded
document.addEventListener('DOMContentLoaded', () => {
  authService = new AuthService();
  
  // Set default dynamic fields for registration form (Student role default)
  updateDynamicRoleFields('student');

  // Check if active authenticated session exists
  checkActiveSession();
});

/**
 * Switch Auth Tab (Sign In vs Register)
 */
function switchAuthTab(tab) {
  clearAlert();
  const loginTab = document.getElementById('tab-login');
  const registerTab = document.getElementById('tab-register');
  const loginForm = document.getElementById('login-form');
  const registerForm = document.getElementById('register-form');

  if (tab === 'login') {
    loginTab.classList.add('active');
    registerTab.classList.remove('active');
    loginForm.classList.remove('hidden');
    registerForm.classList.add('hidden');
  } else {
    registerTab.classList.add('active');
    loginTab.classList.remove('active');
    registerForm.classList.remove('hidden');
    loginForm.classList.add('hidden');
  }
}

/**
 * Render Dynamic Fields for Registration depending on selected Role
 */
function updateDynamicRoleFields(role) {
  const container = document.getElementById('dynamic-fields');
  container.innerHTML = '';

  if (role === 'student') {
    container.innerHTML = `
      <div class="form-group">
        <label for="reg-student-id">Student ID</label>
        <div class="input-wrapper">
          <input type="text" id="reg-student-id" placeholder="e.g. STU-2024-889" required>
        </div>
      </div>
      <div class="form-group">
        <label for="reg-major">Major / Field of Study</label>
        <div class="input-wrapper">
          <input type="text" id="reg-major" placeholder="e.g. Computer Science" required>
        </div>
      </div>
      <div class="form-group">
        <label for="reg-grad-year">Expected Graduation Year</label>
        <div class="input-wrapper">
          <input type="number" id="reg-grad-year" placeholder="2026" min="2024" max="2035" required>
        </div>
      </div>
    `;
  } else if (role === 'alumni') {
    container.innerHTML = `
      <div class="form-group">
        <label for="reg-company">Current Company / Organization</label>
        <div class="input-wrapper">
          <input type="text" id="reg-company" placeholder="e.g. Google, Microsoft" required>
        </div>
      </div>
      <div class="form-group">
        <label for="reg-job-title">Job Title / Designation</label>
        <div class="input-wrapper">
          <input type="text" id="reg-job-title" placeholder="e.g. Software Engineer" required>
        </div>
      </div>
      <div class="form-group">
        <label for="reg-grad-year">Alumni Graduation Year</label>
        <div class="input-wrapper">
          <input type="number" id="reg-grad-year" placeholder="2020" min="1980" max="2024" required>
        </div>
      </div>
    `;
  } else if (role === 'admin') {
    container.innerHTML = `
      <div class="form-group">
        <label for="reg-admin-code">Administrator Security Code</label>
        <div class="input-wrapper">
          <input type="text" id="reg-admin-code" placeholder="e.g. ADM-KEY-99" required>
        </div>
      </div>
    `;
  }
}

/**
 * Handle Login Form Submission
 */
function handleLogin(event) {
  event.preventDefault();
  clearAlert();

  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;

  const result = authService.login(email, password);

  if (result.success) {
    showAlert(result.message, 'success');
    setTimeout(() => {
      renderDashboard(result.user, result.tokenData);
    }, 400);
  } else {
    showAlert(result.message, 'error');
  }
}

/**
 * Handle Registration Form Submission
 */
function handleRegister(event) {
  event.preventDefault();
  clearAlert();

  const name = document.getElementById('reg-name').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;
  const role = document.querySelector('input[name="reg-role"]:checked').value;

  const userData = {
    name,
    email,
    password,
    role
  };

  if (role === 'student') {
    userData.studentId = document.getElementById('reg-student-id').value.trim();
    userData.major = document.getElementById('reg-major').value.trim();
    userData.graduationYear = document.getElementById('reg-grad-year').value.trim();
  } else if (role === 'alumni') {
    userData.company = document.getElementById('reg-company').value.trim();
    userData.jobTitle = document.getElementById('reg-job-title').value.trim();
    userData.graduationYear = document.getElementById('reg-grad-year').value.trim();
  } else if (role === 'admin') {
    userData.adminCode = document.getElementById('reg-admin-code').value.trim();
  }

  const result = authService.register(userData);

  if (result.success) {
    showAlert(result.message, 'success');
    // Switch to login tab and auto-fill email
    setTimeout(() => {
      switchAuthTab('login');
      document.getElementById('login-email').value = email;
      document.getElementById('login-password').value = password;
      showAlert('Account created successfully! Click "Sign In" below.', 'success');
    }, 600);
  } else {
    showAlert(result.message, 'error');
  }
}



/**
 * Check Active Session on Page Reload
 */
function checkActiveSession() {
  const session = authService.getCurrentSession();
  if (session.authenticated) {
    const token = TokenManager.getSavedToken();
    const verification = TokenManager.verifyToken(token);
    renderDashboard(session.user, {
      token: token,
      payload: verification.payload,
      expiresAt: new Date(verification.payload.exp * 1000).toISOString()
    });
  } else {
    showAuthView();
  }
}

/**
 * Render Logged-In User Dashboard
 */
function renderDashboard(user, tokenData) {
  clearAlert();
  document.getElementById('auth-view').classList.add('hidden');
  const dashView = document.getElementById('dashboard-view');
  dashView.classList.remove('hidden');

  // Avatar initial
  document.getElementById('user-avatar').textContent = user.getName().charAt(0).toUpperCase();
  document.getElementById('user-name').textContent = user.getName();
  document.getElementById('user-email').textContent = user.getEmail();

  // Role Badge & polymorphism
  const roleDetails = user.getRoleDetails();
  const badgeElem = document.getElementById('user-badge');
  badgeElem.textContent = roleDetails.roleTitle;
  badgeElem.className = `badge ${roleDetails.badgeClass}`;

  // Populate Role Attributes Grid
  const detailsGrid = document.getElementById('user-details-grid');
  detailsGrid.innerHTML = '';

  for (const [key, value] of Object.entries(roleDetails.attributes)) {
    const item = document.createElement('div');
    item.className = 'detail-item';
    item.innerHTML = `
      <div class="detail-label">${key}</div>
      <div class="detail-value">${value}</div>
    `;
    detailsGrid.appendChild(item);
  }

  // Common attribute item: User ID & Account Creation
  const idItem = document.createElement('div');
  idItem.className = 'detail-item';
  idItem.innerHTML = `
    <div class="detail-label">System User ID</div>
    <div class="detail-value" style="font-family: var(--font-mono); font-size: 0.85rem;">${user.getId()}</div>
  `;
  detailsGrid.appendChild(idItem);

  // Populate Token Box & Claims
  document.getElementById('token-box').textContent = tokenData.token;
  document.getElementById('token-claims').textContent = JSON.stringify(tokenData.payload, null, 2);
}

/**
 * Return to Auth View
 */
function showAuthView() {
  document.getElementById('dashboard-view').classList.add('hidden');
  document.getElementById('auth-view').classList.remove('hidden');
}

/**
 * Handle Logout
 */
function handleLogout() {
  authService.logout();
  showAuthView();
  showAlert('You have been logged out successfully.', 'success');
}

/**
 * Helper: Show Alert Message
 */
function showAlert(message, type = 'error') {
  const alertBox = document.getElementById('alert-box');
  alertBox.className = `alert-box alert-${type}`;
  alertBox.innerHTML = `
    <span>${type === 'error' ? '⚠️' : '✅'}</span>
    <span>${message}</span>
  `;
  alertBox.classList.remove('hidden');
}

/**
 * Helper: Clear Alert Message
 */
function clearAlert() {
  const alertBox = document.getElementById('alert-box');
  alertBox.classList.add('hidden');
}
