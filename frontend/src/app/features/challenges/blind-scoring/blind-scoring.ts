import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';

import {
  Category,
  ChallengesApi,
  ChallengeStatus,
  DishLabel,
  ParticipantChallenge,
  PlateColor,
  ScoreEntry
} from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { LoadingSkeleton } from '../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { StarRating } from '../../../shared/components/star-rating/star-rating';

const CATEGORY_LABELS: Record<Category, string> = {
  [Category.MUNDGEFUEHL]: 'Mundgefühl',
  [Category.TELLERSPRACHE]: 'Tellersprache',
  [Category.GESCHMACK]: 'Geschmack'
};

type LoadState = 'loading' | 'loaded' | 'error';

/**
 * `GET /challenges/{id}` (the participant-blind view), pre-filled from `mySubmission` for
 * edit-until-reveal. `accountId` on `participantCookAssignments` is null until reveal — the
 * grid identifies dishes by plate color only, never by cook name, per the frontend plan's
 * Phase 6.
 */
@Component({
  selector: 'app-blind-scoring',
  imports: [
    ErrorState,
    LoadingSkeleton,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    PageHeader,
    RouterLink,
    StarRating
  ],
  templateUrl: './blind-scoring.html',
  styleUrl: './blind-scoring.scss'
})
export class BlindScoring {
  private readonly challengesApi = inject(ChallengesApi);
  private readonly appConfig = inject(AppConfig);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly CATEGORY_LABELS = CATEGORY_LABELS;

  protected readonly loadState = signal<LoadState>('loading');
  protected readonly loadErrorMessage = signal('');
  protected readonly challenge = signal<ParticipantChallenge | null>(null);

  protected readonly scoreValues = signal<Record<string, number | null>>({});
  protected readonly revealedMidEdit = signal(false);
  protected readonly submitting = signal(false);
  protected readonly submitErrorMessage = signal<string | null>(null);
  protected readonly submitted = signal(false);

  protected readonly submitButtonLabel = computed(() =>
    this.challenge()?.mySubmission ? 'Save changes' : 'Submit scores'
  );

  protected readonly instructionText = computed<string>(() => {
    const challenge = this.challenge();
    if (!challenge || challenge.labels.length < 2) return '';
    const [first, second] = challenge.labels;
    return (
      `Blind tasting — rate the ${this.plateLabel(first).toLowerCase()} plate and the ` +
      `${this.plateLabel(second).toLowerCase()} plate, 1–5 stars each, without knowing who cooked which.`
    );
  });

  constructor() {
    effect(() => {
      this.id();
      this.loadChallenge();
    });
  }

  protected loadChallenge(): void {
    const id = this.id();
    this.loadState.set('loading');
    this.challengesApi.getChallenge(id).subscribe({
      next: (response) => {
        this.challenge.set(response.data);
        this.loadState.set('loaded');
        this.revealedMidEdit.set(response.data.status === ChallengeStatus.REVEALED);
        this.initScores(response.data);
      },
      error: (error: ApiError) => {
        this.loadErrorMessage.set(error.message);
        this.loadState.set('error');
      }
    });
  }

  private initScores(challenge: ParticipantChallenge): void {
    const values: Record<string, number | null> = {};
    for (const category of challenge.categories) {
      for (const label of challenge.labels) {
        const existing = challenge.mySubmission?.scores.find(
          (score) => score.category === category && score.dishLabel === label
        );
        values[this.keyFor(category, label)] = existing?.points ?? null;
      }
    }
    this.scoreValues.set(values);
  }

  protected keyFor(category: Category, label: DishLabel): string {
    return `${category}:${label}`;
  }

  protected setScore(category: Category, label: DishLabel, value: number | null): void {
    this.scoreValues.set({ ...this.scoreValues(), [this.keyFor(category, label)]: value });
  }

  protected colorFor(label: DishLabel): PlateColor | null {
    const colorId = this.challenge()?.participantCookAssignments.find((a) => a.label === label)?.colorId ?? null;
    if (!colorId) return null;
    return this.appConfig.plateColors().find((color) => color.id === colorId) ?? null;
  }

  protected plateLabel(label: DishLabel): string {
    return this.colorFor(label)?.name ?? `Dish ${label}`;
  }

  protected canSubmit(): boolean {
    const challenge = this.challenge();
    if (!challenge) return false;
    const values = this.scoreValues();
    return challenge.categories.every((category) =>
      challenge.labels.every((label) => values[this.keyFor(category, label)] != null)
    );
  }

  protected submit(): void {
    const challenge = this.challenge();
    if (!challenge || !this.canSubmit() || this.submitting()) return;

    this.submitting.set(true);
    this.submitErrorMessage.set(null);

    const values = this.scoreValues();
    const scores: ScoreEntry[] = [];
    for (const category of challenge.categories) {
      for (const label of challenge.labels) {
        scores.push({ dishLabel: label, category, points: values[this.keyFor(category, label)]! });
      }
    }

    this.challengesApi.submitScores(challenge.id, { scores }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        if (error.status === 409) {
          // Someone revealed mid-edit — show that, don't retry.
          this.revealedMidEdit.set(true);
        } else {
          this.submitErrorMessage.set(error.message);
        }
      }
    });
  }

  protected viewResults(): void {
    const challenge = this.challenge();
    if (challenge) {
      void this.router.navigate(['/challenges', challenge.id, 'results']);
    }
  }

  protected goHome(): void {
    void this.router.navigateByUrl('/home');
  }
}
