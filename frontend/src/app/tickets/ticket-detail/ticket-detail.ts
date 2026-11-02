import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { PriorityChip } from '../priority-chip/priority-chip';
import {
  CATEGORIES,
  Category,
  PRIORITIES,
  Priority,
  Ticket,
  TicketStatus,
  UpdateTicketRequest,
} from '../ticket.model';
import { TicketService } from '../ticket.service';

@Component({
  selector: 'app-ticket-detail',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    ClipboardModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    PriorityChip,
  ],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TicketDetail {
  private readonly ticketService = inject(TicketService);
  private readonly snackBar = inject(MatSnackBar);

  /** Bound from the ":id" route parameter (provideRouter(..., withComponentInputBinding())). */
  readonly id = input.required<string>();

  protected readonly categories = CATEGORIES;
  protected readonly priorities = PRIORITIES;
  protected readonly ticket = signal<Ticket | undefined>(undefined);
  protected readonly saving = signal(false);

  /** The fields an agent may correct. */
  protected readonly form = inject(FormBuilder).nonNullable.group({
    category: ['OTHER' as Category, Validators.required],
    priority: ['MEDIUM' as Priority, Validators.required],
    suggestedReply: ['', Validators.maxLength(5000)],
  });

  constructor() {
    toObservable(this.id)
      .pipe(
        switchMap((id) => this.ticketService.get(Number(id))),
        takeUntilDestroyed(),
      )
      .subscribe((ticket) => this.show(ticket));
  }

  protected save(): void {
    this.patch(this.form.getRawValue(), 'Changes saved.');
  }

  /** Saves any edits and changes the status in one PATCH. */
  protected setStatus(status: TicketStatus): void {
    const message = status === 'RESOLVED' ? 'Ticket resolved.' : 'Ticket reopened.';
    this.patch({ ...this.form.getRawValue(), status }, message);
  }

  private patch(changes: UpdateTicketRequest, message: string): void {
    const ticket = this.ticket();
    if (!ticket || this.form.invalid) {
      return;
    }
    this.saving.set(true);
    this.ticketService.update(ticket.id, changes).subscribe({
      next: (updated) => {
        this.show(updated);
        this.saving.set(false);
        this.snackBar.open(message, undefined, { duration: 3000 });
      },
      error: () => this.saving.set(false),
    });
  }

  private show(ticket: Ticket): void {
    this.ticket.set(ticket);
    this.form.reset({
      category: ticket.category,
      priority: ticket.priority,
      suggestedReply: ticket.suggestedReply ?? '',
    });
  }
}
