import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Family, FamilyResourceService } from '../../../modules/openapi';

@Injectable({
  providedIn: 'root'
})
export class FamilyService {
  constructor(private familyResourceService: FamilyResourceService) {}

  getFamilies(): Observable<Family[]> {
    return this.familyResourceService.familyGet(undefined, undefined, undefined, undefined, undefined, undefined, ['name']);
  }

  getFamily(id: number): Observable<Family> {
    return this.familyResourceService.familyIdGet(id);
  }

  createFamily(family: Family): Observable<Family> {
    return this.familyResourceService.familyPost(family);
  }

  updateFamily(id: number, family: Family): Observable<Family> {
    return this.familyResourceService.familyIdPut(id, family);
  }

  deleteFamily(id: number): Observable<any> {
    return this.familyResourceService.familyIdDelete(id);
  }
}