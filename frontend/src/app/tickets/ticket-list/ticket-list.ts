import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { catchError, of, startWith, switchMap } from 'rxjs';
import { PriorityChip } from '../priority-chip/priority-chip';
import { StatsPanel } from '../stats-panel/stats-panel';
import { CATEGORIES, PRIORITIES, STATUSES, Ticket, TicketFilters } from '../ticket.model';
import { TicketService } from '../ticket.service';

@Component({
  selector: 'app-ticket-list',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
    PriorityChip,
    StatsPanel,
  ],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TicketList {
  private readonly ticketService = inject(TicketService);

  protected readonly categories = CATEGORIES;
  protected readonly priorities = PRIORITIES;
  protected readonly statuses = STATUSES;
  protected readonly columns = ['createdAt', 'subject', 'customerEmail', 'category', 'priority', 'status'];

  /** '' means "All". */
  protected readonly filters = inject(FormBuilder).nonNullable.group<TicketFilters>({
    category: '',
    priority: '',
    status: '',
  });

  /**
   * Every filter change triggers a new request. switchMap cancels the previous,
   * still-running request, so a slow old response can never overwrite a newer one.
   */
  protected readonly tickets = toSignal(
    this.filters.valueChanges.pipe(
      startWith(this.filters.getRawValue()),
      switchMap((filters) =>
        this.ticketService.list(filters).pipe(catchError(() => of([] as Ticket[]))),
      ),
    ),
  );

  protected clearFilters(): void {
    this.filters.reset();
  }
}
