-- Migration script: align database with mobile app field names
-- Run this ONLY if the database already has data with old column names.
-- If starting fresh (empty DB), Hibernate ddl-auto=update will create correct columns automatically.

-- 1. Rename user columns
ALTER TABLE users RENAME COLUMN first_name TO prenom;
ALTER TABLE users RENAME COLUMN last_name TO nom;

-- 2. Add new user columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS matricule VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS poste VARCHAR(255);

-- 3. Update enum values in attendance status
UPDATE attendances SET status = 'RETARD' WHERE status = 'LATE';

-- 4. Update enum values in user roles
UPDATE users SET role = 'EMPLOYE' WHERE role = 'EMPLOYEE';

-- 5. Verify
SELECT id, prenom, nom, email, matricule, role FROM users;
SELECT id, status FROM attendances WHERE status IN ('RETARD', 'PRESENT', 'ABSENT');
