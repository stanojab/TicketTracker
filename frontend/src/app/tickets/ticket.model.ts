// TypeScript mirror of the backend DTOs (com.tickettriage.ticket.dto).

export const CATEGORIES = ['BILLING', 'TECHNICAL', 'ACCOUNT', 'FEEDBACK', 'OTHER'] as const;
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const;
export const STATUSES = ['OPEN', 'RESOLVED'] as const;

export type Category = (typeof CATEGORIES)[number];
export type Priority = (typeof PRIORITIES)[number];
export type TicketStatus = (typeof STATUSES)[number];
export type Sentiment = 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';

export interface Ticket {
  id: number;
  customerEmail: string;
  subject: string;
  body: string;
  category: Category;
  priority: Priority;
  sentiment: Sentiment;
  summary: string | null;
  suggestedReply: string | null;
  status: TicketStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTicketRequest {
  customerEmail: string;
  subject: string;
  body: string;
}

export interface CreateTicketResponse {
  ticket: Ticket;
  /** Set when the ticket was saved but AI triage failed. */
  warning: string | null;
}

/** PATCH body: only the fields that should change. */
export interface UpdateTicketRequest {
  category?: Category;
  priority?: Priority;
  suggestedReply?: string;
  status?: TicketStatus;
}

/** An empty string means "no filter". */
export interface TicketFilters {
  category: Category | '';
  priority: Priority | '';
  status: TicketStatus | '';
}

export interface Stats {
  byCategory: Record<Category, number>;
  byPriority: Record<Priority, number>;
}
