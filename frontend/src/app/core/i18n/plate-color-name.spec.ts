import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';

import { provideTestI18n } from '../../testing/i18n';
import { PlateColorNamePipe, translatePlateColor } from './plate-color-name';

describe('plate color names', () => {
  function setup(locale: 'en' | 'de') {
    TestBed.configureTestingModule({ providers: [...provideTestI18n(locale), PlateColorNamePipe] });
    return {
      transloco: TestBed.inject(TranslocoService),
      pipe: TestBed.inject(PlateColorNamePipe),
    };
  }

  it('translates the seeded colors into German', () => {
    const { pipe } = setup('de');

    expect(pipe.transform('Red')).toBe('Rot');
    expect(pipe.transform('Yellow')).toBe('Gelb');
  });

  it('keeps English names in English', () => {
    const { pipe } = setup('en');

    expect(pipe.transform('Red')).toBe('Red');
  });

  it('is case- and spacing-insensitive when looking up the key', () => {
    const { transloco } = setup('de');

    expect(translatePlateColor(transloco, 'RED')).toBe('Rot');
    expect(translatePlateColor(transloco, ' red ')).toBe('Rot');
  });

  it('falls back to the name the server sent for a color without a translation', () => {
    const { pipe, transloco } = setup('de');

    expect(pipe.transform('Turquoise')).toBe('Turquoise');
    expect(translatePlateColor(transloco, 'Light Blue')).toBe('Light Blue');
  });
});
