const { supabase } = require('../config/supabase');

/**
 * Syncs a registration payload to Supabase tables asynchronously.
 * Local MySQL remains primary and functional while attempting Supabase storage.
 */
async function syncRegistrationToSupabase(registration) {
  if (!supabase) {
    return { synced: false, message: 'Supabase client not initialized (requires SUPABASE_URL in backend/.env)' };
  }

  try {
    // 1. Insert into Supabase 'users' table
    const { data: userData, error: userError } = await supabase
      .from('users')
      .upsert({
        full_name: registration.fullName,
        email: registration.email,
        mobile_number: registration.mobileNumber,
        password_hash: registration.passwordHash,
        role: registration.role
      }, { onConflict: 'email' })
      .select('id')
      .maybeSingle();

    if (userError) {
      console.warn('Supabase users upsert note:', userError.message);
      // Fallback: try inserting into 'registrations' flat table
      const { error: flatError } = await supabase
        .from('registrations')
        .upsert({
          full_name: registration.fullName,
          email: registration.email,
          mobile_number: registration.mobileNumber,
          role: registration.role,
          department: registration.department,
          graduation_year: registration.graduationYear,
          company: registration.company,
          designation: registration.designation,
          linkedin_profile: registration.linkedInProfile,
          experience_years: registration.experienceYears,
          industry: registration.industry,
          skills: registration.skills,
          bio: registration.bio,
          max_mentees: registration.maxMentees
        }, { onConflict: 'email' });

      if (flatError) {
        console.warn('Supabase flat registrations upsert note:', flatError.message);
        return { synced: false, error: flatError.message };
      }
      return { synced: true, table: 'registrations' };
    }

    const userId = userData?.id;

    // 2. Insert into Supabase 'alumni' or 'students' table
    if (registration.role === 'alumni') {
      const { error: alumniError } = await supabase
        .from('alumni')
        .upsert({
          user_id: userId,
          department: registration.department,
          graduation_year: registration.graduationYear,
          company: registration.company,
          designation: registration.designation,
          linkedin_profile: registration.linkedInProfile,
          experience_years: registration.experienceYears,
          industry: registration.industry,
          skills: registration.skills,
          bio: registration.bio,
          max_mentees: registration.maxMentees
        });

      if (alumniError) console.warn('Supabase alumni upsert note:', alumniError.message);
    } else if (registration.role === 'student') {
      const { error: studentError } = await supabase
        .from('students')
        .upsert({
          user_id: userId,
          student_id: registration.studentId,
          department: registration.department,
          graduation_year: registration.graduationYear
        });

      if (studentError) console.warn('Supabase student upsert note:', studentError.message);
    }

    return { synced: true, userId };
  } catch (err) {
    console.warn('Supabase sync note:', err.message);
    return { synced: false, error: err.message };
  }
}

module.exports = { syncRegistrationToSupabase };
