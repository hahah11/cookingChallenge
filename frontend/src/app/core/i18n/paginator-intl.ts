import { Injectable, inject } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslocoService } from '@jsverse/transloco';

/** Material's paginator ships English labels; these come from the translation files instead. */
@Injectable()
export class TranslatedPaginatorIntl extends MatPaginatorIntl {
  private readonly transloco = inject(TranslocoService);

  constructor() {
    super();
    this.itemsPerPageLabel = this.transloco.translate('paginator.itemsPerPage');
    this.nextPageLabel = this.transloco.translate('paginator.next');
    this.previousPageLabel = this.transloco.translate('paginator.previous');
    this.firstPageLabel = this.transloco.translate('paginator.first');
    this.lastPageLabel = this.transloco.translate('paginator.last');
    this.getRangeLabel = (page, pageSize, length) => {
      if (length === 0 || pageSize === 0) {
        return this.transloco.translate('paginator.range', { start: 0, end: 0, length });
      }
      const start = page * pageSize + 1;
      const end = Math.min((page + 1) * pageSize, length);
      return this.transloco.translate('paginator.range', { start, end, length });
    };
  }
}
