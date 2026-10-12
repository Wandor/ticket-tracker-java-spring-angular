import { Component, input } from '@angular/core';
import { STATUSES, STATUS_LABELS, Stats } from './ticket.models';

@Component({
  selector: 'app-stats-bar',
  template: `
    <div class="stats" aria-label="Tickets by status">
      @for (s of statuses; track s) {
        <span class="stat"><strong>{{ stats()?.[s] ?? '–' }}</strong>{{ labels[s] }}</span>
      }
    </div>
  `,
})
export class StatsBar {
  readonly stats = input<Stats | null>(null);
  protected readonly statuses = STATUSES;
  protected readonly labels = STATUS_LABELS;
}
