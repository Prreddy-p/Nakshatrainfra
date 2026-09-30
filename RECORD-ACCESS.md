# Record visibility

All uploaded property photos are public on the login-page gallery, as requested.
The public API exposes photo IDs and image bytes only; PDFs, Office documents,
other attachments, and property details retain the access rules below. Public
image responses check raster-image signatures and do not serve arbitrary documents.

- Admin and Manager can access all application records and manage users.
- Associate can access only records whose creator user ID matches their account.
- Customer records and payments attached to a lead retain the lead's ownership,
  including when a Manager converts or edits that lead.
- User management is unavailable to Associates. Anonymous API requests require login.
- Lists, searches, dashboard counts, direct record URLs, edits, deletions, booking
  property references, and lead payment endpoints enforce access on the server.

Ownership is recorded automatically on creation, cannot be supplied through JSON,
and is preserved when an existing record is edited. Display names are not used for
authorization. The server reloads the signed-in account for each request, so role
changes and deleted accounts take effect without trusting browser session storage.

Existing rows are preserved. Rows created before creator IDs were recorded remain
visible to Admin and Manager only. No owner is inferred from a name, assigned agent,
or email. Assigning historical ownership requires a separately reviewed migration.

Restart/redeploy the application to activate these changes, preserving the current
database as described in DATA-PERSISTENCE.md. Existing sessions must sign in again
if they do not have a server-side account ID. Browser refresh uses the server session
to restore the current user. Signing out reloads the page to clear cached records.

Run `mvn test` to verify access isolation, audit fields, conversion/payment behavior,
and database persistence using separate test databases.
