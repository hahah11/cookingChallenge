import { HttpErrorResponse } from '@angular/common/http';

import { API_ERROR_CODES, toApiError } from './api-error';

const translate = (key: string) => `t(${key})`;

function httpError(body: unknown, status: number): HttpErrorResponse {
  return new HttpErrorResponse({ error: body, status });
}

describe('toApiError', () => {
  it('replaces the backend message with the translation for its code, keeping the original for diagnostics', () => {
    const error = toApiError(
      httpError(
        {
          error: { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password', details: [] },
        },
        401,
      ),
      translate,
    );

    expect(error.code).toBe('INVALID_CREDENTIALS');
    expect(error.message).toBe('t(errors.INVALID_CREDENTIALS)');
    expect(error.serverMessage).toBe('Invalid email or password');
    expect(error.status).toBe(401);
  });

  it('keeps the field details from a validation error', () => {
    const details = [{ field: 'email', message: 'must be a well-formed email address' }];
    const error = toApiError(
      httpError(
        { error: { code: 'VALIDATION_ERROR', message: 'Request validation failed', details } },
        400,
      ),
      translate,
    );

    expect(error.details).toEqual(details);
    expect(error.message).toBe('t(errors.VALIDATION_ERROR)');
  });

  it('uses the generic message for a code the UI does not know, rather than a raw key', () => {
    const error = toApiError(
      httpError({ error: { code: 'SOMETHING_NEW', message: 'English text', details: [] } }, 500),
      translate,
    );

    expect(error.code).toBe('SOMETHING_NEW');
    expect(error.message).toBe('t(errors.UNKNOWN_ERROR)');
  });

  it('turns a response without an error envelope (network failure) into UNKNOWN_ERROR', () => {
    const error = toApiError(httpError(null, 0), translate);

    expect(error.code).toBe('UNKNOWN_ERROR');
    expect(error.message).toBe('t(errors.UNKNOWN_ERROR)');
    expect(error.status).toBe(0);
  });

  it('has a translation key for every known code', () => {
    for (const code of API_ERROR_CODES) {
      const error = toApiError(
        httpError({ error: { code, message: 'x', details: [] } }, 400),
        translate,
      );
      expect(error.message).toBe(`t(errors.${code})`);
    }
  });
});
