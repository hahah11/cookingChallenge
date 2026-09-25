import { TestBed } from '@angular/core/testing';

import { ChallengeStatus } from '../../../core/api/generated';
import { StatusTag } from './status-tag';
import { provideTestI18n } from '../../../testing/i18n';

describe('StatusTag', () => {
  async function createComponent(status: ChallengeStatus) {
    await TestBed.configureTestingModule({
      providers: [...provideTestI18n()], imports: [StatusTag] }).compileComponents();
    const fixture = TestBed.createComponent(StatusTag);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    return fixture;
  }

  it('renders "Open" for an open challenge with the error override class and a leading check icon', async () => {
    const fixture = await createComponent(ChallengeStatus.OPEN);
    const chip = fixture.nativeElement.querySelector('mat-chip');
    expect(chip.querySelector('.status-tag__label').textContent.trim()).toBe('Open');
    expect(chip.querySelector('mat-icon').textContent.trim()).toBe('check');
    expect(chip.classList).toContain('status-tag--open');
    expect(chip.classList).not.toContain('status-tag--revealed');
  });

  it('renders "Scoring closed" for a closed challenge with the closed override class', async () => {
    const fixture = await createComponent(ChallengeStatus.CLOSED);
    const chip = fixture.nativeElement.querySelector('mat-chip');
    expect(chip.querySelector('.status-tag__label').textContent.trim()).toBe('Scoring closed');
    expect(chip.classList).toContain('status-tag--closed');
    expect(chip.classList).not.toContain('status-tag--open');
    expect(chip.classList).not.toContain('status-tag--revealed');
  });

  it('renders "Revealed" for a revealed challenge with the revealed override class', async () => {
    const fixture = await createComponent(ChallengeStatus.REVEALED);
    const chip = fixture.nativeElement.querySelector('mat-chip');
    expect(chip.querySelector('.status-tag__label').textContent.trim()).toBe('Revealed');
    expect(chip.classList).toContain('status-tag--revealed');
  });
});
