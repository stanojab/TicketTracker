import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { CATEGORIES, PRIORITIES } from '../ticket.model';
import { TicketService } from '../ticket.service';
import { PriorityChip } from '../priority-chip/priority-chip';

/** Small summary of ticket counts, from GET /api/stats. */
@Component({
  selector: 'app-stats-panel',
  imports: [MatCardModule, PriorityChip],
  templateUrl: './stats-panel.html',
  styleUrl: './stats-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatsPanel {
  protected readonly categories = CATEGORIES;
  protected readonly priorities = PRIORITIES;
  protected readonly stats = toSignal(inject(TicketService).stats());
}
