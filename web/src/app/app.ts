import { Component, effect, inject, signal } from '@angular/core';
import { firstValueFrom, forkJoin } from 'rxjs';
import { StatsBar } from './tickets/stats-bar';
import { TicketApiService } from './tickets/ticket-api.service';
import { TicketForm } from './tickets/ticket-form';
import { TicketTable } from './tickets/ticket-table';
import { Page, STATUSES, STATUS_LABELS, Stats, Status, Ticket } from './tickets/ticket.models';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-root',
  imports: [StatsBar, TicketForm, TicketTable],
  template: `
    <main class="page">
      <header class="page-header">
        <h1>Ticket Tracker</h1>
        <app-stats-bar [stats]="stats()" />
      </header>

      <section class="card">
        <h2>New ticket</h2>
        <app-ticket-form (created)="onCreated()" />
      </section>

      <section class="card">
        <div class="toolbar">
          <h2>Tickets</h2>
          <div class="filters" role="group" aria-label="Filter by status">
            <button [attr.aria-pressed]="filter() === undefined" (click)="selectFilter(undefined)">All</button>
            @for (s of statuses; track s) {
              <button [attr.aria-pressed]="filter() === s" (click)="selectFilter(s)">{{ labels[s] }}</button>
            }
          </div>
        </div>

        @if (error()) {
          <div class="alert">{{ error() }}</div>
        }
        @if (result(); as result) {
          <app-ticket-table
            [tickets]="result.data"
            [savingId]="savingId()"
            (statusChange)="changeStatus($event.id, $event.status)"
          />

          @if (result.pagination.total_pages > 1) {
            <nav class="pager" aria-label="Pagination">
              <button class="secondary" [disabled]="page() <= 1" (click)="page.set(page() - 1)">Previous</button>
              <span>
                Page {{ result.pagination.page }} of {{ result.pagination.total_pages }}
                ({{ result.pagination.total }} tickets)
              </span>
              <button
                class="secondary"
                [disabled]="page() >= result.pagination.total_pages"
                (click)="page.set(page() + 1)"
              >
                Next
              </button>
            </nav>
          }
        }
      </section>
    </main>
  `,
})
export class App {
  private readonly api = inject(TicketApiService);

  protected readonly statuses = STATUSES;
  protected readonly labels = STATUS_LABELS;

  protected readonly filter = signal<Status | undefined>(undefined);
  protected readonly page = signal(1);
  protected readonly result = signal<Page<Ticket> | null>(null);
  protected readonly stats = signal<Stats | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly savingId = signal<number | null>(null);

  constructor() {
    // Reload whenever the filter or page changes.
    effect(() => {
      void this.load(this.filter(), this.page());
    });
  }

  protected selectFilter(status: Status | undefined): void {
    this.filter.set(status);
    this.page.set(1);
  }

  protected onCreated(): void {
    // New tickets appear on page one, newest first.
    if (this.page() === 1) void this.load(this.filter(), 1);
    else this.page.set(1);
  }

  protected async changeStatus(id: number, status: Status): Promise<void> {
    this.savingId.set(id);
    try {
      await firstValueFrom(this.api.updateStatus(id, status));
      await this.load(this.filter(), this.page());
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Could not update ticket');
    } finally {
      this.savingId.set(null);
    }
  }

  private async load(status: Status | undefined, page: number): Promise<void> {
    try {
      const { list, counts } = await firstValueFrom(
        forkJoin({ list: this.api.list({ status, page, limit: PAGE_SIZE }), counts: this.api.stats() }),
      );
      // A status change can empty the last page of a filtered view; step back instead.
      if (list.data.length === 0 && page > 1) {
        this.page.set(page - 1);
        return;
      }
      this.result.set(list);
      this.stats.set(counts);
      this.error.set(null);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Could not load tickets');
    }
  }
}
