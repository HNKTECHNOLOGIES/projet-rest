# Architecture technique initiale rloc-async

## Objectif de cette phase

Initialiser un socle Maven Spring Boot pour une architecture REST asynchrone multivaluee R-LOC, sans developper les controleurs REST ni les traitements Spring Batch metier OS55.

## Principes retenus depuis les documents

- Les demandes multivaluees utiliseront JSONL.
- Chaque element metier utilisera `num` comme identifiant dans la demande.
- Le socle commun doit etre reutilisable par plusieurs offres de service.
- OS55 est isole dans ses packages dedies.
- Aucun threadpool metier manuel n'est cree dans le squelette.
- Les regles metier OS55 ne sont pas inventees dans cette phase.

## Packages

| Package | Responsabilite initiale |
|---|---|
| `config` | Configuration Spring, proprietes externalisees, profils. |
| `entity` | Futures entites JPA. |
| `repository` | Futurs repositories JPA/JDBC. |
| `exception` | Futures exceptions techniques et fonctionnelles communes. |
| `rest.common.controller` | Futurs endpoints transverses mutualisables. |
| `rest.common.dto` | DTO communs du socle asynchrone. |
| `rest.common.service` | Services communs de soumission, statut, stockage et acquittement. |
| `rest.os55.controller` | Futurs endpoints REST propres a OS55. |
| `rest.os55.dto` | Futurs DTO d'entree/sortie OS55. |
| `batch.common` | Futurs composants Spring Batch transverses. |
| `batch.os55.reader` | Futurs readers JSONL OS55. |
| `batch.os55.processor` | Futurs processors metier OS55. |
| `batch.os55.writer` | Futurs writers JSONL OS55. |
| `batch.os55.config` | Future configuration de job/steps OS55. |
| `service.os55` | Futurs services metier partages par REST synchrone/asynchrone et Batch. |

## Flux cible

```text
Client
  -> REST OSxx
  -> stockage JSONL entrant dans storage/incoming
  -> deplacement vers storage/processing pendant traitement
  -> Spring Batch OSxx
  -> publication JSONL resultat dans storage/outgoing
  -> archivage dans storage/archive selon retention
```

## Configuration

La configuration PostgreSQL est externalisee par variables d'environnement :

- `RLOC_DB_URL`
- `RLOC_DB_USERNAME`
- `RLOC_DB_PASSWORD`

Les repertoires JSONL sont egalement externalises :

- `RLOC_STORAGE_INCOMING_DIR`
- `RLOC_STORAGE_PROCESSING_DIR`
- `RLOC_STORAGE_OUTGOING_DIR`
- `RLOC_STORAGE_ARCHIVE_DIR`

## Points non developpes dans cette phase

- Controleurs REST.
- Services metier de suppression de surveillance.
- Reader/processor/writer Batch OS55.
- Entites et migrations SQL definitives.
- Strategie complete de reprise/idempotence.
