# PostgreSQL test setup for OS55

This setup is for local and automated tests only. Do not point it to a shared or production database.

## Start the local test database

```bash
docker-compose -f compose.test.yml up -d
```

The database is exposed only on localhost by default:

```text
127.0.0.1:55432
```

Default development values:

```text
Database: rloc_async_test
User:     rloc_test
Password: rloc_test_password
```

Override them with environment variables if needed:

```bash
export RLOC_TEST_DB_NAME=rloc_async_test
export RLOC_TEST_DB_USER=rloc_test
export RLOC_TEST_DB_PASSWORD=rloc_test_password
export RLOC_TEST_DB_PORT=55432
```

## Verify the database

```bash
docker-compose -f compose.test.yml exec postgres-test   psql -U rloc_test -d rloc_async_test -c '\dt'
```

Check OS55 business data:

```bash
docker-compose -f compose.test.yml exec postgres-test   psql -U rloc_test -d rloc_async_test -c 'select * from local; select * from surveillance order by id_local, id_fiche;'
```

## Technical tables

The async and Spring Batch technical tables are created by Flyway migrations under `src/main/resources/db/migration` when the Spring application/tests run.

The test-only business schema is available under:

```text
src/test/resources/db/test-migration
```

## Run automated tests

Automated PostgreSQL tests use Testcontainers:

```bash
mvn clean test
```

If Docker is not available, the Testcontainers class is skipped by JUnit.

## Run against the compose database manually

```bash
export RLOC_TEST_DB_URL=jdbc:postgresql://localhost:55432/rloc_async_test
export RLOC_TEST_DB_USERNAME=rloc_test
export RLOC_TEST_DB_PASSWORD=rloc_test_password
export RLOC_TEST_DB_DRIVER=org.postgresql.Driver
export RLOC_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/test-migration
mvn test -Dspring.profiles.active=test
```

## Reset the environment

```bash
docker-compose -f compose.test.yml down -v
```

Then start it again.

## Minimal test schema note

The real R-LOC business schema is not present in this repository. The `local` and `surveillance` tables are a minimal test schema documented for OS55 integration tests only. No format constraint is enforced on `id_local` or `id_fiche` because no validated rule exists in the codebase yet.
