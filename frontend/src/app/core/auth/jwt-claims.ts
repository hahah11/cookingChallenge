import { SystemRole } from '../api/generated';

/**
 * Shape of `JwtIssuer`'s claims (backend `auth.application.service.JwtIssuer`) — decode only,
 * never verify; the backend is the sole authority on validity.
 */
export interface JwtClaims {
  iss: string;
  iat: number;
  exp: number;
  sub: string;
  roles: SystemRole[];
}

function base64UrlDecode(base64Url: string): string {
  const padded = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const binary = atob(padded);
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

export function decodeJwtClaims(token: string): JwtClaims | null {
  const payload = token.split('.')[1];
  if (!payload) {
    return null;
  }

  try {
    return JSON.parse(base64UrlDecode(payload)) as JwtClaims;
  } catch {
    return null;
  }
}

export function isJwtExpired(claims: JwtClaims): boolean {
  return claims.exp * 1000 <= Date.now();
}
