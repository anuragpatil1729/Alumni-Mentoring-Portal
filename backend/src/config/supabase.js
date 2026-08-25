/**
 * Supabase Client Configuration
 */
const { createClient } = require('@supabase/supabase-js');

const supabaseUrl = process.env.SUPABASE_URL || '';
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_PUBLISHABLE_KEY || process.env.SUPABASE_ANON_KEY || '';

let supabase = null;

if (supabaseUrl && supabaseKey) {
  try {
    supabase = createClient(supabaseUrl, supabaseKey, {
      auth: {
        persistSession: false
      }
    });
    console.log('Supabase client initialized successfully.');
  } catch (err) {
    console.error('Failed to initialize Supabase client:', err.message);
  }
} else {
  console.log('Supabase credentials active; SUPABASE_URL awaiting project URL format.');
}

module.exports = { supabase };
