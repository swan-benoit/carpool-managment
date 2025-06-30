import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { FullSchedule } from '../../../../modules/openapi';
import { ScheduleService } from '../../services/schedule.service';

@Component({
  selector: 'app-schedule-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './schedule-form.component.html',
  styleUrl: './schedule-form.component.css'
})
export class ScheduleFormComponent implements OnInit {
  scheduleForm!: FormGroup;
  isEditMode = false;
  scheduleId?: number;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private scheduleService: ScheduleService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.scheduleId = +id;
      this.loadSchedule();
    }
  }

  initForm(): void {
    this.scheduleForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  loadSchedule(): void {
    if (!this.scheduleId) return;

    this.isLoading = true;
    this.scheduleService.getSchedule(this.scheduleId).subscribe({
      next: (schedule) => {
        this.populateForm(schedule);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement:', error);
        this.isLoading = false;
        alert('Erreur lors du chargement du planning');
      }
    });
  }

  populateForm(schedule: FullSchedule): void {
    this.scheduleForm.patchValue({
      name: schedule.name
    });
  }

  onSubmit(): void {
    if (this.scheduleForm.valid) {
      this.isLoading = true;
      const formValue = this.scheduleForm.value;

      const schedule: FullSchedule = {
        id: this.scheduleId,
        name: formValue.name,
        evenSchedule: {
          trips: new Set()
        },
        oddSchedule: {
          trips: new Set()
        }
      };

      const operation = this.isEditMode
        ? this.scheduleService.updateSchedule(this.scheduleId!, schedule)
        : this.scheduleService.createSchedule(schedule);

      operation.subscribe({
        next: () => {
          this.isLoading = false;
          this.router.navigate(['/schedules']);
        },
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
          this.isLoading = false;
          alert('Erreur lors de la sauvegarde');
        }
      });
    } else {
      this.markFormGroupTouched(this.scheduleForm);
    }
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  cancel(): void {
    this.router.navigate(['/schedules']);
  }
}