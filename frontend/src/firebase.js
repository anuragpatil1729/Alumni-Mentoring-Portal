// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics, isSupported } from "firebase/analytics";
import { getAuth, GoogleAuthProvider } from "firebase/auth";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyDh5mga4iLPM4VlnxDSLX6CJjkZw7nX0Sk",
  authDomain: "alumni-mentoring-portal.firebaseapp.com",
  projectId: "alumni-mentoring-portal",
  storageBucket: "alumni-mentoring-portal.firebasestorage.app",
  messagingSenderId: "517174234289",
  appId: "1:517174234289:web:719e2bb531e057f129a690",
  measurementId: "G-XPLK04XB0L"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Auth & Google Provider
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();

// Initialize Analytics conditionally
export let analytics;
if (typeof window !== "undefined") {
  isSupported().then((supported) => {
    if (supported) {
      analytics = getAnalytics(app);
    }
  });
}

export default app;
