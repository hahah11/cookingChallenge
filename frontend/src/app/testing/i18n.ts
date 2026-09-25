import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import {
  EnvironmentProviders,
  LOCALE_ID,
  Provider,
  inject,
  provideAppInitializer,
} from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import {
  Translation,
  TranslocoLoader,
  TranslocoService,
  provideTransloco,
} from '@jsverse/transloco';
import { Observable, of } from 'rxjs';

import de from '../../../public/i18n/de.json';
import en from '../../../public/i18n/en.json';
import { SupportedLocale } from '../core/i18n/locale';
import { TranslatedPaginatorIntl } from '../core/i18n/paginator-intl';

const TRANSLATIONS: Record<SupportedLocale, Translation> = { en, de };

class StaticLoader implements TranslocoLoader {
  getTranslation(lang: string): Observable<Translation> {
    return of(TRANSLATIONS[lang as SupportedLocale]);
  }
}

/**
 * Real translation files, loaded synchronously — specs assert on the actual English (or German)
 * copy without any HTTP. Add to a TestBed's `providers`; pass `'de'` to render in German.
 */
export function provideTestI18n(
  locale: SupportedLocale = 'en',
): (Provider | EnvironmentProviders)[] {
  registerLocaleData(localeDe);
  return [
    provideTransloco({
      config: {
        availableLangs: ['en', 'de'],
        defaultLang: locale,
        fallbackLang: 'en',
        prodMode: true,
      },
      loader: StaticLoader,
    }),
    { provide: LOCALE_ID, useValue: locale },
    { provide: MatPaginatorIntl, useClass: TranslatedPaginatorIntl },
    provideAppInitializer(() => inject(TranslocoService).load(locale)),
  ];
}
