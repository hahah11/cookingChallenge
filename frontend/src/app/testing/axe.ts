import { configureAxe } from 'vitest-axe';

// jsdom has no real layout/paint, so axe-core's color-contrast rule produces
// noise (false positives/negatives) rather than a meaningful check — contrast
// is verified against the design tokens instead, not per-component in jsdom.
const runAxe = configureAxe({ rules: { 'color-contrast': { enabled: false } } });

/**
 * `vitest-axe`'s `toHaveNoViolations` matcher types target Vitest's old `Vi`
 * global-namespace `Assertion`, which Vitest 4 no longer uses — so asserting
 * on `.violations` directly here instead of registering that matcher.
 */
export async function expectNoAxeViolations(element: Element): Promise<void> {
  const results = await runAxe(element);
  expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
}
