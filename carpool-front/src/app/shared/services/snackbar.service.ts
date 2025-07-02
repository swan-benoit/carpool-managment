import {inject, Injectable} from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { SnackbarData } from '../components/snackbar/snackbar.component';
import {MatSnackBar} from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class SnackbarService {
  matSnackBar: MatSnackBar = inject(MatSnackBar);

}
