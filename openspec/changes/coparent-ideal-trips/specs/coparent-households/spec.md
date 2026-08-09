## ADDED Requirements

### Requirement: Déclaration d'un foyer co-parent

Le système SHALL permettre de déclarer que plusieurs familles-driver sont co-parents d'un même foyer partageant la garde d'un ou plusieurs enfants. Le lien de foyer SHALL être exprimable dans le workbook (identifiant/groupe de foyer déclaré dans l'`Index` ou dans l'onglet famille) et lu par la normalisation. Une famille sans lien de foyer SHALL être traitée comme son propre foyer d'une seule famille.

#### Scenario: Deux familles déclarées dans le même foyer
- **WHEN** deux onglets famille (ex: `Orion (Joséphine)` et `Orion (Mickael)`) déclarent le même identifiant de foyer
- **THEN** la normalisation les regroupe dans un seul foyer contenant les deux familles-driver et l'ensemble de leurs enfants

#### Scenario: Famille sans lien de foyer
- **WHEN** une famille ne déclare aucun identifiant de foyer
- **THEN** elle constitue un foyer d'une seule famille et son calcul d'équité reste identique au comportement actuel

#### Scenario: Enfant rattaché à une seule des familles du foyer
- **WHEN** un enfant partagé n'est saisi que sur l'un des onglets co-parents du foyer
- **THEN** cet enfant est comptabilisé une seule fois au niveau du foyer, sans duplication

### Requirement: Cible d'équité calculée au niveau du foyer

Le système SHALL calculer l'obligation de transport (« trajet idéal » / `perfectMeanTripPerWeek`) une seule fois par foyer à partir des créneaux de présence des enfants du foyer, sans double comptage. La cible du foyer SHALL être dérivée des enfants du foyer indépendamment de la famille-driver à laquelle chaque enfant est rattaché.

#### Scenario: Cible du foyer indépendante du rattachement de l'enfant
- **WHEN** l'enfant partagé est rattaché à la famille A plutôt qu'à la famille B du même foyer
- **THEN** la cible d'équité totale du foyer est identique dans les deux cas

#### Scenario: Pas de double comptage des créneaux
- **WHEN** le foyer contient un enfant présent sur N créneaux
- **THEN** la contribution du foyer à la demande totale de transport correspond à ces N créneaux comptés une seule fois

### Requirement: Répartition de la cible entre co-parents

Le système SHALL répartir la cible d'équité du foyer à parts égales entre ses familles-driver, indépendamment de la possession de l'enfant, de la disponibilité ou de la capacité voiture de chaque parent. Chaque co-parent d'un même foyer SHALL recevoir la même cible d'équité (« Idéal/sem »).

#### Scenario: Co-parent sans enfant rattaché reçoit une cible non nulle
- **WHEN** une famille-driver co-parente n'a aucun enfant rattaché mais appartient à un foyer ayant une obligation de transport non nulle
- **THEN** sa cible d'équité (« Idéal/sem ») est strictement positive

#### Scenario: Cible identique entre co-parents
- **WHEN** deux familles-driver appartiennent au même foyer
- **THEN** elles reçoivent exactement la même cible d'équité, quelles que soient leurs disponibilités ou capacités respectives

#### Scenario: Somme des parts égale la cible du foyer
- **WHEN** la cible du foyer est répartie entre ses familles-driver
- **THEN** la somme des cibles individuelles des co-parents égale la cible totale du foyer

### Requirement: Objectif de planification alimenté par la cible corrigée

Le système SHALL utiliser la cible d'équité corrigée dans le calcul du score de justice consommé par l'optimiseur, de sorte que confier des trajets à un co-parent disponible n'entraîne plus une pénalité de justice injustifiée.

#### Scenario: Le co-parent disponible n'est plus systématiquement à zéro trajet
- **WHEN** un foyer co-parent avec deux parents disponibles est planifié
- **THEN** l'optimiseur peut répartir les trajets entre les co-parents sans que la justice minimale du plan chute du seul fait qu'un co-parent conduit conformément à sa cible

#### Scenario: Justice cohérente pour un co-parent conforme à sa cible
- **WHEN** un co-parent effectue un nombre de trajets proche de sa cible répartie
- **THEN** son score de justice est élevé (faible déviation), au lieu de s'effondrer parce que sa cible valait 0
