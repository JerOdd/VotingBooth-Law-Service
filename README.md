# law-service
Law Service for VotingBooth

## Modules
- `core`: entities, repositories and Flyway migrations, shared by the other modules.
- `api`: REST API (in progress).
- `job`: nightly sync of the Assemblée nationale's "Dossiers législatifs" open data archive into Postgres, with the raw JSON in S3.

## Running the sync locally
```sh
docker compose up -d                      # Postgres on 5434, SeaweedFS (S3) on 9001 with a versioned law-raw bucket
./mvnw -pl job -am package -DskipTests
java -jar job/target/law-job-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --manual
```
- Runs once and exits by default (Kubernetes CronJob). `--force` reprocesses every record, ignoring stored hashes.
- `--law.sync.schedule.enabled=true` keeps it running and syncs on `law.sync.cron` (Paris time, 03:30 by default).
- Tests need Docker (Testcontainers Postgres for the end-to-end sync test).
