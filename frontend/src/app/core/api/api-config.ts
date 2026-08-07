import { EnvironmentProviders } from '@angular/core';

import { provideApi } from './generated';

/**
 * Same-origin base path: `ng serve --proxy-config proxy.conf.json` forwards `/api` to the
 * backend in dev, and the built app is served from the same origin as the API in production —
 * see `openapi/cookingchallenge-api.yaml`'s `servers: [{ url: / }]`.
 */
export function provideApiConfig(): EnvironmentProviders {
  return provideApi('');
}
