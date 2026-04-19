USE CoOwnershipDB; 
UPDATE users 
SET password_hash = '$2a$10$7eqJtq98hPqEX7fNZaFWoOa9LzdFea5Z8SGvtQvE4H14R/2uOeG6.', 
    role = 'ROLE_ADMIN', 
    role_requested = 'ROLE_ADMIN', 
    profile_status = 'APPROVED', 
    is_verified = 1 
WHERE email = 'namvohoai23@gmail.com';
