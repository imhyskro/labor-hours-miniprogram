USE labor_management;
UPDATE sys_user
SET password_hash = '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
    first_login = 0
WHERE username = 'admin';
