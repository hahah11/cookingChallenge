import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { afterEach, beforeEach, vi } from 'vitest';

import { ChallengesApi } from '../../../core/api/generated';
import { ChallengePhoto } from './challenge-photo';

describe('ChallengePhoto', () => {
  const blob = new Blob(['fake-image'], { type: 'image/png' });
  let getChallengeImage: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    getChallengeImage = vi.fn().mockReturnValue(of(blob));
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:fake-url');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);

    await TestBed.configureTestingModule({
      imports: [ChallengePhoto],
      providers: [{ provide: ChallengesApi, useValue: { getChallengeImage } }]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('fetches the image blob and renders it as an object URL when hasImage is true', () => {
    const fixture = TestBed.createComponent(ChallengePhoto);
    fixture.componentRef.setInput('challengeId', 'chal-1');
    fixture.componentRef.setInput('hasImage', true);
    fixture.detectChanges();

    expect(getChallengeImage).toHaveBeenCalledWith('chal-1');
    const img = fixture.nativeElement.querySelector('img');
    expect(img.src).toContain('blob:fake-url');
  });

  it('renders a placeholder and skips the request when hasImage is false', () => {
    const fixture = TestBed.createComponent(ChallengePhoto);
    fixture.componentRef.setInput('challengeId', 'chal-2');
    fixture.componentRef.setInput('hasImage', false);
    fixture.detectChanges();

    expect(getChallengeImage).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.challenge-photo--placeholder')).not.toBeNull();
  });

  it('revokes the object URL on destroy', () => {
    const fixture = TestBed.createComponent(ChallengePhoto);
    fixture.componentRef.setInput('challengeId', 'chal-3');
    fixture.componentRef.setInput('hasImage', true);
    fixture.detectChanges();

    fixture.destroy();

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:fake-url');
  });
});
