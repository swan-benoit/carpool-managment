import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class SnackbarService {
  private matSnackBar: MatSnackBar = inject(MatSnackBar);

  /**
   * Affiche une snackbar de succès (verte)
   * @param message - Le message à afficher
   * @param duration - Durée d'affichage en millisecondes (défaut: 3000ms)
   */
  success(message: string, duration: number = 3000): void {
    this.matSnackBar.open(message, '✅', {
      duration,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['snackbar-success']
    });
  }

  /**
   * Affiche une snackbar d'erreur (rouge)
   * @param message - Le message à afficher
   * @param duration - Durée d'affichage en millisecondes (défaut: 4000ms)
   */
  error(message: string, duration: number = 4000): void {
    this.matSnackBar.open(message, '❌', {
      duration,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['snackbar-error']
    });
  }

  /**
   * Affiche une snackbar d'avertissement (orange)
   * @param message - Le message à afficher
   * @param duration - Durée d'affichage en millisecondes (défaut: 3500ms)
   */
  warning(message: string, duration: number = 3500): void {
    this.matSnackBar.open(message, '⚠️', {
      duration,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['snackbar-warning']
    });
  }

  /**
   * Affiche une snackbar d'information (bleue)
   * @param message - Le message à afficher
   * @param duration - Durée d'affichage en millisecondes (défaut: 3000ms)
   */
  info(message: string, duration: number = 3000): void {
    this.matSnackBar.open(message, 'ℹ️', {
      duration,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['snackbar-info']
    });
  }

  /**
   * Ferme toutes les snackbars ouvertes
   */
  dismiss(): void {
    this.matSnackBar.dismiss();
  }
}