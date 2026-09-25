import { detectLocale } from './locale';

describe('detectLocale', () => {
  it('picks German for plain and regional German tags', () => {
    expect(detectLocale(['de'])).toBe('de');
    expect(detectLocale(['de-AT'])).toBe('de');
    expect(detectLocale(['de-CH'])).toBe('de');
    expect(detectLocale(['DE_de'])).toBe('de');
  });

  it('picks English for English tags', () => {
    expect(detectLocale(['en-US'])).toBe('en');
    expect(detectLocale(['en'])).toBe('en');
  });

  it('falls back to English for unsupported languages and an empty list', () => {
    expect(detectLocale(['fr-FR'])).toBe('en');
    expect(detectLocale(['ja', 'es'])).toBe('en');
    expect(detectLocale([])).toBe('en');
  });

  it('takes the first supported entry in preference order, skipping unsupported ones', () => {
    expect(detectLocale(['fr', 'de', 'en'])).toBe('de');
    expect(detectLocale(['en-GB', 'de'])).toBe('en');
  });

  it('ignores blank entries', () => {
    expect(detectLocale(['', '  ', 'de-DE'])).toBe('de');
  });

  it('reads navigator.languages by default', () => {
    const spy = vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['de-AT', 'en']);
    try {
      expect(detectLocale()).toBe('de');
    } finally {
      spy.mockRestore();
    }
  });
});
