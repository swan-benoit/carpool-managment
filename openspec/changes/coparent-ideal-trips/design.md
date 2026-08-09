## Context

Le domaine modélise chaque `Family` avec un `name`, une `carCapacity` unique, une liste de `children` et des préférences par créneau. Le planning et l'équité sont calculés par famille, indexés par `family.name` (`PlanningScorer`, `FamilyJusticeScore`).

`FamilyPlanningStats.perfectMeanTripPerWeek(family, families)` dérive la cible d'équité d'une famille de :
`totalRequiredTripsPerWeek * (familyAvailableSlots / totalAvailableSlots)`
où `availableSlots` d'une famille = somme des créneaux de présence de **ses enfants**.

Pour un parent séparé sans enfant rattaché, `familyAvailableSlots = 0` → cible = 0. Or `BruteForceSchedulePlanner.compareScores` classe les plans d'abord sur complétude, slots assignés, redundant drivers, puis `justice.minimumJusticeScore` (4e critère, avant les préférences). Un co-parent à cible 0 obtient sa meilleure justice en conduisant 0 trajet ; comme il est le minimum du plan, l'optimiseur déporte ses trajets sur les autres. Le bug corrompt donc l'objectif de planification, pas seulement l'affichage.

Contrainte forte : les familles non séparées doivent conserver un comportement identique (rétrocompat).

## Goals / Non-Goals

**Goals:**
- Introduire un regroupement « foyer » de plusieurs familles-driver co-parentes.
- Calculer la cible d'équité une fois par foyer (enfants du foyer, sans double comptage) et la répartir entre co-parents selon leur disponibilité/capacité de conduite.
- Alimenter le score de justice avec cette cible corrigée pour que l'optimiseur réparte réellement les trajets entre co-parents.
- Zéro régression pour les familles seules.

