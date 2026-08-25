function App() {
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
            <button type="button" className="google-btn">
              <span className="google-icon">G</span>
              Continue with Google
            </button>

            <div className="divider">
              <span>OR REGISTER WITH EMAIL</span>
            </div>

            <form>
              <section className="section">
                <div className="section-title">
                  <span className="section-number">1</span>
                  Personal Information
                </div>

                <div className="form-group">
                  <label htmlFor="name">
                    Full Name <span className="required">*</span>
                  </label>
                  <input type="text" id="name" name="name" placeholder="Enter your full name" required />
                </div>

                <div className="form-group">
                  <label htmlFor="email">
                    Email Address <span className="required">*</span>
                  </label>
                  <input type="email" id="email" name="email" placeholder="example@gmail.com" required />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="password">
                      Password <span className="required">*</span>
                    </label>
                    <input type="password" id="password" name="password" placeholder="Create a password" required />
                  </div>

                  <div className="form-group">
                    <label htmlFor="confirm-password">
                      Confirm Password <span className="required">*</span>
                    </label>
                    <input
                      type="password"
                      id="confirm-password"
                      name="confirm-password"
                      placeholder="Re-enter password"
                      required
                    />
                  </div>
                </div>
              </section>

              <section className="section">
                <div className="section-title">
                  <span className="section-number">2</span>
                  Professional Information
                </div>

                <div className="form-group">
                  <label htmlFor="degree">
                    Degree / Qualification <span className="required">*</span>
                  </label>
                  <input
                    type="text"
                    id="degree"
                    name="degree_details"
                    placeholder="e.g. B.Tech Computer Engineering"
                    required
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label htmlFor="graduation">
                      Graduation Year <span className="required">*</span>
                    </label>
                    <input
                      type="number"
                      id="graduation"
                      name="graduation_year"
                      placeholder="e.g. 2022"
                      min="1950"
                      max="2030"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label htmlFor="experience">
                      Experience (Years) <span className="required">*</span>
                    </label>
                    <input type="number" id="experience" name="experience_years" placeholder="e.g. 3" min="0" required />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="company">
                    Current Company <span className="required">*</span>
                  </label>
                  <input type="text" id="company" name="company" placeholder="Enter your current company" required />
                </div>

                <div className="form-group">
                  <label htmlFor="industry">
                    Industry <span className="required">*</span>
                  </label>
                  <select id="industry" name="industry" required defaultValue="">
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
                  <input type="text" id="skills" name="skill_tags" placeholder="e.g. Java, Python, SQL, React" required />
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
                    placeholder="Tell students about your experience, expertise and areas where you can provide mentorship..."
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="mentees">
                    Maximum Number of Mentees <span className="required">*</span>
                  </label>
                  <input type="number" id="mentees" name="max_mentees" placeholder="e.g. 5" min="1" required />
                  <div className="hint">Maximum number of students you are willing to mentor.</div>
                </div>
              </section>

              <button type="submit" className="register-btn">
                Create Alumni Account
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
