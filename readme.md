# OS55 — Use Cases pour Cursor

## Contexte
Implémenter OS55 « Supprimer Surveillance » (R-LOC, spécification fonctionnelle version 1.4 du 20/05/2025) en REST asynchrone multivalué et Spring Batch. La version synchrone monovaluée peut exister : rechercher et réutiliser ses règles et services métier.

## Sources de vérité
1. La fiche fonctionnelle OS55 version 1.4 fournie par l'équipe (joindre le PDF au contexte Cursor).
2. Les classes et schémas réellement présents dans le dépôt.
3. Les décisions d'architecture ci-dessous. Ne pas inventer de formats, colonnes, codes d'erreur, versions de Spring ni signatures.

## Contrats existants
- `fr.gouv.finances.rloc.async.rest.common.dto.RequeteDTO` : `id: Long`, `ieis: String`, `status: String` (`EN_ATTENTE`, `EN_COURS`, `TERMINE`, `ERREUR`, `ACQUITTE`), `codeOS: int`, `cheminFichierEntree`, `cheminFichierSortie`, `dateCreation`, `dateMiseAJour`, `dateDebut`, `dateFin` (`org.joda.time.LocalDateTime`), `listeErreur: List<ErreurDTO>`.
- `ErreurDTO` : `code: Integer`, `descriptif: String`, `id: String` (commentaire existant : base64), `libelle: String`, `message: String`, `severite: Integer`, `timestamp: Instant`.
- Ne pas modifier les DTO existants sans analyse de compatibilité. `RequeteDTO.id` ne doit pas être présumé égal à l'UUID fonctionnel. Examiner où l'UUID est stocké et, si absent, proposer un champ ou DTO dédié avant toute modification.
- Dans les éléments métier, le champ d'identification est **`num`**, jamais `elementId`.

## Architecture cible
Client -> API REST -> métadonnées BDD + fichier d'entrée -> ordonnanceur -> Spring Batch (reader/processor/writer) -> suppression BDD + fichier résultat -> polling/téléchargement -> acquittement -> purge.
- Entrées et sorties JSONL, une ligne par élément ; upload et download en streaming.
- Métadonnées de demande en BDD, contenu des éléments en fichiers, pas de stockage intégral des éléments en BDD.
- Séparer erreur de soumission globale, erreur technique de job, et rejet métier par élément.
- Ne jamais exposer les chemins physiques de fichiers à un client externe.
- Une demande n'est éligible au batch qu'après publication complète et vérifiable de son fichier d'entrée.
- Ne pas lancer automatiquement le job dans le thread HTTP.

## API cible (à adapter aux conventions existantes)
- `POST /api/os55` : reçoit JSONL et métadonnées d'en-tête (UUID de demande, client/IEIS, nombre d'éléments). Retour 202 après persistance durable, 400/401/403/409/413 selon le cas.
- `GET /api/os55/{uuid}/status` : statut et progression.
- `GET /api/os55/{uuid}` : fichier de résultats en streaming lorsque disponible.
- `POST /api/os55/{uuid}/ack` : acquittement idempotent.
Les noms exacts des endpoints et des headers doivent être validés par rapport aux conventions du projet et à l'APIM.

## Cas d'utilisation communs

### UC-C01 — Soumettre une demande
**Acteur :** client autorisé. **Précondition :** UUID, identité client et flux transmis.
**Nominal :** authentifier/autoriser, contrôler métadonnées techniques, réserver l'UUID, streamer le corps vers un fichier temporaire, finaliser le fichier, publier la demande `EN_ATTENTE`, retourner 202 avec identifiant de suivi.
**Exceptions :** champs techniques manquants (400), refus d'accès (401/403), UUID existant (409), taille excessive (413), stockage indisponible (5xx sans demande prête).
**Acceptation :** aucun contrôle métier OS55 dans le contrôleur ; aucun chargement intégral en mémoire ; une demande 202 est retrouvable et traitable.

### UC-C02 — Anti-rejeu et validation des métadonnées
**Nominal :** contrainte unique en BDD sur UUID, vérification identité client, type de contenu, quotas de taille/nombre d'éléments, métadonnées requises. La contrainte unique tranche les courses concurrentes.
**Acceptation :** deux POST concurrents avec même UUID ne créent pas deux demandes ; l'autre reçoit 409 ; un client ne consulte jamais les données d'un autre.
**À décider :** portée de l'unicité UUID, comportement si upload interrompu, valeur et fiabilité de l'en-tête nombre d'éléments.

