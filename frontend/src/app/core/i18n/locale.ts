export const SUPPORTED_LOCALES = ['en', 'de'] as const;

export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];

export const DEFAULT_LOCALE: SupportedLocale = 'en';

/**
 * Picks the app language from the browser's preference list: the first entry whose primary
 * subtag is supported wins (`de-AT`, `de-CH` → `de`), so `['fr', 'de']` gives German rather than
 * skipping to the default. Anything unsupported falls back to English. This is the single source
 * of truth for the UI language, the `LOCALE_ID` (date formats) and the language sent to the backend.
 */
export function detectLocale(languages: readonly string[] = browserLanguages()): SupportedLocale {
  for (const tag of languages) {
    const primary = tag.trim().toLowerCase().split(/[-_]/)[0];
    const match = SUPPORTED_LOCALES.find((locale) => locale === primary);
    if (match) {
      return match;
    }
  }
  return DEFAULT_LOCALE;
}

function browserLanguages(): readonly string[] {
  if (typeof navigator === 'undefined') {
    return [];
  }
  return navigator.languages?.length
    ? navigator.languages
    : navigator.language
      ? [navigator.language]
      : [];
}
