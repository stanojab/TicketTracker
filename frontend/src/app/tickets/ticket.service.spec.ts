import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TicketService } from './ticket.service';

describe('TicketService', () => {
  let service: TicketService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TicketService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only the filters that are set', () => {
    service.list({ category: 'BILLING', priority: '', status: 'OPEN' }).subscribe();

    const request = http.expectOne((req) => req.url === '/api/tickets');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('category')).toBe('BILLING');
    expect(request.request.params.get('status')).toBe('OPEN');
    expect(request.request.params.has('priority')).toBe(false);
    request.flush([]);
  });

  it('updates a ticket with PATCH', () => {
    service.update(7, { status: 'RESOLVED' }).subscribe();

    const request = http.expectOne('/api/tickets/7');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'RESOLVED' });
    request.flush({});
  });

  it('creates a ticket with POST', () => {
    const body = { customerEmail: 'a@example.com', subject: 'Hi', body: 'Help' };
    service.create(body).subscribe((response) => expect(response.warning).toBeNull());

    const request = http.expectOne('/api/tickets');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ ticket: { id: 1 }, warning: null });
  });
});