**Non-Goals:**
- Fusionner les deux parents en une seule `Family` (chacun garde sa capacité et ses préférences propres — c'est le cœur du besoin).
- Modéliser la garde alternée paire/impaire (la présence de l'enfant reste toutes semaines ; hors périmètre).
- Refondre la structure du workbook au-delà de l'ajout du lien de foyer.
- Corriger le bug d'export xlsx `#N` (onglets planning en collision) — sujet distinct.

## Decisions

### D1 — Déclarer le foyer via une colonne « Foyer » dans l'`Index`
Ajout d'une colonne optionnelle (ex: « Foyer » / group key) dans l'onglet `Index`, à côté de « Nom onglet ». Les familles partageant la même valeur non vide forment un foyer.

- **Pourquoi** : l'`Index` est déjà l'autorité de la liste des familles (`WorkbookFamilyReader` l'itère). Une colonne y est lue trivialement, rétrocompatible (vide = foyer solo), et visible d'un coup d'œil.
- **Alternative rejetée** : champ dans chaque onglet famille (ligne « Garde alternée / contexte » existe déjà mais est du texte libre non structuré) → plus dur à valider et à croiser entre onglets.
- **Alternative rejetée** : convention de nommage (préfixe commun) → fragile, implicite.

### D2 — Modéliser le foyer hors entité `Family` persistée
Introduire un regroupement au niveau normalisation/calcul (ex: clé `householdId` portée par `NormalizedWorkbookFamily`, ou record `Household` regroupant des `Family`), sans casser `Family` (ajout d'un champ nullable de clé de foyer au plus).

- **Pourquoi** : le calcul d'équité vit dans `FamilyPlanningStats`/`PlanningScorer` qui travaillent sur des listes de familles ; regrouper à ce niveau limite l'empreinte et évite une migration de schéma lourde.
- **Alternative rejetée** : entité JPA `Household` complète → surcoût non justifié tant que l'entrée est le workbook.

### D3 — Répartition de la cible : à parts égales entre co-parents
Cible du foyer divisée également entre ses familles-driver : part de i = `cible_foyer / nb_familles_du_foyer`.

- **Pourquoi** : décision métier du propriétaire du domaine. Deux parents séparés se partagent l'obligation de transport de leur enfant à 50/50 ; chacun doit fournir la même contribution « idéale », indépendamment de sa disponibilité ou de la taille de sa voiture. Une cible égale pousse aussi l'optimiseur à équilibrer les trajets réels entre les deux parents.
- **Alternative rejetée** : au prorata de la disponibilité (créneaux non-`IMPOSSIBLE`) → produisait des cibles inégales (ex: Joséphine 0.52 vs Mickael 1.57) jugées illogiques : deux co-parents doivent viser la même charge.
- **Alternative rejetée** : au prorata de la capacité → même problème d'inégalité entre parents.
- **Conséquence** : un parent peu disponible peut ne pas atteindre sa cible (déviation), mais la cible reste la référence de partage équitable attendue.

### D4 — Agrégation de la demande de foyer sans double comptage
`perfectMeanTripPerWeek` et `totalAvailableSlots` agrègent les enfants **par foyer**. Les enfants du foyer comptent une fois ; la cible du foyer est ensuite éclatée par co-parent via D3. Les familles solo = foyer d'une famille → formule inchangée.

- **Pourquoi** : garantit que déplacer l'enfant d'un onglet à l'autre du foyer ne change pas la cible (exigence spec) et préserve la rétrocompat.

### D5 — Justice indexée par famille mais cible issue du foyer
`PlanningScorer`/`FamilyJusticeScore` continuent de rapporter une ligne par famille (Joséphine, Mickael restent visibles séparément), mais la `perfectMean` de chaque ligne provient de la part de foyer (D3). L'indexation actuelle par `family.name` est conservée ; on ajoute la résolution nom→foyer.

- **Pourquoi** : conserve la lisibilité des sorties (une ligne par parent conducteur) tout en corrigeant la cible.

## Risks / Trade-offs

- **Choix de la pondération D3 mal calibré** → répartition contre-intuitive. → Rendre la formule simple et testée par scénarios (spec), itérer si besoin ; commencer sans pondération capacité si ambigu.
- **Régression sur familles solo** → cibles modifiées à tort. → Traiter explicitement « pas de clé de foyer = foyer solo » avec tests golden sur le workbook existant (score/justice inchangés hors Orion).
- **Collision de noms entre co-parents** (déjà rencontrée : deux onglets `B4=Orion`) → clés de calcul écrasées. → Exiger des `family.name` distincts ; la clé de foyer les relie sans les fusionner. Valider à la lecture.
- **Enfant dupliqué par erreur sur les deux onglets** → double comptage. → Dédupliquer les enfants au niveau foyer et/ou avertir si un même enfant apparaît deux fois dans un foyer.
- **Sorties xlsx** consommant `perfectMean` → doivent refléter la nouvelle valeur. → Couvrir `WorkbookStatsCli`/writer dans les tests.

## Migration Plan

1. Ajouter la colonne « Foyer » au template (`WorkbookTemplateGenerator`) et sa lecture (`WorkbookFamilyReader`), champ optionnel.
2. Regrouper par foyer dans la normalisation (D2).
3. Adapter `FamilyPlanningStats` (agrégation + répartition D3/D4) puis `PlanningScorer`/`FamilyJusticeScore` (D5).
4. Tests : golden sur workbook sans foyer (invariance), scénarios co-parents (Orion) validant cible non nulle + somme = cible foyer + meilleure répartition planifiée.
5. Rétrocompat : workbooks existants sans colonne « Foyer » → comportement identique.

Rollback : la fonctionnalité étant pilotée par une colonne optionnelle, un workbook sans clé de foyer revient au comportement d'origine ; revert code possible sans migration de données.

## Open Questions

- Forme de `f(capacité)` dans la pondération D3 : ignorer la capacité (dispo seule) pour la v1, ou linéaire ? Recommandation : démarrer **dispo seule**, ajouter capacité si un cas réel le motive.
- La répartition doit-elle borner un co-parent à sa disponibilité maximale réelle (ex: Joséphine ne peut dépasser 1/sem) pour éviter de lui allouer une cible inatteignable ? À trancher en implémentation.
- Un foyer peut-il contenir plus de deux familles-driver (familles recomposées) ? Le modèle D2/D3 le permet ; à confirmer comme cas supporté.
