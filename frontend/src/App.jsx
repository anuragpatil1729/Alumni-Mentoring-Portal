import { useState } from "react";
import { signInWithPopup } from "firebase/auth";
import { auth, googleProvider } from "./firebase";

function App() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    mobileNumber: "",
    password: "",
    confirmPassword: "",
    department: "",
    graduationYear: "",
    experience: "",
    company: "",
    designation: "",
    linkedInProfile: "",
    industry: "",
    skills: "",
    bio: "",
    maxMentees: ""
  });

  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const handleGoogleSignIn = async () => {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      setStatusMessage({
        type: "success",
        text: `Google Sign-In successful! Welcome, ${user.displayName || user.email}.`
      });
    } catch (error) {
      console.error("Google Sign-In Error:", error);
      setStatusMessage({
        type: "error",
        text: `Google Sign-In failed: ${error.message}`
      });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setStatusMessage(null);
    setFieldErrors({});

    if (formData.password !== formData.confirmPassword) {
      setFieldErrors({ confirmPassword: "Passwords do not match." });
      return;
    }

    setLoading(true);

    try {
      const payload = {
        fullName: formData.name,
        email: formData.email,
        mobileNumber: formData.mobileNumber,
        password: formData.password,
        role: "alumni",
        graduationYear: Number(formData.graduationYear),
        department: formData.department,
        company: formData.company,
        designation: formData.designation || "Alumni Mentor",
        linkedInProfile: formData.linkedInProfile || null,
        experienceYears: formData.experience ? Number(formData.experience) : null,
        industry: formData.industry || null,
        skills: formData.skills || null,
        bio: formData.bio || null,
        maxMentees: formData.maxMentees ? Number(formData.maxMentees) : null
      };

      const response = await fetch("http://localhost:5001/api/auth/register/alumni", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (response.ok && data.success) {
        setStatusMessage({
          type: "success",
          text: data.message || "Alumni account created successfully!"
        });
        setFormData({
          name: "",
          email: "",
          mobileNumber: "",
          password: "",
          confirmPassword: "",
          department: "",
          graduationYear: "",
          experience: "",
          company: "",
          designation: "",
          linkedInProfile: "",
          industry: "",
          skills: "",
          bio: "",
          maxMentees: ""
        });
      } else {
        if (data.errors) {
          setFieldErrors(data.errors);
        }
        setStatusMessage({
          type: "error",
          text: data.message || "Registration failed. Please check the inputs."
        });
      }
    } catch (err) {
      console.error("Registration error:", err);
      setStatusMessage({
        type: "error",
        text: "Could not connect to backend server. Make sure backend is running at http://localhost:5001"
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <header className="header">
        <div className="logo">
          Alumni<span>Connect</span>
        </div>
        <div className="header-text">Alumni Mentoring Portal</div>
      </header>

      <main className="main">
        <div className="form-container">
          <div className="form-header">
            <h1>Alumni Registration</h1>
            <p>Create your alumni profile and start mentoring students</p>
          </div>

          <div className="form-body">
            {statusMessage && (
              <div
                style={{
                  padding: "12px 16px",
                  borderRadius: "8px",
                  marginBottom: "20px",
                  fontSize: "14px",
                  fontWeight: "500",
                  backgroundColor: statusMessage.type === "success" ? "#d1fae5" : "#fee2e2",
                  color: statusMessage.type === "success" ? "#065f46" : "#991b1b",
                  border: `1px solid ${statusMessage.type === "success" ? "#a7f3d0" : "#fca5a5"}`
                }}
              >
                {statusMessage.text}
              </div>
            )}

            <button type="button" className="google-btn" onClick={handleGoogleSignIn}>
              <span className="google-icon">G</span>
              Continue with Google
            </button>

            <div className="divider">
              <span>OR REGISTER WITH EMAIL</span>
            </div>

            <form onSubmit={handleSubmit}>
              <section className="section">
                <div className="section-title">
                  <span className="section-number">1</span>
                  Personal Information
                </div>

                <div className="form-group">
                  <label htmlFor="name">
                    Full Name <span className="required">*</span>
                  </label>
                  <input
                    type="text"
                    id="name"
                    name="name"
                    value={formData.name}
                    onChange={handleInputChange}
                    placeholder="Enter your full name"
                    required
                  />
                  {fieldErrors.fullName && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.fullName}</div>}
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="email">
                      Email Address <span className="required">*</span>
                    </label>
                    <input
                      type="email"
                      id="email"
                      name="email"
                      value={formData.email}
                      onChange={handleInputChange}
                      placeholder="example@gmail.com"
                      required
                    />
                    {fieldErrors.email && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.email}</div>}
                  </div>

                  <div className="form-group">
                    <label htmlFor="mobileNumber">
                      Mobile Number <span className="required">*</span>
                    </label>
                    <input
                      type="tel"
                      id="mobileNumber"
                      name="mobileNumber"
                      value={formData.mobileNumber}
                      onChange={handleInputChange}
                      placeholder="e.g. 9876543210"
                      required
                    />
                    {fieldErrors.mobileNumber && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.mobileNumber}</div>}
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="password">
                      Password <span className="required">*</span>
                    </label>
                    <input
                      type="password"
                      id="password"
                      name="password"
                      value={formData.password}
                      onChange={handleInputChange}
                      placeholder="Create a password (min 8 chars)"
                      required
                    />
                    {fieldErrors.password && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.password}</div>}
                  </div>

                  <div className="form-group">
                    <label htmlFor="confirmPassword">
                      Confirm Password <span className="required">*</span>
                    </label>
                    <input
                      type="password"
                      id="confirmPassword"
                      name="confirmPassword"
                      value={formData.confirmPassword}
                      onChange={handleInputChange}
                      placeholder="Re-enter password"
                      required
                    />
                    {fieldErrors.confirmPassword && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.confirmPassword}</div>}
                  </div>
                </div>
              </section>

              <section className="section">
                <div className="section-title">
                  <span className="section-number">2</span>
                  Professional Information
                </div>

                <div className="form-group">
                  <label htmlFor="department">
                    Department / Branch <span className="required">*</span>
                  </label>
                  <input
                    type="text"
                    id="department"
                    name="department"
                    value={formData.department}
                    onChange={handleInputChange}
                    placeholder="e.g. Computer Engineering"
                    required
                  />
                  {fieldErrors.department && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.department}</div>}
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="graduationYear">
                      Graduation Year <span className="required">*</span>
                    </label>
                    <input
                      type="number"
                      id="graduationYear"
                      name="graduationYear"
                      value={formData.graduationYear}
                      onChange={handleInputChange}
                      placeholder="e.g. 2022"
                      min="1950"
                      max="2026"
                      required
                    />
                    {fieldErrors.graduationYear && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.graduationYear}</div>}
                  </div>

                  <div className="form-group">
                    <label htmlFor="experience">
                      Experience (Years) <span className="required">*</span>
                    </label>
                    <input
                      type="number"
                      id="experience"
                      name="experience"
                      value={formData.experience}
                      onChange={handleInputChange}
                      placeholder="e.g. 3"
                      min="0"
                      required
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="company">
                      Current Company <span className="required">*</span>
                    </label>
                    <input
                      type="text"
                      id="company"
                      name="company"
                      value={formData.company}
                      onChange={handleInputChange}
                      placeholder="Enter your current company"
                      required
                    />
                    {fieldErrors.company && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.company}</div>}
                  </div>

                  <div className="form-group">
                    <label htmlFor="designation">
                      Job Designation / Title <span className="required">*</span>
                    </label>
                    <input
                      type="text"
                      id="designation"
                      name="designation"
                      value={formData.designation}
                      onChange={handleInputChange}
                      placeholder="e.g. Senior Software Engineer"
                      required
                    />
                    {fieldErrors.designation && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.designation}</div>}
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="linkedInProfile">LinkedIn Profile URL</label>
                  <input
                    type="url"
                    id="linkedInProfile"
                    name="linkedInProfile"
                    value={formData.linkedInProfile}
                    onChange={handleInputChange}
                    placeholder="https://linkedin.com/in/yourprofile"
                  />
                  {fieldErrors.linkedInProfile && <div style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px' }}>{fieldErrors.linkedInProfile}</div>}
                </div>

                <div className="form-group">
                  <label htmlFor="industry">
                    Industry <span className="required">*</span>
                  </label>
                  <select
                    id="industry"
                    name="industry"
                    value={formData.industry}
                    onChange={handleInputChange}
                    required
                  >
                    <option value="" disabled>
                      Select your industry
                    </option>
                    <option value="IT">Information Technology</option>
                    <option value="Finance">Finance</option>
                    <option value="Healthcare">Healthcare</option>
                    <option value="Education">Education</option>
                    <option value="Consulting">Consulting</option>
                    <option value="Manufacturing">Manufacturing</option>
                    <option value="Other">Other</option>
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="skills">
                    Skills <span className="required">*</span>
                  </label>
                  <input
                    type="text"
                    id="skills"
                    name="skills"
                    value={formData.skills}
                    onChange={handleInputChange}
                    placeholder="e.g. Java, Python, SQL, React"
                    required
                  />
                  <div className="hint">Enter multiple skills separated by commas.</div>
                </div>
              </section>

              <section className="section">
                <div className="section-title">
                  <span className="section-number">3</span>
                  Mentorship Information
                </div>

                <div className="form-group">
                  <label htmlFor="bio">About You</label>
                  <textarea
                    id="bio"
                    name="bio"
                    value={formData.bio}
                    onChange={handleInputChange}
                    placeholder="Tell students about your experience, expertise and areas where you can provide mentorship..."
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="maxMentees">
                    Maximum Number of Mentees <span className="required">*</span>
                  </label>
                  <input
                    type="number"
                    id="maxMentees"
                    name="maxMentees"
                    value={formData.maxMentees}
                    onChange={handleInputChange}
                    placeholder="e.g. 5"
                    min="1"
                    required
                  />
                  <div className="hint">Maximum number of students you are willing to mentor.</div>
                </div>
              </section>

              <button type="submit" className="register-btn" disabled={loading}>
                {loading ? "Creating Account..." : "Create Alumni Account"}
              </button>

              <div className="login">
                Already have an account? <a href="#login">Login</a>
              </div>
            </form>
          </div>
        </div>

        <div className="footer">© 2026 AlumniConnect · Alumni Mentoring Portal</div>
      </main>
    </>
  );
}

export default App;
