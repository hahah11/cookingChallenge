import { Component, ElementRef, effect, input, viewChild } from '@angular/core';
import * as QRCode from 'qrcode';

/**
 * Renders `value` (the opaque `registrationUrl`) as a QR code locally.
 * **Never** the prototype's external `api.qrserver.com` call with a raw
 * challengeId — that would let anyone self-register into any challenge, see
 * the frontend plan's "Deliberate deviations from the prototype".
 */
@Component({
  selector: 'app-qr-code',
  template: `<canvas #canvas class="qr-code" role="img" [attr.aria-label]="label()"></canvas>`,
  styles: `
    :host {
      display: inline-block;
    }

    .qr-code {
      display: block;
      max-width: 100%;
      height: auto;
      border-radius: var(--md-sys-shape-corner-medium);
    }
  `
})
export class QrCode {
  private readonly canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  readonly value = input.required<string>();
  readonly label = input('Registration QR code');
  readonly size = input(240);

  constructor() {
    effect(() => {
      const canvas = this.canvasRef().nativeElement;
      const value = this.value();
      const size = this.size();
      QRCode.toCanvas(canvas, value, { width: size, margin: 1 }).catch(() => {
        // Rendering only fails for oversized/invalid payloads; the canvas
        // simply stays blank, which is visible and non-destructive.
      });
    });
  }
}
