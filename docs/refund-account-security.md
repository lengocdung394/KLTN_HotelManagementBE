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

Existing booking-cancellation and checkout-refund bank details are encrypted
when the backend starts. Refund list responses omit all account details and
include only a flag indicating whether details are stored. Manager/admin-only
bank-details endpoints reveal decrypted values for review; the checkout
endpoint also verifies access to the refund's hotel. Account details are
deleted immediately for zero-refund, rejected, and completed-payout
cancellations, and after a checkout refund is completed. Pending cancellation
account details expire after 30 days.

Checkout refund account details use the same AES-GCM encryption helper and
`REFUND_ENCRYPTION_KEY` as booking cancellations. The checkout-refund database
columns allow room for the encrypted values. Keep the key stable across
deployments; replacing it without a controlled key-rotation migration makes
pending details unreadable.

For bank refunds selected during check-out, bank name, account holder, account
number, and optional bank code are encrypted before the refund is saved. The
manager review step can retrieve the saved details through the protected
checkout-refund bank-details endpoint.

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
