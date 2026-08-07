import { HttpErrorResponse } from '@angular/common/http';

import { ErrorBody, ErrorResponse } from '../api/generated';

/**
 * Codes the UI switches on (see `docs/cookingChallenge/plans/frontend-implementation-plan.md`
 * Phase 2). The `string & {}` branch keeps the union open for codes not yet catalogued here,
 * without widening it to a plain `string` and losing autocomplete.
 */
export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'INVALID_CREDENTIALS'
  | 'INVALID_OR_EXPIRED_LINK'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'NOT_A_PARTICIPANT'
  | 'NOT_FOUND'
  | 'ACCOUNT_ALREADY_EXISTS'
  | 'CHALLENGE_NOT_OPEN'
  | 'INVALID_STATE'
  | (string & {});

export interface ApiError extends Omit<ErrorBody, 'code'> {
  code: ApiErrorCode;
  status: number;
}

/** Network/CORS failures never reach the backend, so there is no error envelope to unwrap. */
function unknownApiError(httpError: HttpErrorResponse): ApiError {
  return {
    code: 'UNKNOWN_ERROR',
    message: httpError.message || 'Something went wrong. Please try again.',
    details: [],
    requestId: '',
    timestamp: new Date().toISOString(),
    status: httpError.status
  };
}

export function toApiError(httpError: HttpErrorResponse): ApiError {
  const body = httpError.error as Partial<ErrorResponse> | null;
  const errorBody = body?.error;

  if (!errorBody) {
    return unknownApiError(httpError);
  }

  return { ...errorBody, status: httpError.status };
}
