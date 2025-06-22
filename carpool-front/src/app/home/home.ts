import {Component, inject} from '@angular/core';
import {ApiModule, FamilyResourceService} from '../modules/openapi';
import {AsyncPipe} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';

@Component({
  selector: 'app-home',
  imports: [
    AsyncPipe,
  ],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {
  familiesService = inject(FamilyResourceService)
  families = this.familiesService.familyGet()

  add() {
    this.familiesService.familyPost({
      name: "swan",
      carCapacity: 1000,
    }).subscribe()
  }
}