### UC-C03 — Stocker le fichier d'entrée
**Nominal :** écriture binaire/streaming vers `.part`, limites en octets, fermeture et synchronisation selon infrastructure, publication atomique dans le répertoire prêt, métadonnées persistées.
**Exceptions :** coupure réseau, disque plein, fichier tronqué, erreur entre commit BDD et renommage ; procédure de réconciliation documentée.
**Acceptation :** aucun batch ne lit un `.part` ; nettoyage des orphelins ; pas de désérialisation complète côté REST.

### UC-C04 — Ordonnancer les demandes
**Nominal :** détecter `EN_ATTENTE`, prendre une demande de façon atomique, mettre `EN_COURS`, lancer Spring Batch avec UUID comme paramètre, borner les jobs simultanés.
**Acceptation :** deux nœuds ne prennent pas la même demande ; reprise après crash ; pas de threadpool métier manuel ; limites de concurrence configurables.

### UC-C05 — Générer et publier les résultats
**Nominal :** produire un résultat par élément d'entrée dans un JSONL temporaire, avec `num` identique, code `OK`/`KO` et données conformes ; publier le fichier final avant le statut `TERMINE`.
**Acceptation :** nombre de lignes de sortie = nombre d'éléments d'entrée acceptés ; pas de résultats dupliqués après reprise ; les erreurs métier par élément ne transforment pas automatiquement la demande en `ERREUR`.
**Attention :** transaction BDD et fichier ne sont pas atomiques ensemble ; concevoir un mécanisme de reprise/idempotence (par exemple journal durable par élément ou sortie reconstruisible) avant d'implémenter.

### UC-C06 — Consulter le statut
**Nominal :** GET status, vérifier l'autorisation, renvoyer `RequeteDTO` ou DTO public compatible ; statuts `EN_ATTENTE`, `EN_COURS`, `TERMINE`, `ERREUR`, `ACQUITTE` ; progression fondée sur les éléments validés.
**Acceptation :** pas de chemin physique dans la réponse publique ; progression cohérente ; demandes inconnues ou non autorisées traitées sans fuite d'information.
**À décider :** exposer progression via DTO dédié si `RequeteDTO` ne le permet pas.

### UC-C07 — Télécharger les résultats
**Nominal :** GET résultat lorsque `TERMINE`, lecture en streaming du fichier de sortie ; préserver le format unitaire du service et `num`.
**Acceptation :** fichier volumineux non chargé en mémoire ; statut non terminé traité explicitement ; vérification de propriété de la demande ; erreur de fichier absent gérée.
**À décider :** JSONL vs tableau JSON, compression, découpage et pagination.

### UC-C08 — Acquitter la réception
**Nominal :** POST ack après téléchargement, marquer `ACQUITTE`, déclencher nettoyage différé et idempotent.
**Acceptation :** appels ack répétés sûrs ; pas de suppression avant validation de l'état ; fichiers et métadonnées nettoyés de façon cohérente.

### UC-C09 — Purger
**Nominal :** job périodique selon durée de rétention configurable, nettoyage des demandes expirées, fichiers temporaires/orphelins, métadonnées associées.
**Acceptation :** jamais de suppression de fichier d'un traitement actif ; logs et compteurs de purge ; échec de purge réessayable.

### UC-C10 — Reprendre les erreurs techniques
**Nominal :** distinguer rejets métier (KO élément) et erreurs techniques (retry/skip/restart selon type), exploiter les métadonnées Spring Batch, reprendre après crash.
**Acceptation :** aucune double suppression, aucune perte ou duplication de résultat, statut `ERREUR` sur échec technique terminal, diagnostics exploitables.

### UC-C11 — Réguler le débit multiclient
**Nominal :** configurer limite globale de jobs, limite par IEIS/client et politique équitable (round robin pondéré si validé), préserver la charge synchrone.
**Acceptation :** un client ne monopolise pas les workers ; limites observables et testables ; paramètres configurables.

## Cas d'utilisation métier OS55

### UC-55-01 — Lire un élément
**Entrée :** `{ "num": 1, "idLocal": "...", "idFiche": "..." }`.
**Nominal :** lecture JSONL ligne par ligne ; préserver `num` et la correspondance entrée/résultat.
**Acceptation :** un résultat par ligne soumise, y compris en cas de rejet métier. Définir le traitement des lignes JSON malformées et des `num` absents/doublons avant codage ; la fiche OS55 ne les spécifie pas.

