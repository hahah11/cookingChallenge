import { formatDate } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationInitStatus, LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';
import { vi } from 'vitest';

import de from '../../../../public/i18n/de.json';
import en from '../../../../public/i18n/en.json';
import { provideI18n } from './i18n';

describe('provideI18n', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    document.documentElement.lang = '';
  });

  async function boot(languages: string[]) {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(languages);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n()],
    });
    const http = TestBed.inject(HttpTestingController);
    const init = TestBed.inject(ApplicationInitStatus);
    return { http, init };
  }

  it('sets up German end to end for a German browser', async () => {
    const { http, init } = await boot(['de-AT', 'en']);
    http.expectOne('/i18n/de.json').flush(de);
    await init.donePromise;

    expect(document.documentElement.lang).toBe('de');
    expect(TestBed.inject(LOCALE_ID)).toBe('de');
    expect(TestBed.inject(TranslocoService).translate('common.cancel')).toBe('Abbrechen');
    expect(formatDate(new Date(2026, 0, 5), 'mediumDate', TestBed.inject(LOCALE_ID))).toBe(
      '05.01.2026',
    );
  });

  it('sets up English for an English browser', async () => {
    const { http, init } = await boot(['en-US']);
    http.expectOne('/i18n/en.json').flush(en);
    await init.donePromise;

    expect(document.documentElement.lang).toBe('en');
    expect(TestBed.inject(LOCALE_ID)).toBe('en');
    expect(TestBed.inject(TranslocoService).translate('common.cancel')).toBe('Cancel');
  });

  it('falls back to English for a browser language we do not support', async () => {
    const { http, init } = await boot(['fr-FR']);
    http.expectOne('/i18n/en.json').flush(en);
    await init.donePromise;

    expect(document.documentElement.lang).toBe('en');
  });
});
