INSERT INTO authorities(id,authority) VALUES (1,'ADMIN');
INSERT INTO appusers(id,username,password,authority) VALUES (1,'admin1','$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',1);

INSERT INTO authorities(id,authority) VALUES (2,'GYM_OWNER');
INSERT INTO appusers(id,username,password,authority) VALUES (2,'gym_owner1','$2a$10$kcz1B6yRGH1fOyONFQXg8OLEbrANr.nN6FeO47O8qeK4lGTexnDp.',2);

INSERT INTO gym_owners(id,first_name,last_name,user_id) VALUES (1, 'John', 'Doe', 2);

INSERT INTO gyms(id, name, address, telephone, plan, gym_owner)
VALUES (1, 'FitSlot', 'Av. Palmera, 26', '955684230', 'PLATINUM', 1);

INSERT INTO authorities(id,authority) VALUES (4,'INSTRUCTOR');
INSERT INTO authorities(id,authority) VALUES (6,'RECEPTIONIST');
INSERT INTO appusers(id,username,password,authority) VALUES (14,'instructor1','$2a$10$aeypcHWSf4YEkDAF0d.vjOLu94aS40MBUb4rOtDncFxZdo2wpkt8.',4);
INSERT INTO appusers(id,username,password,authority) VALUES (15,'instructor2','$2a$10$aeypcHWSf4YEkDAF0d.vjOLu94aS40MBUb4rOtDncFxZdo2wpkt8.',4);
INSERT INTO appusers(id,username,password,authority) VALUES (16,'instructor3','$2a$10$aeypcHWSf4YEkDAF0d.vjOLu94aS40MBUb4rOtDncFxZdo2wpkt8.',4);
INSERT INTO appusers(id,username,password,authority) VALUES (17,'instructor4','$2a$10$aeypcHWSf4YEkDAF0d.vjOLu94aS40MBUb4rOtDncFxZdo2wpkt8.',4);
INSERT INTO appusers(id,username,password,authority) VALUES (18,'instructor5','$2a$10$aeypcHWSf4YEkDAF0d.vjOLu94aS40MBUb4rOtDncFxZdo2wpkt8.',4);

INSERT INTO appusers(id,username,password,authority) VALUES (19,'receptionist1','$2a$10$FlNJmecQJFCTHfmAtIjxauN94mMeRacVEamMxw6M52bHqWDyJdT9S',6);

INSERT INTO instructors(id, first_name,last_name,city, gym, user_id) VALUES (1, 'James', 'Carter','Sevilla', 1, 14);
INSERT INTO instructors(id, first_name,last_name,city, gym, user_id) VALUES (2, 'Helen', 'Leary','Sevilla', 1, 15);
INSERT INTO instructors(id, first_name,last_name,city, gym, user_id) VALUES (3, 'Linda', 'Douglas','Sevilla', 1, 16);
INSERT INTO instructors(id, first_name,last_name,city, gym, user_id) VALUES (4, 'Rafael', 'Ortega','Badajoz', 1, 17);
INSERT INTO instructors(id, first_name,last_name,city, gym, user_id) VALUES (5, 'Henry', 'Stevens','Badajoz', 1, 18);

