import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { TicketService } from '../ticket.service';

/** "Paste a customer email" form. On submit the backend creates and AI-triages the ticket. */
@Component({
  selector: 'app-ticket-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './ticket-form.html',
  styleUrl: './ticket-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TicketForm {
  private readonly ticketService = inject(TicketService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  /** Same rules as the backend's Bean Validation annotations on CreateTicketRequest. */
  protected readonly form = inject(FormBuilder).nonNullable.group({
    customerEmail: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    subject: ['', [Validators.required, Validators.maxLength(200)]],
    body: ['', [Validators.required, Validators.maxLength(10_000)]],
  });

  protected readonly submitting = signal(false);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.ticketService.create(this.form.getRawValue()).subscribe({
      next: ({ ticket, warning }) => {
        if (warning) {
          this.snackBar.open(warning, 'OK', { duration: 10_000 });
        } else {
          this.snackBar.open('Ticket created and triaged by AI.', undefined, { duration: 3000 });
        }
        this.router.navigate(['/tickets', ticket.id]);
      },
      // The error interceptor already shows the message.
      error: () => this.submitting.set(false),
    });
  }
}
