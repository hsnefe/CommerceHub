-- Demo accounts for the public deployment, so the demo can be tried without
-- registering first.
--
--   demo@commercehub.dev   / demo1234    USER
--   admin@commercehub.dev  / admin1234   ADMIN + USER
--
-- The passwords are deliberately public: this catalog is disposable demo data
-- on a throwaway host. Do not reuse these accounts for anything else.
--
-- Hashes are BCrypt $2a$ at cost 10, which is what the service's
-- new BCryptPasswordEncoder() produces and verifies.
--
-- Emails are stored lower case because AuthService lowercases the address on
-- both register and login; a mixed-case row here would never match a lookup.
--
-- email_verified is true so the accounts work regardless of how
-- AUTH_EMAIL_VERIFICATION_REQUIRED is set.
--
-- Role ids come from V2__seed_roles.sql.

INSERT INTO users (id, email, password_hash, email_verified) VALUES
    ('d0000000-0000-4000-8000-000000000001', 'demo@commercehub.dev',
     '$2a$10$xS260ZtLAjwkavDOt29K0Oy.iy.lbegZxCgFluvN/4hfAGcz8p8Oq', TRUE),
    ('d0000000-0000-4000-8000-000000000002', 'admin@commercehub.dev',
     '$2a$10$RDVnjtNFItzeb0.AbMLhY.n6PVsDivkZi/8J9iLbtVhLuVdaK5VNG', TRUE);

INSERT INTO user_roles (user_id, role_id) VALUES
    -- demo -> USER
    ('d0000000-0000-4000-8000-000000000001', '00000000-0000-0000-0000-000000000001'),
    -- admin -> ADMIN and USER
    ('d0000000-0000-4000-8000-000000000002', '00000000-0000-0000-0000-000000000002'),
    ('d0000000-0000-4000-8000-000000000002', '00000000-0000-0000-0000-000000000001');
