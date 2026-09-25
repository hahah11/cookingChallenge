import { DOCUMENT, registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import {
  EnvironmentProviders,
  LOCALE_ID,
  inject,
  isDevMode,
  makeEnvironmentProviders,
  provideAppInitializer,
} from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslocoService, provideTransloco } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';

import { TranslatedPaginatorIntl } from './paginator-intl';
import { DEFAULT_LOCALE, SUPPORTED_LOCALES, detectLocale } from './locale';
import { TranslocoHttpLoader } from './transloco-loader';

/**
 * Wires the whole language setup from one detection: Transloco's active language, Angular's
 * `LOCALE_ID` (so `DatePipe` etc. format for German), `<html lang>`, and a blocking load of the
 * translation file so no screen ever renders raw keys.
 */
export function provideI18n(): EnvironmentProviders {
  registerLocaleData(localeDe);
  const locale = detectLocale();

  return makeEnvironmentProviders([
    provideTransloco({
      config: {
        availableLangs: [...SUPPORTED_LOCALES],
        defaultLang: locale,
        fallbackLang: DEFAULT_LOCALE,
        reRenderOnLangChange: false,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
    { provide: LOCALE_ID, useValue: locale },
    { provide: MatPaginatorIntl, useClass: TranslatedPaginatorIntl },
    provideAppInitializer(() => {
      inject(DOCUMENT).documentElement.lang = locale;
      return firstValueFrom(inject(TranslocoService).load(locale));
    }),
  ]);
}
