import { TestBed } from '@angular/core/testing';

import { ChallengeStatus } from '../../../core/api/generated';
import { StatusTag } from './status-tag';

describe('StatusTag', () => {
  async function createComponent(status: ChallengeStatus) {
    await TestBed.configureTestingModule({ imports: [StatusTag] }).compileComponents();
    const fixture = TestBed.createComponent(StatusTag);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    return fixture;
  }

  it('renders "Open" for an open challenge, not highlighted', async () => {
    const fixture = await createComponent(ChallengeStatus.OPEN);
    const chip = fixture.nativeElement.querySelector('mat-chip');
    expect(chip.textContent.trim()).toBe('Open');
    expect(chip.classList).not.toContain('status-tag--revealed');
  });

  it('renders "Revealed" for a revealed challenge with the success override class', async () => {
    const fixture = await createComponent(ChallengeStatus.REVEALED);
    const chip = fixture.nativeElement.querySelector('mat-chip');
    expect(chip.textContent.trim()).toBe('Revealed');
    expect(chip.classList).toContain('status-tag--revealed');
  });
});
