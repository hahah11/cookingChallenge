import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { expectNoAxeViolations } from '../../../testing/axe';
import { LinkExpired } from './link-expired';

describe('LinkExpired', () => {
  function setup() {
    TestBed.configureTestingModule({
      imports: [LinkExpired],
      providers: [provideRouter([])]
    });

    const fixture = TestBed.createComponent(LinkExpired);
    fixture.detectChanges();
    return { fixture };
  }

  it('shows the access-link copy by default', () => {
    const { fixture } = setup();

    expect(fixture.nativeElement.textContent).toContain('Link expired');
    expect(fixture.nativeElement.textContent).toContain('This link is no longer valid');
  });

  it('shows the QR copy when kind is qr', () => {
    const { fixture } = setup();
    fixture.componentRef.setInput('kind', 'qr');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('QR code expired');
    expect(fixture.nativeElement.textContent).toContain('This QR code is no longer valid');
  });

  it('links back to /login', () => {
    const { fixture } = setup();

    const link: HTMLAnchorElement = fixture.nativeElement.querySelector('.link-expired__action');
    expect(link.getAttribute('href')).toBe('/login');
  });

  it('shows the access-link copy when reached via router navigation with no kind param, exactly how errorInterceptor reaches it', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'link-expired', component: LinkExpired }], withComponentInputBinding())
      ]
    });

    const harness = await RouterTestingHarness.create('/link-expired');
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Link expired');
    expect(harness.routeNativeElement?.textContent).toContain('This link is no longer valid');
  });

  it(
    'has no axe violations',
    async () => {
      const { fixture } = setup();

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
