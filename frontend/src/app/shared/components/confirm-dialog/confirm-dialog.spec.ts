import { Overlay } from '@angular/cdk/overlay';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ConfirmDialog, ConfirmDialogData } from './confirm-dialog';

describe('ConfirmDialog', () => {
  let dialog: MatDialog;
  let appRef: ApplicationRef;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatDialogModule],
      providers: [Overlay]
    }).compileComponents();
    dialog = TestBed.inject(MatDialog);
    appRef = TestBed.inject(ApplicationRef);
  });

  it('resolves true when the confirm action is used', async () => {
    const data: ConfirmDialogData = {
      title: 'Reveal results?',
      message: 'This closes scoring. You can reopen it later if you need to.',
      confirmLabel: 'Reveal'
    };
    const ref = dialog.open(ConfirmDialog, { data });
    await appRef.whenStable();

    const buttons = document.querySelectorAll('button[mat-flat-button]');
    expect(buttons.length).toBe(1);
    expect(buttons[0].textContent?.trim()).toBe('Reveal');
    (buttons[0] as HTMLButtonElement).click();

    const result = await ref.afterClosed().toPromise();
    expect(result).toBe(true);
  });
});
