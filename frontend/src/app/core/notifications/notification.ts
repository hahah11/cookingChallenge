import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

const DURATION_MS = 4000;

/** Thin wrapper around `MatSnackBar` so features never inject it directly. */
@Injectable({ providedIn: 'root' })
export class Notification {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.show(message);
  }

  info(message: string): void {
    this.show(message);
  }

  error(message: string): void {
    this.show(message);
  }

  private show(message: string): void {
    this.snackBar.open(message, 'Dismiss', { duration: DURATION_MS });
  }
}
