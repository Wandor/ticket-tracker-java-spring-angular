import { Component, inject, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiError, TicketApiService } from './ticket-api.service';
import { PRIORITIES, Priority } from './ticket.models';

@Component({
  selector: 'app-ticket-form',
  imports: [ReactiveFormsModule],
  template: `
    <form class="create" [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <label>
        Title
        <input formControlName="title" maxlength="200" [attr.aria-invalid]="!!fieldErrors()['title']" />
        @if (fieldErrors()['title']) {
          <span class="field-error">Title {{ fieldErrors()['title'] }}</span>
        }
      </label>
      <label>
        Priority
        <select formControlName="priority">
          @for (p of priorities; track p) {
            <option [value]="p">{{ p }}</option>
          }
        </select>
      </label>
      <label class="full">
        Description (optional)
        <textarea formControlName="description"></textarea>
      </label>
      <div class="full">
        <button type="submit" [disabled]="saving() || form.invalid">
          {{ saving() ? 'Creating…' : 'Create ticket' }}
        </button>
        @if (message()) {
          <div class="alert">{{ message() }}</div>
        }
      </div>
    </form>
  `,
})
export class TicketForm {
  private readonly api = inject(TicketApiService);

  readonly created = output<void>();

  protected readonly priorities = PRIORITIES;
  protected readonly saving = signal(false);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly message = signal<string | null>(null);

  // Client-side rules only gate the button; the API remains the source of truth.
  protected readonly form = inject(NonNullableFormBuilder).group({
    title: ['', [Validators.required, Validators.pattern(/\S/)]],
    description: [''],
    priority: ['medium' as Priority],
  });

  async submit(): Promise<void> {
    this.saving.set(true);
    this.fieldErrors.set({});
    this.message.set(null);
    try {
      await firstValueFrom(this.api.create(this.form.getRawValue()));
      this.form.reset();
      this.created.emit();
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fields).length > 0) {
        this.fieldErrors.set(err.fields);
      } else {
        this.message.set(err instanceof Error ? err.message : 'Could not create ticket');
      }
    } finally {
      this.saving.set(false);
    }
  }
}
