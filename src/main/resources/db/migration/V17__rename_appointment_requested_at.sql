-- requested_at sat one word away from created_at (when the booking was made) and the two were
-- easy to mix up; requested_for says the slot the appointment is for. Same portable form as V12.
ALTER TABLE appointments RENAME COLUMN requested_at TO requested_for;
ALTER INDEX idx_appointments_requested_at RENAME TO idx_appointments_requested_for;
