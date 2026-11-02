import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { Priority } from '../ticket.model';

/** A Material chip coloured by priority (colours are defined in priority-chip.scss). */
@Component({
  selector: 'app-priority-chip',
  imports: [MatChipsModule],
  template: `<mat-chip [class]="'priority-' + priority().toLowerCase()" disableRipple>{{ priority() }}</mat-chip>`,
  styleUrl: './priority-chip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PriorityChip {
  readonly priority = input.required<Priority>();
}
