import { Locale } from '../api/generated';
import { detectLocale } from './locale';

/** The detected UI language as the API's `Locale` enum, for the `locale` field the backend stores per account. */
export function detectApiLocale(): Locale {
  return detectLocale() === 'de' ? Locale.DE : Locale.EN;
}
