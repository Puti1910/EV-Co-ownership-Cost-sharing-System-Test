USE Group_Management_DB;
DELETE FROM `Group` WHERE vehicleId = 2;
DELETE FROM GroupMember WHERE groupId IN (SELECT groupId FROM `Group` WHERE vehicleId = 2);

USE co_ownership_booking;
DELETE FROM vehicles WHERE vehicle_id = 2;
