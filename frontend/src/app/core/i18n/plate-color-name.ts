import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

/**
 * Plate colors come from the backend (`/config`) with English names. Known ones are translated
 * through `plateColor.<name>` (lowercased, letters and digits only — "Light Blue" → `lightblue`);
 * a color added later without a translation keeps the name the server sent.
 */
export function translatePlateColor(transloco: TranslocoService, name: string): string {
  const key = `plateColor.${name.toLowerCase().replace(/[^a-z0-9]/g, '')}`;
  const translated = transloco.translate(key);
  return translated === key ? name : translated;
}

@Pipe({ name: 'plateColorName' })
export class PlateColorNamePipe implements PipeTransform {
  private readonly transloco = inject(TranslocoService);

  transform(name: string): string {
    return translatePlateColor(this.transloco, name);
  }
}
