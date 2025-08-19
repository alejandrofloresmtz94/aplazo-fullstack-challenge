-- Remove DEFAULT CURRENT_TIMESTAMP from created_at columns to avoid conflicts with JPA @CreationTimestamp
ALTER TABLE customers ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE loans ALTER COLUMN created_at DROP DEFAULT;
