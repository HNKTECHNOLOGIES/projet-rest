# rloc-async

Projet Maven Java Spring Boot destine aux offres de services REST multivaleurs asynchrones de R-LOC.

Le premier OS cible est OS55 - Supprimer Surveillance. Cette initialisation pose uniquement le socle technique ; les controleurs REST et les traitements Spring Batch metier OS55 ne sont pas encore developpes.

## Stack technique

- Java 17
- Spring Boot 3.x
- Spring Batch 5.x
- Spring Web MVC
- Spring Data JPA
- Spring JDBC
- PostgreSQL
- Jackson
- Lombok
- Maven
- JUnit 5 et Mockito

## Package principal

```text
fr.gouv.finances.rloc.async
```

## Architecture des packages

```text
fr.gouv.finances.rloc.async
├── config
├── entity
├── repository
├── exception
├── rest
│   ├── common
│   │   ├── controller
│   │   ├── dto
│   │   └── service
│   └── os55
│       ├── controller
│       └── dto
├── batch
│   ├── common
│   └── os55
│       ├── reader
│       ├── processor
│       ├── writer
│       └── config
└── service
    └── os55
```

## Configuration PostgreSQL

La configuration est externalisee par variables d'environnement :

```bash
export RLOC_DB_URL=jdbc:postgresql://localhost:5432/rloc_async
export RLOC_DB_USERNAME=rloc
export RLOC_DB_PASSWORD=rloc
```

Le profil par defaut est `dev`, configurable via :

```bash
export SPRING_PROFILES_ACTIVE=dev
```

## Repertoires de stockage JSONL

Les repertoires prevus sont :

```text
storage/incoming
storage/processing
storage/outgoing
storage/archive
```

Le contenu runtime de ces repertoires est ignore par Git ; seuls les `.gitkeep` sont conserves.

## Commandes

Compiler et lancer les tests :

```bash
mvn test
```

Demarrer l'application en profil dev :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
