import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

const toCanvas = vi.fn().mockResolvedValue(undefined);
vi.mock('qrcode', () => ({ toCanvas: (...args: unknown[]) => toCanvas(...args) }));

import { QrCode } from './qr-code';

describe('QrCode', () => {
  beforeEach(() => {
    toCanvas.mockClear();
  });

  it('renders the registration URL onto the canvas', async () => {
    await TestBed.configureTestingModule({ imports: [QrCode] }).compileComponents();
    const fixture = TestBed.createComponent(QrCode);
    fixture.componentRef.setInput('value', 'https://cookingchallenge.example/register?token=abc');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(toCanvas).toHaveBeenCalledTimes(1);
    const [canvas, value, options] = toCanvas.mock.calls[0];
    expect(canvas.tagName).toBe('CANVAS');
    expect(value).toBe('https://cookingchallenge.example/register?token=abc');
    expect(options).toMatchObject({ width: 240 });
  });

  it('sets an accessible label on the canvas', async () => {
    await TestBed.configureTestingModule({ imports: [QrCode] }).compileComponents();
    const fixture = TestBed.createComponent(QrCode);
    fixture.componentRef.setInput('value', 'https://cookingchallenge.example/register?token=abc');
    fixture.detectChanges();

    const canvas = fixture.nativeElement.querySelector('canvas');
    expect(canvas.getAttribute('role')).toBe('img');
    expect(canvas.getAttribute('aria-label')).toBe('Registration QR code');
  });
});
