import de from '../../../../public/i18n/de.json';
import en from '../../../../public/i18n/en.json';
import { API_ERROR_CODES } from '../errors/api-error';

interface Tree {
  [key: string]: string | Tree;
}

function flatten(tree: Tree, prefix = ''): Record<string, string> {
  return Object.entries(tree).reduce<Record<string, string>>((acc, [key, value]) => {
    const path = `${prefix}${key}`;
    return typeof value === 'string'
      ? { ...acc, [path]: value }
      : { ...acc, ...flatten(value, `${path}.`) };
  }, {});
}

/** `{{ name }}` parameters used by a message, sorted, e.g. "count,name". */
function params(message: string): string {
  return [...message.matchAll(/\{\{\s*(\w+)\s*\}\}/g)]
    .map((match) => match[1])
    .sort()
    .join(',');
}

/**
 * Messages whose languages legitimately take different parameters. English lowercases the plate
 * color ("the red plate"), while German colors are nouns and must stay capitalised, so the German
 * text uses `first`/`second` where English uses `firstLower`/`secondLower`.
 */
const PARAMS_MAY_DIFFER = ['blindScoring.instructions'];

describe('translation files', () => {
  const english = flatten(en);
  const german = flatten(de);

  it('define exactly the same keys in English and German', () => {
    expect(Object.keys(german).sort()).toEqual(Object.keys(english).sort());
  });

  it('use the same {{parameters}} in both languages for every message', () => {
    for (const key of Object.keys(english).filter((k) => !PARAMS_MAY_DIFFER.includes(k))) {
      expect(params(german[key]), key).toBe(params(english[key]));
    }
  });

  it('contain no blank messages', () => {
    for (const [key, message] of [...Object.entries(english), ...Object.entries(german)]) {
      expect(message.trim(), key).not.toBe('');
    }
  });

  it('translate every API error code the UI can receive', () => {
    for (const code of API_ERROR_CODES) {
      expect(english[`errors.${code}`], code).toBeTruthy();
      expect(german[`errors.${code}`], code).toBeTruthy();
    }
  });

  it('are actually translated, not copies of the English text (spot check)', () => {
    expect(german['common.cancel']).not.toBe(english['common.cancel']);
    expect(german['login.submit']).not.toBe(english['login.submit']);
  });
});
