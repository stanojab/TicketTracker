import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateTicketRequest,
  CreateTicketResponse,
  Stats,
  Ticket,
  TicketFilters,
  UpdateTicketRequest,
} from './ticket.model';

/** All HTTP calls to the backend. Relative URLs: the dev proxy / nginx forwards /api to Spring Boot. */
@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);

  list(filters: Partial<TicketFilters> = {}): Observable<Ticket[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value) {
        params = params.set(key, value);
      }
    }
    return this.http.get<Ticket[]>('/api/tickets', { params });
  }

  get(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`/api/tickets/${id}`);
  }

  create(request: CreateTicketRequest): Observable<CreateTicketResponse> {
    return this.http.post<CreateTicketResponse>('/api/tickets', request);
  }

  update(id: number, changes: UpdateTicketRequest): Observable<Ticket> {
    return this.http.patch<Ticket>(`/api/tickets/${id}`, changes);
  }

  stats(): Observable<Stats> {
    return this.http.get<Stats>('/api/stats');
  }
}