INSERT INTO authorities(id,authority) VALUES (5,'MEMBER');
INSERT INTO appusers(id,username,password,authority) VALUES (20,'member1','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (21,'member2','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (22,'member3','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (23,'member4','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (24,'member5','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (25,'member6','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (26,'member7','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (27,'member8','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (28,'member9','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);
INSERT INTO appusers(id,username,password,authority) VALUES (29,'member10','$2y$10$e.H24HBrSx3nvBr28dxii.qjeiw8ar.zrLqu6FWmQXOg1fXanL8Wq',5);

INSERT INTO members (id, first_name, last_name, email, telephone, status)
VALUES
(1, 'James', 'Smith', 'james.smith@example.com', '2025550101', 'ACTIVE'),
(2, 'Emily', 'Johnson', 'emily.johnson@example.com', '2025550102', 'ACTIVE'),
(3, 'Michael', 'Brown', 'michael.brown@example.com', '2025550103', 'ACTIVE'),
(4, 'Olivia', 'Williams', 'olivia.williams@example.com', '2025550104', 'ACTIVE'),
(5, 'William', 'Jones', 'william.jones@example.com', '2025550105', 'ACTIVE'),
(6, 'Sophia', 'Taylor', 'sophia.taylor@example.com', '2025550106', 'ACTIVE'),
(7, 'Daniel', 'Anderson', 'daniel.anderson@example.com', '2025550107', 'ACTIVE'),
(8, 'Charlotte', 'Wilson', 'charlotte.wilson@example.com', '2025550108', 'ACTIVE'),
(9, 'Benjamin', 'Thomas', 'benjamin.thomas@example.com', '2025550109', 'ACTIVE'),
(10, 'Isabella', 'Martin', 'isabella.martin@example.com', '2025550110', 'ACTIVE');

INSERT INTO membership_tiers
(id, name, price, duration_days, entry_quota, time_band)
VALUES
(1, 'Monthly', 40.00, 30, NULL, 'FULL_DAY'),
(2, 'Yearly', 350.00, 365, NULL, 'FULL_DAY'),
(3, '10 Entries', 30.00, NULL, 10, 'FULL_DAY'),
(4, 'Student Morning', 25.00, 30, NULL, 'MORNINGS_ONLY');

INSERT INTO tier_services (tier_id, service_type)
VALUES
(1, 'WEIGHT_ROOM'),
(1, 'POOL'),
(1, 'CLASSES');

INSERT INTO tier_services (tier_id, service_type)
VALUES
(2, 'WEIGHT_ROOM'),
(2, 'POOL'),
(2, 'CLASSES');

INSERT INTO tier_services (tier_id, service_type)
VALUES
(3, 'WEIGHT_ROOM'),
(3, 'POOL'),
(3, 'CLASSES');

INSERT INTO tier_services (tier_id, service_type)
VALUES
(4, 'WEIGHT_ROOM');

INSERT INTO time_band_configurations
    (id, time_band, start_time, end_time)
VALUES
    (1, 'FULL_DAY', '08:00:00', '20:00:00'),
    (2, 'MORNINGS_ONLY', '08:00:00', '12:00:00'),
    (3, 'EVENINGS_ONLY', '12:00:00', '16:00:00'),
    (4, 'AFTERNOONS_ONLY', '16:00:00', '20:00:00');

INSERT INTO medical_certificates
    (id, member_id, issue_date, expiry_date)
VALUES
    (1, 1, '2026-01-01', '2027-01-01'),
    (2, 2, '2026-01-01', '2027-01-01'),
    (3, 3, '2026-01-01', '2026-06-01'),
    (4, 4, '2026-01-01', '2027-01-01'),
    (5, 5, '2026-01-01', '2027-01-01'),
    (6, 6, '2026-01-01', '2027-01-01'),
    (7, 7, '2026-01-01', '2027-01-01'),
    (8, 8, '2026-01-01', '2027-01-01'),
    (9, 9, '2026-01-01', '2027-01-01'),
    (10, 10, '2026-01-01', '2027-01-01');

INSERT INTO member_subscriptions
    (id, member_id, tier_id, start_date, end_date,
     remaining_entries, frozen_days_used, status)
VALUES
    (1, 1, 1, '2026-11-01', '2026-11-30', NULL, 0, 'ACTIVE'),
    (2, 2, 2, '2026-01-01', '2026-12-31', NULL, 0, 'ACTIVE'),
    (3, 3, 1, '2026-11-01', '2026-11-30', NULL, 0, 'ACTIVE'),
    (4, 4, 3, '2026-11-01', '2026-11-30', 10, 0, 'ACTIVE'),
    (5, 5, 4, '2026-11-01', '2026-11-30', NULL, 0, 'ACTIVE'),
    (6, 6, 1, '2026-09-01', '2026-09-30', NULL, 0, 'EXPIRED'),
    (7, 7, 2, '2026-01-01', '2026-12-31', NULL, 10, 'FROZEN'),
    (8, 8, 3, '2026-11-01', '2026-11-30', 0, 0, 'ACTIVE'),
    (9, 9, 1, '2026-11-01', '2026-11-30', NULL, 0, 'ACTIVE'),
    (10, 10, 4, '2026-11-01', '2026-11-30', NULL, 0, 'ACTIVE');

INSERT INTO payment_records
    (id, member_id, subscription_id, amount, payment_date, status)
VALUES
    (1, 1, 1, 40.00, '2026-11-01 09:00:00', 'PAID'),
    (2, 2, 2, 350.00, '2026-01-01 10:00:00', 'PAID'),
    (3, 3, 3, 40.00, '2026-11-01 09:30:00', 'PAID'),
    (4, 4, 4, 30.00, '2026-11-01 11:00:00', 'PAID'),
    (5, 5, 5, 25.00, '2026-11-01 08:30:00', 'PAID'),
    (6, 6, 6, 40.00, '2026-09-01 09:00:00', 'PAID'),
    (7, 7, 7, 350.00, '2026-01-01 10:30:00', 'PAID'),
    (8, 8, 8, 30.00, '2026-11-01 12:00:00', 'PAID'),
    (9, 9, 9, 40.00, '2026-11-01 09:15:00', 'PAID'),
    (10, 10, 10, 25.00, '2026-11-01 08:45:00', 'PENDING');

INSERT INTO class_sessions
(id, name, room, instructor_id, gym_id, date, start_time, end_time, max_capacity)
VALUES
(101, 'Yoga', 'CLASS_ROOM_1', 1, 1, '2026-11-23', '09:00:00', '10:00:00', 20),
(102, 'Pilates', 'CLASS_ROOM_2', NULL, 1, '2026-11-23', '10:00:00', '11:00:00', 15),
(103, 'Spinning', 'CLASS_ROOM_1', 3, 1, '2026-11-23', '11:00:00', '12:00:00', 18),
(104, 'Aqua Aerobics', 'POOL', 2, 1, '2026-11-24', '09:00:00', '10:00:00', 15),
(105, 'Yoga', 'CLASS_ROOM_1', NULL, 1, '2026-11-24', '17:00:00', '18:00:00', 20),
(106, 'Pilates', 'CLASS_ROOM_2', 2, 1, '2026-11-25', '10:00:00', '11:00:00', 15),
(107, 'Spinning', 'CLASS_ROOM_1', 3, 1, '2026-11-25', '18:00:00', '19:00:00', 18),
(108, 'Yoga', 'CLASS_ROOM_2', 1, 1, '2026-11-26', '09:00:00', '10:00:00', 20),
(109, 'Aqua Aerobics', 'POOL', 2, 1, '2026-11-27', '11:00:00', '12:00:00', 15),
(110, 'Pilates', 'CLASS_ROOM_1', 3, 1, '2026-11-27', '17:00:00', '18:00:00', 15);


INSERT INTO instructor_availabilities (id, instructor_id, date, start_time, end_time)
VALUES
(1001, 1, '2026-11-23', '08:00:00', '10:00:00'),
(1002, 1, '2026-11-24', '08:00:00', '11:00:00'),
(1003, 1, '2026-11-24', '16:00:00', '19:00:00'),
(1004, 1, '2026-11-25', '10:00:00', '12:00:00'),
(1005, 1, '2026-11-25', '17:00:00', '20:00:00'),
(1006, 1, '2026-11-26', '09:00:00', '12:00:00'),
(1007, 1, '2026-11-27', '10:00:00', '12:00:00'),
(1008, 1, '2026-11-27', '16:00:00', '19:00:00'),
(1009, 1, '2026-11-30', '09:00:00', '12:00:00'),
(1010, 1, '2026-12-02', '16:00:00', '19:00:00');


INSERT INTO instructor_availabilities (id, instructor_id, date, start_time, end_time)
VALUES
(1011, 2, '2026-11-23', '09:00:00', '12:00:00'),
(1012, 2, '2026-11-24', '09:00:00', '11:00:00'),
(1013, 2, '2026-11-24', '16:00:00', '19:00:00'),
(1014, 2, '2026-11-25', '09:00:00', '12:00:00'),
(1015, 2, '2026-11-25', '17:00:00', '19:00:00'),
(1016, 2, '2026-11-26', '08:00:00', '10:00:00'),
(1017, 2, '2026-11-27', '10:00:00', '13:00:00'),
(1018, 2, '2026-11-27', '17:00:00', '19:00:00'),
(1019, 2, '2026-11-30', '10:00:00', '13:00:00'),
(1020, 2, '2026-12-02', '09:00:00', '12:00:00');


INSERT INTO instructor_availabilities (id, instructor_id, date, start_time, end_time)
VALUES
(1021, 3, '2026-11-23', '08:00:00', '13:00:00'),
(1022, 3, '2026-11-24', '08:00:00', '10:00:00'),
(1023, 3, '2026-11-24', '16:00:00', '18:00:00'),
(1024, 3, '2026-11-25', '10:00:00', '12:00:00'),
(1025, 3, '2026-11-25', '17:00:00', '20:00:00'),
(1026, 3, '2026-11-26', '09:30:00', '13:00:00'),
(1027, 3, '2026-11-27', '09:00:00', '12:00:00'),
(1028, 3, '2026-11-27', '16:00:00', '19:00:00'),
(1029, 3, '2026-11-30', '11:00:00', '14:00:00'),
(1030, 3, '2026-12-02', '10:00:00', '14:00:00');


INSERT INTO instructor_availabilities (id, instructor_id, date, start_time, end_time)
VALUES
(1031, 4, '2026-11-23', '09:30:00', '12:00:00'),
(1032, 4, '2026-11-24', '09:30:00', '12:00:00'),
(1033, 4, '2026-11-24', '17:00:00', '20:00:00'),
(1034, 4, '2026-11-25', '10:00:00', '11:00:00'),
(1035, 4, '2026-11-25', '18:00:00', '20:00:00'),
(1036, 4, '2026-11-26', '08:00:00', '12:00:00'),
(1037, 4, '2026-11-27', '11:00:00', '13:00:00'),
(1038, 4, '2026-11-27', '17:00:00', '18:00:00'),
(1039, 4, '2026-11-30', '08:00:00', '12:00:00'),
(1040, 4, '2026-12-02', '17:00:00', '20:00:00');

INSERT INTO instructor_availabilities (id, instructor_id, date, start_time, end_time)
VALUES
(1041, 5, '2026-11-23', '08:00:00', '11:00:00'),
(1042, 5, '2026-11-24', '09:00:00', '12:00:00'),
(1043, 5, '2026-11-24', '16:00:00', '20:00:00'),
(1044, 5, '2026-11-25', '09:00:00', '11:00:00'),
(1045, 5, '2026-11-25', '17:00:00', '18:00:00'),
(1046, 5, '2026-11-26', '10:00:00', '14:00:00'),
(1047, 5, '2026-11-27', '09:00:00', '11:00:00'),
(1048, 5, '2026-11-27', '16:00:00', '18:00:00'),
(1049, 5, '2026-11-30', '16:00:00', '19:00:00'),
(1050, 5, '2026-12-02', '09:00:00', '11:00:00');
