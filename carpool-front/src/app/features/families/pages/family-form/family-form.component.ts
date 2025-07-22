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
  
  // Tous les créneaux possibles pour les indisponibilités
  allTimeSlots = [
    { weekDay: WeekDay.Monday, timeSlot: TimeSlot.Morning, weekType: WeekType.Even, label: 'Lundi Matin - Semaine Paire' },
    { weekDay: WeekDay.Monday, timeSlot: TimeSlot.Evening, weekType: WeekType.Even, label: 'Lundi Soir - Semaine Paire' },
    { weekDay: WeekDay.Tuesday, timeSlot: TimeSlot.Morning, weekType: WeekType.Even, label: 'Mardi Matin - Semaine Paire' },
    { weekDay: WeekDay.Tuesday, timeSlot: TimeSlot.Evening, weekType: WeekType.Even, label: 'Mardi Soir - Semaine Paire' },
    { weekDay: WeekDay.Thursday, timeSlot: TimeSlot.Morning, weekType: WeekType.Even, label: 'Jeudi Matin - Semaine Paire' },
    { weekDay: WeekDay.Thursday, timeSlot: TimeSlot.Evening, weekType: WeekType.Even, label: 'Jeudi Soir - Semaine Paire' },
    { weekDay: WeekDay.Friday, timeSlot: TimeSlot.Morning, weekType: WeekType.Even, label: 'Vendredi Matin - Semaine Paire' },
    { weekDay: WeekDay.Friday, timeSlot: TimeSlot.Evening, weekType: WeekType.Even, label: 'Vendredi Soir - Semaine Paire' },
    
    { weekDay: WeekDay.Monday, timeSlot: TimeSlot.Morning, weekType: WeekType.Odd, label: 'Lundi Matin - Semaine Impaire' },
    { weekDay: WeekDay.Monday, timeSlot: TimeSlot.Evening, weekType: WeekType.Odd, label: 'Lundi Soir - Semaine Impaire' },
    { weekDay: WeekDay.Tuesday, timeSlot: TimeSlot.Morning, weekType: WeekType.Odd, label: 'Mardi Matin - Semaine Impaire' },
    { weekDay: WeekDay.Tuesday, timeSlot: TimeSlot.Evening, weekType: WeekType.Odd, label: 'Mardi Soir - Semaine Impaire' },
    { weekDay: WeekDay.Thursday, timeSlot: TimeSlot.Morning, weekType: WeekType.Odd, label: 'Jeudi Matin - Semaine Impaire' },
    { weekDay: WeekDay.Thursday, timeSlot: TimeSlot.Evening, weekType: WeekType.Odd, label: 'Jeudi Soir - Semaine Impaire' },
    { weekDay: WeekDay.Friday, timeSlot: TimeSlot.Morning, weekType: WeekType.Odd, label: 'Vendredi Matin - Semaine Impaire' },
    { weekDay: WeekDay.Friday, timeSlot: TimeSlot.Evening, weekType: WeekType.Odd, label: 'Vendredi Soir - Semaine Impaire' }
  ];

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
      unavailabilitySlots: this.fb.group({})
    });
    
    // Initialiser les cases à cocher pour les indisponibilités
    this.initUnavailabilityCheckboxes();
  }

  get children(): FormArray {
    return this.familyForm.get('children') as FormArray;
  }

  get unavailabilitySlots(): FormGroup {
    return this.familyForm.get('unavailabilitySlots') as FormGroup;
  }
  
  initUnavailabilityCheckboxes(): void {
    const unavailabilityGroup = this.fb.group({});
    
    this.allTimeSlots.forEach(slot => {
      const key = this.getSlotKey(slot.weekDay, slot.timeSlot, slot.weekType);
      unavailabilityGroup.addControl(key, this.fb.control(false));
    });
    
    this.familyForm.setControl('unavailabilitySlots', unavailabilityGroup);
  }
  
  getSlotKey(weekDay: WeekDay, timeSlot: TimeSlot, weekType: WeekType): string {
    return `${weekDay}_${timeSlot}_${weekType}`;
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

    // Populate unavailabilities - convertir en cases à cocher
    if (family.requirements) {
      Array.from(family.requirements).forEach(requirement => {
        if (requirement.weekDay && requirement.timeSlot && requirement.weekType) {
          const key = this.getSlotKey(requirement.weekDay, requirement.timeSlot, requirement.weekType);
          this.unavailabilitySlots.get(key)?.setValue(true);
        }
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

  addChild(child?: Child): void {
    this.children.push(this.createChildFormGroup(child));
  }

  removeChild(index: number): void {
    this.children.removeAt(index);
  }

  createAbsenceDayFormGroup(absenceDay?: AbsenceDays): FormGroup {
    return this.fb.group({
      id: [absenceDay?.id || null],
      weekDay: [absenceDay?.weekDay || '', Validators.required],
      weekType: [absenceDay?.weekType || '', Validators.required]
    });
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

      // Convertir les cases à cocher en requirements
      const requirements: Requirement[] = [];
      Object.keys(formValue.unavailabilitySlots).forEach(key => {
        if (formValue.unavailabilitySlots[key]) {
          const [weekDay, timeSlot, weekType] = key.split('_');
          requirements.push({
            timeSlot: timeSlot as TimeSlot,
            weekDay: weekDay as WeekDay,
            weekType: weekType as WeekType
          });
        }
      });
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
        requirements: requirements
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
  
  // Méthodes utilitaires pour l'affichage
  getSlotsByWeekType(weekType: WeekType) {
    return this.allTimeSlots.filter(slot => slot.weekType === weekType);
  }
  
  getSlotsByDay(slots: any[], weekDay: WeekDay) {
    return slots.filter(slot => slot.weekDay === weekDay);
  }
  
  getWeekTypeLabel(weekType: WeekType): string {
    return weekType === WeekType.Even ? 'Semaines Paires' : 'Semaines Impaires';
  }
  
  getWeekDayLabel(weekDay: WeekDay): string {
    switch (weekDay) {
      case WeekDay.Monday: return 'Lundi';
      case WeekDay.Tuesday: return 'Mardi';
      case WeekDay.Thursday: return 'Jeudi';
      case WeekDay.Friday: return 'Vendredi';
      default: return '';
    }
  }
  
  getTimeSlotLabel(timeSlot: TimeSlot): string {
    return timeSlot === TimeSlot.Morning ? 'Matin' : 'Soir';
  }
}