import { TestBed } from '@angular/core/testing';
import { PriorityChip } from './priority-chip';

describe('PriorityChip', () => {
  it('shows the priority with a matching colour class', async () => {
    const fixture = TestBed.createComponent(PriorityChip);
    fixture.componentRef.setInput('priority', 'URGENT');
    await fixture.whenStable();

    const chip = (fixture.nativeElement as HTMLElement).querySelector('mat-chip');
    expect(chip?.textContent).toContain('URGENT');
    expect(chip?.classList).toContain('priority-urgent');
  });
});
