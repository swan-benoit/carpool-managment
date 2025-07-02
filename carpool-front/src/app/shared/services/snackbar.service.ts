import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { SnackbarData } from '../components/snackbar/snackbar.component';

@Injectable({
  providedIn: 'root'
})
export class SnackbarService {
  private snackbarSubject = new BehaviorSubject<{ data: SnackbarData | null; show: boolean }>({
    data: null,
    show: false
  });

  snackbar$ = this.snackbarSubject.asObservable();

  show(data: SnackbarData): void {
    // Fermer la snackbar précédente si elle existe
    this.hide();
    
    // Petit délai pour permettre l'animation de fermeture
    setTimeout(() => {
      this.snackbarSubject.next({
        data: { duration: 3000, ...data },
        show: true
      });

      // Auto-fermeture après la durée spécifiée
      const duration = data.duration || 3000;
      setTimeout(() => {
        this.hide();
      }, duration);
    }, 100);
  }

  hide(): void {
    this.snackbarSubject.next({
      data: this.snackbarSubject.value.data,
      show: false
    });
  }

  success(message: string, duration?: number): void {
    this.show({
      message,
      type: 'success',
      duration
    });
  }

  error(message: string, duration?: number): void {
    this.show({
      message,
      type: 'error',
      duration
    });
  }

  warning(message: string, duration?: number): void {
    this.show({
      message,
      type: 'warning',
      duration
    });
  }

  info(message: string, duration?: number): void {
    this.show({
      message,
      type: 'info',
      duration
    });
  }
}