import { HttpErrorResponse } from '@angular/common/http';

import { ErrorBody, ErrorResponse } from '../api/generated';

/**
 * Every `error.code` the backend emits (`GlobalExceptionHandler` plus the security handlers), plus
 * the client-side `UNKNOWN_ERROR` for failures that never reached it. Each has a translated
 * message under `errors.<CODE>`.
 */
export const API_ERROR_CODES = [
  'VALIDATION_ERROR',
  'INVALID_ARGUMENT',
  'INVALID_CREDENTIALS',
  'INVALID_OR_EXPIRED_LINK',
  'UNAUTHENTICATED',
  'FORBIDDEN',
  'NOT_A_PARTICIPANT',
  'NOT_FOUND',
  'ACCOUNT_ALREADY_EXISTS',
  'CHALLENGE_NOT_OPEN',
  'DUPLICATE_SUBMISSION',
  'INVALID_STATE',
  'PASSWORD_RESET_NOT_ELIGIBLE',
  'INTERNAL_ERROR',
  'UNKNOWN_ERROR'
] as const;

/**
 * Codes the UI switches on (see `docs/cookingChallenge/plans/frontend-implementation-plan.md`
 * Phase 2). The `string & {}` branch keeps the union open for codes not yet catalogued here,
 * without widening it to a plain `string` and losing autocomplete.
 */
export type ApiErrorCode = (typeof API_ERROR_CODES)[number] | (string & {});

/**
 * `message` is the translated, user-facing text for `code`; the backend's own English prose is
 * kept in `serverMessage` for logs only and must not be shown.
 */
export interface ApiError extends Omit<ErrorBody, 'code'> {
  code: ApiErrorCode;
  status: number;
  serverMessage?: string;
}

export type Translate = (key: string) => string;

function messageFor(code: string, translate: Translate): string {
  const known = (API_ERROR_CODES as readonly string[]).includes(code) ? code : 'UNKNOWN_ERROR';
  return translate(`errors.${known}`);
}

/** Network/CORS failures never reach the backend, so there is no error envelope to unwrap. */
function unknownApiError(httpError: HttpErrorResponse, translate: Translate): ApiError {
  return {
    code: 'UNKNOWN_ERROR',
    message: messageFor('UNKNOWN_ERROR', translate),
    serverMessage: httpError.message,
    details: [],
    requestId: '',
    timestamp: new Date().toISOString(),
    status: httpError.status
  };
}

export function toApiError(httpError: HttpErrorResponse, translate: Translate): ApiError {
  const body = httpError.error as Partial<ErrorResponse> | null;
  const errorBody = body?.error;

  if (!errorBody) {
    return unknownApiError(httpError, translate);
  }

  return {
    ...errorBody,
    message: messageFor(errorBody.code, translate),
    serverMessage: errorBody.message,
    status: httpError.status
  };
}
