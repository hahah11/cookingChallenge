import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { Config, ConfigApi } from '../api/generated';

/**
 * Roles, plate colors, and feature flags come from `GET /api/v1/config`, fetched once via
 * `provideAppInitializer` and cached here — never hardcoded, per
 * `docs/frontend/01-architecture.md#app-configuration-and-permissions`.
 */
@Injectable({ providedIn: 'root' })
export class AppConfig {
  private readonly configApi = inject(ConfigApi);

  private readonly config = signal<Config | null>(null);

  readonly availableRoles = computed(() => this.config()?.availableRoles ?? []);
  readonly plateColors = computed(() => this.config()?.plateColors ?? []);
  readonly featureFlags = computed(() => this.config()?.featureFlags ?? {});

  load(): Observable<Config> {
    return this.configApi.getConfig().pipe(
      map((response) => response.data),
      tap((config) => this.config.set(config))
    );
  }
}
