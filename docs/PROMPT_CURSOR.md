# Prompt à coller dans Cursor

Tu es un architecte et développeur senior Java / Spring Boot / Spring Batch travaillant sur R-LOC.

Implémente l'OS55 « Supprimer Surveillance » en mode REST asynchrone multivalué, en suivant **strictement** `docs/use-cases/OS55_USE_CASES.md` (copie du README fourni) et la fiche OS55 v1.4 du 20/05/2025.

**D'abord, inspecte le dépôt** : versions Java/Spring, OS55 synchrone, conventions REST, DTO `RequeteDTO` et `ErreurDTO`, entités, repositories, configuration Batch, schéma SQL, mécanismes de sécurité et de fichiers. Ne réinvente pas les règles ni les types.

**Livrable initial, sans modification de code** :
1. Tableau des classes existantes réutilisables et des classes à créer.
2. Diagramme de flux REST -> stockage -> scheduler -> Batch -> résultat -> ack.
3. Modèle de données et transitions des statuts existants.
4. Stratégie de transaction et de reprise garantissant l'absence de double suppression et de résultats dupliqués.
5. Liste des questions bloquantes et hypothèses explicites, sans les masquer.
6. Plan de réalisation phase par phase avec tests et critères d'acceptation.

Contraintes non négociables : `num` et jamais `elementId` ; un résultat par élément ; codes OS55 5114/5173/1232/1330/6001/6023 ; `OK` avec `idSurveillance`, `KO` avec motif ; JSONL en streaming ; ne pas exposer les chemins de fichiers ; réutiliser les DTO communs sans changement incompatible ; pas de threadpool métier manuel.

**Attends ma validation de l'analyse avant de générer le code.**
