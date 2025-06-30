import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Family, Child, Requirement, AbsenceDays, WeekDay, WeekType, TimeSlot } from '../../../../modules/openapi';
import { FamilyService } from '../../services/family.service';

@Component({
  selector: 'app-family-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './family-form.component.html',
  styleUrl: './family-form.component.css'
})
export class FamilyFormComponent implements OnInit {
  familyForm!: FormGroup;
  isEditMode = false;
  familyId?: number;
  isLoading = false;

  weekDays = [
    { value: WeekDay.Monday, label: 'Lundi' },
    { value: WeekDay.Tuesday, label: 'Mardi' },
    { value: WeekDay.Thursday, label: 'Jeudi' },
    { value: WeekDay.Friday, label: 'Vendredi' }
  ];

  weekTypes = [
    { value: WeekType.Even, label: 'Paire' },
    { value: WeekType.Odd, label: 'Impaire' }
  ];

  timeSlots = [
    { value: TimeSlot.Morning, label: 'Matin' },
    { value: TimeSlot.Evening, label: 'Soir' }
  ];

  constructor(
    private fb: FormBuilder,
    private familyService: FamilyService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.familyId = +id;
      this.loadFamily();
    }
  }

  initForm(): void {
    this.familyForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      carCapacity: [4, [Validators.required, Validators.min(1), Validators.max(20)]],
      children: this.fb.array([]),
      requirements: this.fb.array([])
    });
  }

  get children(): FormArray {
    return this.familyForm.get('children') as FormArray;
  }

  get requirements(): FormArray {
    return this.familyForm.get('requirements') as FormArray;
  }

  loadFamily(): void {
    if (!this.familyId) return;

    this.isLoading = true;
    this.familyService.getFamily(this.familyId).subscribe({
      next: (family) => {
        this.populateForm(family);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement:', error);
        this.isLoading = false;
        alert('Erreur lors du chargement de la famille');
      }
    });
  }

  populateForm(family: Family): void {
    this.familyForm.patchValue({
      name: family.name,
      carCapacity: family.carCapacity
    });

    // Populate children
    if (family.children) {
      family.children.forEach(child => {
        this.addChild(child);
      });
    }

    // Populate requirements
    if (family.requirements) {
      Array.from(family.requirements).forEach(requirement => {
        this.addRequirement(requirement);
      });
    }
  }

  createChildFormGroup(child?: Child): FormGroup {
    const childForm = this.fb.group({
      id: [child?.id || null],
      name: [child?.name || '', [Validators.required, Validators.minLength(2)]],
      absenceDays: this.fb.array([])
    });

    if (child?.absenceDays) {
      const absenceDaysArray = childForm.get('absenceDays') as FormArray;
      Array.from(child.absenceDays).forEach(absenceDay => {
        absenceDaysArray.push(this.createAbsenceDayFormGroup(absenceDay));
      });
    }

    return childForm;
  }

  createAbsenceDayFormGroup(absenceDay?: AbsenceDays): FormGroup {
    return this.fb.group({
      id: [absenceDay?.id || null],
      weekDay: [absenceDay?.weekDay || '', Validators.required],
      weekType: [absenceDay?.weekType || '', Validators.required]
    });
  }

  createRequirementFormGroup(requirement?: Requirement): FormGroup {
    return this.fb.group({
      id: [requirement?.id || null],
      timeSlot: [requirement?.timeSlot || '', Validators.required],
      weekDay: [requirement?.weekDay || '', Validators.required],
      weekType: [requirement?.weekType || '', Validators.required]
    });
  }

  addChild(child?: Child): void {
    this.children.push(this.createChildFormGroup(child));
  }

  removeChild(index: number): void {
    this.children.removeAt(index);
  }

  addRequirement(requirement?: Requirement): void {
    this.requirements.push(this.createRequirementFormGroup(requirement));
  }

  removeRequirement(index: number): void {
    this.requirements.removeAt(index);
  }

  getChildAbsenceDays(childIndex: number): FormArray {
    return this.children.at(childIndex).get('absenceDays') as FormArray;
  }

  addAbsenceDay(childIndex: number): void {
    const absenceDaysArray = this.getChildAbsenceDays(childIndex);
    absenceDaysArray.push(this.createAbsenceDayFormGroup());
  }

  removeAbsenceDay(childIndex: number, absenceIndex: number): void {
    const absenceDaysArray = this.getChildAbsenceDays(childIndex);
    absenceDaysArray.removeAt(absenceIndex);
  }

  onSubmit(): void {
    if (this.familyForm.valid) {
      this.isLoading = true;
      const formValue = this.familyForm.value;

      const family: Family = {
        id: this.familyId,
        name: formValue.name,
        carCapacity: formValue.carCapacity,
        children: formValue.children.map((child: any) => {
          console.log(child.absenceDays);
          return ({
            id: child.id,
            name: child.name,
            absenceDays: child.absenceDays
          });
        }),
        requirements: formValue.requirements
      };

      const operation = this.isEditMode
        ? this.familyService.updateFamily(this.familyId!, family)
        : this.familyService.createFamily(family);

      operation.subscribe({
        next: () => {
          this.isLoading = false;
          this.router.navigate(['/families']);
        },
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
          this.isLoading = false;
          alert('Erreur lors de la sauvegarde');
        }
      });
    } else {
      this.markFormGroupTouched(this.familyForm);
    }
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      } else if (control instanceof FormArray) {
        control.controls.forEach(arrayControl => {
          if (arrayControl instanceof FormGroup) {
            this.markFormGroupTouched(arrayControl);
          }
        });
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/families']);
  }
}
