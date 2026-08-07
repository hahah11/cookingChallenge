import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { NavBar } from './nav-bar';

describe('NavBar', () => {
  it('renders one link per item with its icon and label', async () => {
    await TestBed.configureTestingModule({
      imports: [NavBar],
      providers: [provideRouter([])]
    }).compileComponents();

    const fixture = TestBed.createComponent(NavBar);
    fixture.componentRef.setInput('items', [
      { icon: 'history', label: 'History', route: '/challenges' },
      { icon: 'groups', label: 'Rivalries', route: '/rivalries' },
      { icon: 'person', label: 'Accounts', route: '/accounts' }
    ]);
    fixture.detectChanges();

    const links = fixture.nativeElement.querySelectorAll('a.nav-bar__item');
    expect(links.length).toBe(3);
    expect(links[0].textContent.trim()).toContain('History');
    expect(links[0].getAttribute('href')).toBe('/challenges');
  });
});
