import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { STATUSES, STATUS_LABELS, Status, Ticket } from './ticket.models';

@Component({
  selector: 'app-ticket-table',
  imports: [DatePipe],
  template: `
    @if (tickets().length === 0) {
      <p class="empty">No tickets here yet.</p>
    } @else {
      <table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          @for (ticket of tickets(); track ticket.id) {
            <tr>
              <td class="title">
                {{ ticket.title }}
                @if (ticket.description) {
                  <small>{{ ticket.description }}</small>
                }
              </td>
              <td>
                <span class="badge priority-{{ ticket.priority }}">{{ ticket.priority }}</span>
              </td>
              <td>
                <select
                  class="status-{{ ticket.status }}"
                  [value]="ticket.status"
                  [disabled]="savingId() === ticket.id"
                  [attr.aria-label]="'Status of ' + ticket.title"
                  (change)="onChange(ticket.id, $event)"
                >
                  @for (s of statuses; track s) {
                    <option [value]="s" [selected]="s === ticket.status">{{ labels[s] }}</option>
                  }
                </select>
              </td>
              <td class="nowrap">{{ ticket.created_at | date: 'medium' }}</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class TicketTable {
  readonly tickets = input.required<Ticket[]>();
  readonly savingId = input<number | null>(null);
  readonly statusChange = output<{ id: number; status: Status }>();

  protected readonly statuses = STATUSES;
  protected readonly labels = STATUS_LABELS;

  protected onChange(id: number, event: Event): void {
    this.statusChange.emit({ id, status: (event.target as HTMLSelectElement).value as Status });
  }
}
