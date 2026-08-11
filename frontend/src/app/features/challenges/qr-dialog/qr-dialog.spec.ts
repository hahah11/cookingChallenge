import { Overlay } from '@angular/cdk/overlay';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ChallengesApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { QrDialog, QrDialogData } from './qr-dialog';

describe('QrDialog', () => {
  async function setup(createRegistrationInvite: ReturnType<typeof vi.fn>) {
    await TestBed.configureTestingModule({
      imports: [QrDialog, MatDialogModule],
      providers: [
        Overlay,
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { challengeId: 'chal-1', challengeName: 'Summer cook-off' } satisfies QrDialogData
        },
        { provide: ChallengesApi, useValue: { createRegistrationInvite } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(QrDialog);
    fixture.detectChanges();
    await TestBed.inject(ApplicationRef).whenStable();
    return { fixture };
  }

  it('renders the QR code with the fetched registration URL', async () => {
    const createRegistrationInvite = vi
      .fn()
      .mockReturnValue(of({ data: { token: 'tok', registrationUrl: 'https://cookoff.example/register?token=tok' }, meta: {} }));
    const { fixture } = await setup(createRegistrationInvite);

    expect(createRegistrationInvite).toHaveBeenCalledWith('chal-1');
    expect(fixture.nativeElement.querySelector('app-qr-code')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Summer cook-off');
  });

  it('shows a retryable error state on failure', async () => {
    const apiError: ApiError = {
      code: 'UNKNOWN_ERROR',
      message: 'Network error',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 0
    };
    const { fixture } = await setup(vi.fn().mockReturnValue(throwError(() => apiError)));

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();
  });
});
