import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Ticket } from '../ticket.model';
import { TicketService } from '../ticket.service';
import { TicketForm } from './ticket-form';

describe('TicketForm', () => {
  const create = vi.fn();

  beforeEach(() => {
    create.mockReset();
    TestBed.configureTestingModule({
      imports: [TicketForm],
      providers: [provideRouter([]), { provide: TicketService, useValue: { create } }],
    });
  });

  function fillAndSubmit(email: string): HTMLElement {
    const fixture = TestBed.createComponent(TicketForm);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    const set = (selector: string, value: string) => {
      const input = element.querySelector(selector) as HTMLInputElement | HTMLTextAreaElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    set('input[formControlName="customerEmail"]', email);
    set('input[formControlName="subject"]', 'Refund please');
    set('textarea[formControlName="body"]', 'I was charged twice.');

    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    return element;
  }

  it('does not submit an invalid email', () => {
    const element = fillAndSubmit('not-an-email');

    expect(create).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Enter a valid email address');
  });

  it('creates the ticket and opens its detail page', () => {
    create.mockReturnValue(of({ ticket: { id: 42 } as Ticket, warning: null }));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fillAndSubmit('anna@example.com');

    expect(create).toHaveBeenCalledWith({
      customerEmail: 'anna@example.com',
      subject: 'Refund please',
      body: 'I was charged twice.',
    });
    expect(navigate).toHaveBeenCalledWith(['/tickets', 42]);
  });
});
