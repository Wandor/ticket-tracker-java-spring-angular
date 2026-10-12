import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { NewTicket, Page, Stats, Status, Ticket } from './ticket.models';

export class ApiError extends Error {
  constructor(
    message: string,
    readonly fields: Record<string, string> = {},
  ) {
    super(message);
  }
}

/** Typed client for the Spring API, reached through the /api dev-server proxy. */
@Injectable({ providedIn: 'root' })
export class TicketApiService {
  private readonly http = inject(HttpClient);

  list(params: { status?: Status; page: number; limit: number }): Observable<Page<Ticket>> {
    let query = new HttpParams().set('page', params.page).set('limit', params.limit);
    if (params.status) query = query.set('status', params.status);
    return this.http.get<Page<Ticket>>('/api/tickets', { params: query }).pipe(catchError(toApiError));
  }

  create(ticket: NewTicket): Observable<Ticket> {
    return this.http.post<Ticket>('/api/tickets', ticket).pipe(catchError(toApiError));
  }

  updateStatus(id: number, status: Status): Observable<Ticket> {
    return this.http.patch<Ticket>(`/api/tickets/${id}`, { status }).pipe(catchError(toApiError));
  }

  stats(): Observable<Stats> {
    return this.http.get<Stats>('/api/tickets/stats').pipe(catchError(toApiError));
  }
}

function toApiError(err: HttpErrorResponse): Observable<never> {
  const body = err.error?.error;
  return throwError(() => new ApiError(body?.message ?? `Request failed (${err.status})`, body?.fields));
}
