# Refund account security and PayOS payouts

The backend requires `REFUND_ENCRYPTION_KEY` before it starts. Generate a fresh
32-byte key and store it in the deployment secret manager, for example:

```powershell
$bytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Do not commit the generated value or store it beside the database. Keep a
protected backup of the key: losing it makes encrypted pending refund details
unreadable. Key rotation requires decrypting and re-encrypting pending account
data with a controlled migration.

Existing booking-cancellation bank details are encrypted when the backend
starts. Refund list responses omit account numbers; only the manager/admin
bank-details endpoint reveals the decrypted values for review. Account details
are deleted immediately for zero-refund, rejected, and completed-payout
cancellations. Pending account details expire after 30 days.

Booking cancellation reasons are stored in `bookings.booking_status` as
`CANCELLED_BY_CUSTOMER`, `CANCELLED_BY_STAFF`, or
`CANCELLED_PAYMENT_TIMEOUT`. The refund workflow remains in
`booking_cancellations.status`. If the old `cancellation_source` column exists,
run `booking-cancellation-status-migration.sql` once to backfill statuses and
drop that redundant column.

Payout requests are created server-side through the PayOS Java SDK with a
stable idempotency key. A scheduled status check marks the booking refunded
only after PayOS reports the payout and its transactions as completed. The
merchant account must be enabled for PayOS payouts before this flow can be
used in production.
