import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { TitleStrategy, provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { provideApiConfig } from './core/api/api-config';
import { AppConfig } from './core/config/app-config';
import { authInterceptor } from './core/http/auth-interceptor';
import { errorInterceptor } from './core/http/error-interceptor';
import { provideI18n } from './core/i18n/i18n';
import { TranslatedTitleStrategy } from './core/i18n/translated-title-strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideApiConfig(),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideI18n(),
    { provide: TitleStrategy, useExisting: TranslatedTitleStrategy },
    provideAppInitializer(() => {
      inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined');
    }),
    provideAppInitializer(() => inject(AppConfig).load())
  ]
};
