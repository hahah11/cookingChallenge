import { Component, OnDestroy, effect, inject, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { ChallengesApi } from '../../../core/api/generated';

/**
 * Renders `GET /challenges/{id}/image` as a `Blob` object URL when `hasImage`
 * is true, else a placeholder. Object URLs are revoked on every re-fetch and
 * on destroy — the request never happens for cards known not to have a photo.
 */
@Component({
  selector: 'app-challenge-photo',
  imports: [MatIconModule],
  template: `
    @if (photoUrl(); as url) {
      <img [src]="url" [alt]="alt()" class="challenge-photo" />
    } @else {
      <div class="challenge-photo challenge-photo--placeholder" role="img" [attr.aria-label]="alt()">
        <mat-icon aria-hidden="true">restaurant</mat-icon>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
    }

    .challenge-photo {
      display: block;
      width: 100%;
      aspect-ratio: 4 / 3;
      object-fit: cover;
      background: var(--mat-sys-surface-variant);
    }

    .challenge-photo--placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--mat-sys-on-surface-variant);
    }

    .challenge-photo--placeholder mat-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
    }
  `
})
export class ChallengePhoto implements OnDestroy {
  private readonly challengesApi = inject(ChallengesApi);

  readonly challengeId = input.required<string>();
  readonly hasImage = input.required<boolean>();
  readonly alt = input('');

  protected readonly photoUrl = signal<string | null>(null);

  constructor() {
    effect((onCleanup) => {
      const challengeId = this.challengeId();
      const hasImage = this.hasImage();

      if (!hasImage) {
        this.setPhotoUrl(null);
        return;
      }

      const subscription = this.challengesApi
        .getChallengeImage(challengeId)
        .subscribe((blob) => this.setPhotoUrl(URL.createObjectURL(blob)));
      onCleanup(() => subscription.unsubscribe());
    });
  }

  ngOnDestroy(): void {
    this.setPhotoUrl(null);
  }

  private setPhotoUrl(url: string | null): void {
    const current = this.photoUrl();
    if (current) {
      URL.revokeObjectURL(current);
    }
    this.photoUrl.set(url);
  }
}
