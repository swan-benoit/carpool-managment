/**
 * carpool-back API
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { WeekDay } from './week-day';
import { Car } from './car';
import { TimeSlot } from './time-slot';
import { WeekType } from './week-type';


export interface Trip { 
    id?: number;
    weekDay?: WeekDay;
    timeSlot?: TimeSlot;
    weekType?: WeekType;
    cars?: Array<Car>;
}
export namespace Trip {
}


