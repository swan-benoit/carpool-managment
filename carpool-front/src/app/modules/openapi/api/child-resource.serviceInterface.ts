/**
 * carpool-back API
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { HttpHeaders }                                       from '@angular/common/http';

import { Observable }                                        from 'rxjs';

import { Child } from '../model/models';


import { Configuration }                                     from '../configuration';



export interface ChildResourceServiceInterface {
    defaultHeaders: HttpHeaders;
    configuration: Configuration;

    /**
     * Count
     * 
     */
    childCountGet(extraHttpRequestParams?: any): Observable<number>;

    /**
     * List
     * 
     * @param familyId 
     * @param id 
     * @param name 
     * @param namedQuery 
     * @param page 
     * @param size 
     * @param sort 
     */
    childGet(familyId?: number, id?: number, name?: string, namedQuery?: string, page?: number, size?: number, sort?: Array<string>, extraHttpRequestParams?: any): Observable<Array<Child>>;

    /**
     * Delete
     * 
     * @param id 
     */
    childIdDelete(id: number, extraHttpRequestParams?: any): Observable<{}>;

    /**
     * Get
     * 
     * @param id 
     */
    childIdGet(id: number, extraHttpRequestParams?: any): Observable<Child>;

    /**
     * Update
     * 
     * @param id 
     * @param child 
     */
    childIdPut(id: number, child: Child, extraHttpRequestParams?: any): Observable<Child>;

    /**
     * Add
     * 
     * @param child 
     */
    childPost(child: Child, extraHttpRequestParams?: any): Observable<Child>;

}
