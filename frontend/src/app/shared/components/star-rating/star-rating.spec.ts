import { TestBed } from '@angular/core/testing';

import { StarRating } from './star-rating';

describe('StarRating', () => {
  async function createComponent() {
    await TestBed.configureTestingModule({ imports: [StarRating] }).compileComponents();
    const fixture = TestBed.createComponent(StarRating);
    fixture.componentRef.setInput('label', 'Mundgefühl for dish A');
    fixture.detectChanges();
    return fixture;
  }

  it('renders 5 radio inputs sharing one group name', async () => {
    const fixture = await createComponent();
    const inputs = fixture.nativeElement.querySelectorAll('input[type="radio"]');
    expect(inputs.length).toBe(5);
    const names = new Set(Array.from(inputs).map((el) => (el as HTMLInputElement).name));
    expect(names.size).toBe(1);
  });

  it('starts with no value selected', async () => {
    const fixture = await createComponent();
    expect(fixture.componentInstance.value()).toBeNull();
    const checked = fixture.nativeElement.querySelectorAll('input:checked');
    expect(checked.length).toBe(0);
  });

  it('updates the value model when a star is picked', async () => {
    const fixture = await createComponent();
    const input = fixture.nativeElement.querySelector('input[value="3"]') as HTMLInputElement;
    input.checked = true;
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(fixture.componentInstance.value()).toBe(3);
  });

  it('renders a visually-hidden legend with the accessible label', async () => {
    const fixture = await createComponent();
    const legend = fixture.nativeElement.querySelector('legend');
    expect(legend.textContent.trim()).toBe('Mundgefühl for dish A');
  });

  it('ignores selection while disabled', async () => {
    const fixture = await createComponent();
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    fixture.componentInstance['select'](4);
    expect(fixture.componentInstance.value()).toBeNull();
  });
});