### UC-55-02 — Contrôler la présence
**Règles :** `idLocal` absent -> code erreur 50, motif 5114 ; `idFiche` absent -> code erreur 50, motif 5173.
**Acceptation :** retourner `KO` avec identifiants disponibles et `listeErreur` selon les conventions `ErreurDTO` ; aucune suppression.

### UC-55-03 — Contrôler les formats
**Règles :** format `idLocal` incorrect -> code 10, motif 1232 ; format `idFiche` incorrect -> code 10, motif 1330.
**Acceptation :** réutiliser les validateurs synchrones ; aucune regex ou longueur inventée ; KO sans suppression.

### UC-55-04 — Vérifier l'existence du local
**Règle :** local inexistant dans R-LOC -> code 60, motif 6001.
**Acceptation :** KO sans suppression ; réutiliser le repository/service existant.

### UC-55-05 — Vérifier l'existence de la surveillance
**Règle :** absence de données pour le couple `idLocal/idFiche` -> code 60, motif 6023.
**Acceptation :** KO sans suppression ; ne pas utiliser le motif 6020 barré dans la version 1.4 sans confirmation.

### UC-55-06 — Supprimer et restituer
**Nominal :** retrouver l'occurrence de surveillance correspondant exactement au couple, mémoriser `idSurveillance`, supprimer cette occurrence de manière transactionnelle, produire `OK` + `idSurveillance` + `num`.
**Exceptions :** rejet `KO` avec `num`, `idLocal`, `idFiche`, code erreur, code motif, descriptif/libellé selon la spécification et la convention `ErreurDTO`.
**Acceptation :** ne jamais supprimer d'autres couples ; un résultat par élément ; définir la sémantique des doublons du même couple dans une demande et des suppressions concurrentes avant codage ; reprise idempotente.

## Codes OS55 de la fiche v1.4
| Code erreur | Code motif | Motif |
|---|---|---|
| 50 | 5114 | idLocal absent |
| 50 | 5173 | idFiche absent |
| 10 | 1232 | format idLocal incorrect |
| 10 | 1330 | format idFiche incorrect |
| 60 | 6001 | local inexistant |
| 60 | 6023 | couple sans données de surveillance |

## Tests prioritaires
- Soumission valide, anti-rejeu concurrent, fichier interrompu, dépassement de quota.
- Présence et format de chaque identifiant, local absent, surveillance absente, suppression OK, deux couples distincts.
- Mélange OK/KO dans une demande, cardinalité et correspondance des `num`.
- Reprise après commit suppression avant finalisation du fichier résultat.
- Deux workers sur la même demande ; deux demandes concurrentes ciblant le même couple.
- Autorisation par client/IEIS ; téléchargement streaming ; ack répété ; purge sans toucher aux jobs actifs.

## Questions à résoudre avant génération complète
1. Versions Java/Spring Boot/Spring Batch du dépôt.
2. Format et type exacts de `idLocal`, `idFiche`, `idSurveillance`, et mapping table SURVEILLANCE.
3. Service OS55 synchrone existant et nomenclature exhaustive des erreurs (`R-Loc codes erreurs.ods`).
4. Identifiant UUID : colonne, portée et DTO public ; sens exact de `ieis`.
5. Format de sortie public : JSONL ou tableau JSON ; champs obligatoires.
6. Règles `num` manquant/dupliqué et lignes JSONL invalides.
7. Taille maximale, nombre maximal, polling, quotas, délais de conservation.
8. Idempotence des suppressions et transactions de sortie.

## Ordre de réalisation
1. Audit du dépôt et rapport des écarts/points ouverts, sans modifier le code.
2. UC-C01 à UC-C03 (REST + persistance + stockage).
3. UC-C04 et UC-55-01 à UC-55-06 (Batch + métier).
4. UC-C05 à UC-C07 (résultats, statut, téléchargement).
5. UC-C08 à UC-C11 (ack, purge, reprise, régulation).
6. Tests intégration bout-en-bout et documentation OpenAPI.

À chaque phase : proposer fichiers/classes, écrire code et tests, expliquer les choix, exécuter les tests possibles, attendre validation avant la phase suivante.
