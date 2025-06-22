import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { FamilyResourceService, ChildResourceService, RequirementResourceService, Family, Child, Requirement, TimeSlot, WeekDay, WeekType } from '../../modules/openapi';

@Component({
  selector: 'app-family-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './family-form.html',
  styleUrl: './family-form.css'
})
export class FamilyForm implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private familyService = inject(FamilyResourceService);
  private childService = inject(ChildResourceService);
  private requirementService = inject(RequirementResourceService);

  familyForm: FormGroup;
  isEditMode = false;
  familyId: number | null = null;
  loading = false;
  error: string | null = null;

  timeSlots = [
    { value: TimeSlot.Morning, label: 'Matin' },
    { value: TimeSlot.Evening, label: 'Soir' }
  ];

  weekDays = [
    { value: WeekDay.Monday, label: 'Lundi' },
    { value: WeekDay.Tuesday, label: 'Mardi' },
    { value: WeekDay.Thursday, label: 'Jeudi' },
    { value: WeekDay.Friday, label: 'Vendredi' }
  ];

  weekTypes = [
    { value: WeekType.Even, label: 'Semaine paire' },
    { value: WeekType.Odd, label: 'Semaine impaire' }
  ];

  constructor() {
    this.familyForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      carCapacity: [4, [Validators.required, Validators.min(1), Validators.max(20)]],
      children: this.fb.array([]),
      requirements: this.fb.array([])
    });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.familyId = parseInt(id, 10);
      this.loadFamily();
    } else {
      this.addChild();
      this.addRequirement();
    }
  }

  get children(): FormArray {
    return this.familyForm.get('children') as FormArray;
  }

  get requirements(): FormArray {
    return this.familyForm.get('requirements') as FormArray;
  }

  loadFamily() {
    if (!this.familyId) return;

    this.loading = true;
    this.familyService.familyIdGet(this.familyId).subscribe({
      next: (family) => {
        this.familyForm.patchValue({
          name: family.name,
          carCapacity: family.carCapacity
        });

        // Charger les enfants
        this.childService.childGet(this.familyId!).subscribe({
          next: (children) => {
            children.forEach(child => {
              this.children.push(this.createChildFormGroup(child));
            });
            if (children.length === 0) {
              this.addChild();
            }
          }
        });

        // Charger les indisponibilités
        this.requirementService.requirementGet(this.familyId!).subscribe({
          next: (requirements) => {
            requirements.forEach(requirement => {
              this.requirements.push(this.createRequirementFormGroup(requirement));
            });
            if (requirements.length === 0) {
              this.addRequirement();
            }
            this.loading = false;
          }
        });
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la famille:', error);
        this.error = 'Erreur lors du chargement de la famille';
        this.loading = false;
      }
    });
  }

  createChildFormGroup(child?: Child): FormGroup {
    return this.fb.group({
      id: [child?.id || null],
      name: [child?.name || '', [Validators.required, Validators.minLength(2)]]
    });
  }

  createRequirementFormGroup(requirement?: Requirement): FormGroup {
    return this.fb.group({
      id: [requirement?.id || null],
      timeSlot: [requirement?.timeSlot || TimeSlot.Morning, Validators.required],
      weekDay: [requirement?.weekDay || WeekDay.Monday, Validators.required],
      weekType: [requirement?.weekType || WeekType.Even, Validators.required]
    });
  }

  addChild() {
    this.children.push(this.createChildFormGroup());
  }

  removeChild(index: number) {
    this.children.removeAt(index);
  }

  addRequirement() {
    this.requirements.push(this.createRequirementFormGroup());
  }

  removeRequirement(index: number) {
    this.requirements.removeAt(index);
  }

  onSubmit() {
    if (this.familyForm.valid) {
      this.loading = true;
      this.error = null;

      const formValue = this.familyForm.value;
      const family: Family = {
        name: formValue.name,
        carCapacity: formValue.carCapacity
      };

      if (this.isEditMode && this.familyId) {
        family.id = this.familyId;
        this.updateFamily(family, formValue.children, formValue.requirements);
      } else {
        this.createFamily(family, formValue.children, formValue.requirements);
      }
    } else {
      this.markFormGroupTouched(this.familyForm);
    }
  }

  createFamily(family: Family, children: any[], requirements: any[]) {
    this.familyService.familyPost(family).subscribe({
      next: (createdFamily) => {
        this.saveChildrenAndRequirements(createdFamily.id!, children, requirements);
      },
      error: (error) => {
        console.error('Erreur lors de la création de la famille:', error);
        this.error = 'Erreur lors de la création de la famille';
        this.loading = false;
      }
    });
  }

  updateFamily(family: Family, children: any[], requirements: any[]) {
    this.familyService.familyIdPut(family.id!, family).subscribe({
      next: (updatedFamily) => {
        this.saveChildrenAndRequirements(updatedFamily.id!, children, requirements);
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour de la famille:', error);
        this.error = 'Erreur lors de la mise à jour de la famille';
        this.loading = false;
      }
    });
  }

  saveChildrenAndRequirements(familyId: number, children: any[], requirements: any[]) {
    const childrenPromises = children
      .filter(child => child.name.trim())
      .map(child => {
        const childData: Child = {
          name: child.name,
          family: { id: familyId }
        };
        
        if (child.id) {
          return this.childService.childIdPut(child.id, { ...childData, id: child.id }).toPromise();
        } else {
          return this.childService.childPost(childData).toPromise();
        }
      });

    const requirementsPromises = requirements.map(requirement => {
      const requirementData: Requirement = {
        timeSlot: requirement.timeSlot,
        weekDay: requirement.weekDay,
        weekType: requirement.weekType,
        family: { id: familyId }
      };
      
      if (requirement.id) {
        return this.requirementService.requirementIdPut(requirement.id, { ...requirementData, id: requirement.id }).toPromise();
      } else {
        return this.requirementService.requirementPost(requirementData).toPromise();
      }
    });

    Promise.all([...childrenPromises, ...requirementsPromises])
      .then(() => {
        this.loading = false;
        this.router.navigate(['/families']);
      })
      .catch((error) => {
        console.error('Erreur lors de la sauvegarde:', error);
        this.error = 'Erreur lors de la sauvegarde des enfants et indisponibilités';
        this.loading = false;
      });
  }

  cancel() {
    this.router.navigate(['/families']);
  }

  private markFormGroupTouched(formGroup: FormGroup) {
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

  getFieldError(fieldName: string): string | null {
    const field = this.familyForm.get(fieldName);
    if (field && field.touched && field.errors) {
      if (field.errors['required']) return 'Ce champ est requis';
      if (field.errors['minlength']) return `Minimum ${field.errors['minlength'].requiredLength} caractères`;
      if (field.errors['min']) return `Valeur minimum: ${field.errors['min'].min}`;
      if (field.errors['max']) return `Valeur maximum: ${field.errors['max'].max}`;
    }
    return null;
  }

  getChildFieldError(index: number, fieldName: string): string | null {
    const field = this.children.at(index).get(fieldName);
    if (field && field.touched && field.errors) {
      if (field.errors['required']) return 'Ce champ est requis';
      if (field.errors['minlength']) return `Minimum ${field.errors['minlength'].requiredLength} caractères`;
    }
    return null;
  }
}