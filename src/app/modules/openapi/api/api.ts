export * from './example-resource.service';
import { ExampleResourceService } from './example-resource.service';
export * from './example-resource.serviceInterface';
export * from './family-resource.service';
import { FamilyResourceService } from './family-resource.service';
export * from './family-resource.serviceInterface';
export const APIS = [ExampleResourceService, FamilyResourceService];
