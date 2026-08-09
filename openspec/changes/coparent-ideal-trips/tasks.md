## 1. Lecture du lien de foyer dans le workbook

- [x] 1.1 Ajouter une colonne optionnelle « Foyer » à l'onglet `Index` dans `WorkbookTemplateGenerator` (en-tête + cellules, sans casser les colonnes existantes)
- [x] 1.2 Lire la valeur « Foyer » par famille dans `WorkbookFamilyReader` et la porter jusqu'à `NormalizedWorkbookFamily` (champ `householdId` optionnel)
- [x] 1.3 Valider à la lecture : `family.name` distincts au sein d'un foyer ; avertir/dédupliquer si un même enfant apparaît deux fois dans un foyer
- [x] 1.4 Test lecteur : deux onglets même « Foyer » → un foyer à deux familles ; colonne vide → foyer solo

## 2. Regroupement par foyer

- [x] 2.1 Introduire un regroupement foyer au niveau normalisation/calcul (record `Household` ou map `householdId -> familles`), une famille sans clé = foyer solo
- [x] 2.2 Exposer, par foyer, la liste des familles-driver et l'ensemble dédupliqué des enfants du foyer

## 3. Cible d'équité au niveau foyer

- [x] 3.1 Dans `FamilyPlanningStats`, agréger `availableSlots`/demande de transport par foyer (enfants comptés une fois), sans double comptage
- [x] 3.2 Calculer la cible `perfectMeanTripPerWeek` du foyer indépendamment de la famille de rattachement de l'enfant
- [x] 3.3 Test : déplacer l'enfant d'un onglet co-parent à l'autre ne change pas la cible totale du foyer

## 4. Répartition de la cible entre co-parents

- [x] 4.1 Répartir la cible du foyer à parts égales entre ses familles-driver (`cible_foyer / nb_familles`), indépendamment de la disponibilité/capacité (design D3)
- [x] 4.2 Garantir Σ parts = cible foyer
- [x] 4.3 Garantir qu'un co-parent sans enfant rattaché reçoit une cible strictement positive (identique à l'autre co-parent)
- [x] 4.4 Tests : cible identique entre co-parents, somme = cible foyer, invariance du rattachement de l'enfant

## 5. Justice et scoring

- [x] 5.1 Dans `PlanningScorer`/`FamilyJusticeScore`, alimenter la `perfectMean` de chaque famille-driver par sa part de foyer (résolution nom→foyer), en conservant une ligne par famille dans les sorties
- [x] 5.2 Vérifier que `justice.minimumJusticeScore` consommé par `BruteForceSchedulePlanner.compareScores` n'effondre plus au seul fait qu'un co-parent conduit selon sa cible
- [x] 5.3 Test de bout en bout sur le cas Orion : Joséphine + Mickael reçoivent des cibles cohérentes et l'optimiseur répartit les trajets entre eux

## 6. Sorties et rétrocompatibilité

- [x] 6.1 Vérifier les colonnes « Idéal/sem » et « Justice » de `WorkbookStatsCli` (text) et de l'export xlsx avec la cible corrigée
- [x] 6.2 Test golden : workbook sans colonne « Foyer » → score/justice/cibles inchangés (aucune régression pour les familles solo)
- [x] 6.3 Mettre à jour le README du template (`WorkbookTemplateGenerator`) pour documenter la colonne « Foyer » et le cas parents séparés
