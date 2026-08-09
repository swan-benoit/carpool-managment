## Why

Les parents séparés qui partagent la garde d'un enfant sont modélisés en deux familles-driver distinctes (ex: `Orion (Joséphine)` capacité 4 et `Orion (Mickael)` capacité 3), l'enfant n'étant rattaché qu'à une seule des deux. La cible d'équité (`perfectMeanTripPerWeek`, le « trajet idéal ») est calculée à partir des créneaux de présence des **enfants** d'une famille. Le parent sans enfant rattaché obtient donc une cible de **0**, alors qu'il doit conduire réellement.

Ce n'est pas cosmétique : cette cible alimente directement l'objectif de l'optimiseur. Dans `BruteForceSchedulePlanner.compareScores`, `justice.minimumJusticeScore` est le 4e critère de tri, **au-dessus des préférences**. Comme conduire fait diverger le parent de sa cible 0 (et donc chute son score de justice, qui est le minimum du plan), l'optimiseur est récompensé quand ce parent fait **0 trajet** et pénalisé à chaque trajet qu'on lui confie. Résultat : le co-parent est systématiquement sous-utilisé et les trajets sont déportés sur les autres familles.

## What Changes

- Introduire une notion explicite de **foyer co-parent** : deux familles-driver (ou plus) déclarées comme co-parents d'un même enfant/foyer.
- Le workbook permet de déclarer ce lien (colonne de foyer / groupe dans l'`Index`, ou champ dédié dans l'onglet famille).
- `FamilyPlanningStats.perfectMeanTripPerWeek` calcule la cible d'équité **au niveau du foyer** (une seule obligation de transport pour l'enfant partagé), puis la **répartit à parts égales entre les co-parents** (50/50), plutôt que selon la possession de l'enfant.
- Le scoring de justice (`PlanningScorer`, `FamilyJusticeScore`) reflète cette cible répartie : le co-parent sans enfant rattaché reçoit une cible non nulle cohérente avec ses trajets attendus.
- Rétrocompatibilité : une famille normale (non liée à un foyer co-parent) conserve exactement le calcul actuel.

## Capabilities

### New Capabilities
- `coparent-households`: déclaration d'un foyer regroupant plusieurs familles-driver co-parentes d'un même enfant, et calcul de la cible d'équité (« trajet idéal ») au niveau du foyer avec répartition entre co-parents selon leur disponibilité/capacité de conduite.

### Modified Capabilities
<!-- Aucun spec existant dans openspec/specs. -->

## Impact

- `carpool-back/src/main/java/com/carpool/schedule/FamilyPlanningStats.java` — `perfectMeanTripPerWeek`, `availableSlots`, agrégation par foyer.
- `carpool-back/src/main/java/com/carpool/schedule/PlanningScorer.java` — justice et perfect-mean indexés par foyer, clés actuellement par `family.name`.
- `carpool-back/src/main/java/com/carpool/schedule/FamilyJusticeScore.java` — cible/justice par famille tenant compte du foyer.
- `carpool-back/src/main/java/com/carpool/schedule/calculator/BruteForceSchedulePlanner.java` — objectif inchangé mais alimenté par une cible correcte (comportement de planification amélioré).
- `carpool-back/src/main/java/com/carpool/family/Family.java` — nouveau lien de foyer (id/clé de groupe) ou entité `Household`.
- Workbook : `WorkbookFamilyReader.java`, `NormalizedWorkbookFamily.java`, `WorkbookTemplateGenerator.java`, `WorkbookXlsxWriter.java` (lecture/écriture du lien de foyer).
- Sorties : colonnes « Idéal/sem » et « Justice » du CLI/xlsx (`WorkbookStatsCli`).
- Aucune rupture pour les familles non séparées (comportement identique).
