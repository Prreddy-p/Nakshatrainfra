# Preserve existing application data

Property photos and documents are stored in the `property_attachments` database
table, including their file bytes. They share the database's backup and persistence
requirements; no separate upload directory is used. Deleting an attachment is an
explicit action. Deleting its property also deletes that property's attachments.

Application records live in the database, independently of browser refreshes.
The default database is `data/real-estate-crm.mv.db`. Hibernate uses `update`,
not `create` or `create-drop`. Keep the same database URL and credentials across
restarts and deployments. Do not run tests against the application database.

## Before the next Railway deployment

1. Inspect the running service's database configuration and storage. Do not
   redeploy or change paths until its existing database has been backed up.
2. For H2, use a database-consistent backup/export of the running database, or
   copy the database after a clean shutdown while the current storage remains
   accessible. Do not copy an actively written `.mv.db` file as a backup.
3. Store the backup outside the deployment container and verify it can restore.
4. If using the default `./data` path with Railway's `/app` working directory,
   attach a persistent volume at `/app/data`. Restore the existing database onto
   that volume before starting the application. A new volume does not migrate
   existing files and can hide files previously present at its mount path.
5. Set `REAL_ESTATE_DATA_DIR=/app/data` only after the existing database has been
   safely migrated there. If `REAL_ESTATE_DB_URL` is set, it overrides this setting;
   confirm it points to the intended persistent database. Existing external MySQL
   databases should keep their current connection configuration.
6. Enable volume/database backups. Verify existing users and record counts before
   and after a controlled restart and deployment.

Railway storage configuration has not been changed by these repository edits.
Reference: https://docs.railway.com/volumes
Backups: https://docs.railway.com/volumes/backups

## Git and sync

Runtime database files must not be versioned or shipped as deployment assets.
The database files were removed from Git tracking, with local copies preserved.
Git shows staged deletions for this change: these remove repository copies only.
Before pulling this commit into another checkout, back up that checkout's database:
Git may remove its previously tracked copies when applying the commit.
Do not restore old database files from Git over the current database.

Git ignore rules do not control OneDrive or other folder-sync software. Avoid
syncing an active H2 database between computers. A migration outside a synced
folder requires a verified backup and an intentional database-path change.

## Verification

`DatabaseRestartTest` uses its own temporary database and checks that users,
leads, properties, tasks, payments, documents, and updates survive an application
restart. This verifies application behavior, not Railway volume provisioning.
