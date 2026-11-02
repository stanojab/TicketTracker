import { Routes } from '@angular/router';

/**
 * Each page is lazy-loaded: its code (and the Material components it uses) is downloaded
 * only when the user first navigates to it, which keeps the initial bundle small.
 */
export const routes: Routes = [
  { path: '', redirectTo: 'tickets', pathMatch: 'full' },
  {
    path: 'tickets',
    title: 'Tickets · TicketTriage',
    loadComponent: () => import('./tickets/ticket-list/ticket-list').then((m) => m.TicketList),
  },
  {
    // 'new' must come before ':id', otherwise "new" would be treated as an id.
    path: 'tickets/new',
    title: 'New ticket · TicketTriage',
    loadComponent: () => import('./tickets/ticket-form/ticket-form').then((m) => m.TicketForm),
  },
  {
    path: 'tickets/:id',
    title: 'Ticket · TicketTriage',
    loadComponent: () => import('./tickets/ticket-detail/ticket-detail').then((m) => m.TicketDetail),
  },
  { path: '**', redirectTo: 'tickets' },
];
