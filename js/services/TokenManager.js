/**
 * TokenManager Class (OOP Service for Session & Token Authentication)
 * Handles Token Generation (JWT-like structure), Verification, Expiration checks,
 * and Session persistence in browser storage.
 */
class TokenManager {
  static STORAGE_KEY = 'amp_auth_token';
  static SECRET_KEY = 'AlumniMentoringPortalSecretKey';

  /**
   * Helper to convert string to Base64 (browser safe)
   */
  static #toBase64(obj) {
    const jsonStr = JSON.stringify(obj);
    return btoa(encodeURIComponent(jsonStr));
  }

  /**
   * Helper to decode Base64 back to object
   */
  static #fromBase64(base64Str) {
    try {
      const jsonStr = decodeURIComponent(atob(base64Str));
      return JSON.parse(jsonStr);
    } catch (e) {
      return null;
    }
  }

  /**
   * Generates a Bearer Auth Token for a user with expiration
   * Token format: header.payload.signature
   */
  static generateToken(user, expiresInMinutes = 60) {
    const header = {
      alg: 'HS256',
      typ: 'JWT'
    };

    const now = Math.floor(Date.now() / 1000);
    const exp = now + (expiresInMinutes * 60);

    const payload = {
      sub: user.getId(),
      name: user.getName(),
      email: user.getEmail(),
      role: user.getRole(),
      iat: now,
      exp: exp
    };

    const encodedHeader = this.#toBase64(header);
    const encodedPayload = this.#toBase64(payload);
    
    // Simple signature simulation
    const rawSignature = `${encodedHeader}.${encodedPayload}.${this.SECRET_KEY}`;
    const signature = btoa(rawSignature).replace(/=/g, '').substring(0, 16);

    const token = `${encodedHeader}.${encodedPayload}.${signature}`;
    return {
      token: token,
      tokenType: 'Bearer',
      expiresAt: new Date(exp * 1000).toISOString(),
      expiresInSeconds: expiresInMinutes * 60,
      payload: payload
    };
  }

  /**
   * Verifies if a token is valid, correctly formatted, and not expired
   */
  static verifyToken(token) {
    if (!token || typeof token !== 'string') {
      return { valid: false, reason: 'No token provided' };
    }

    const parts = token.split('.');
    if (parts.length !== 3) {
      return { valid: false, reason: 'Malformed token structure' };
    }

    const [encodedHeader, encodedPayload, signature] = parts;
    const header = this.#fromBase64(encodedHeader);
    const payload = this.#fromBase64(encodedPayload);

    if (!header || !payload) {
      return { valid: false, reason: 'Invalid payload encoding' };
    }

    const now = Math.floor(Date.now() / 1000);
    if (payload.exp && payload.exp < now) {
      return { valid: false, reason: 'Token has expired', expired: true };
    }

    // Verify signature match
    const expectedRaw = `${encodedHeader}.${encodedPayload}.${this.SECRET_KEY}`;
    const expectedSig = btoa(expectedRaw).replace(/=/g, '').substring(0, 16);

    if (signature !== expectedSig) {
      return { valid: false, reason: 'Invalid token signature' };
    }

    return {
      valid: true,
      payload: payload
    };
  }

  /**
   * Save session token to storage
   */
  static saveSession(token) {
    localStorage.setItem(this.STORAGE_KEY, token);
  }

  /**
   * Retrieve active session token
   */
  static getSavedToken() {
    return localStorage.getItem(this.STORAGE_KEY);
  }

  /**
   * Clear session token (Logout)
   */
  static clearSession() {
    localStorage.removeItem(this.STORAGE_KEY);
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = TokenManager;
}
