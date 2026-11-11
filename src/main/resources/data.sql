INSERT INTO authorities(id,authority) VALUES (1,'ADMIN');
INSERT INTO appusers(id,username,password,authority) VALUES (1,'admin1','$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',1);

INSERT INTO authorities(id,authority) VALUES (2,'GYM_OWNER');
INSERT INTO appusers(id,username,password,authority) VALUES (2,'gym_owner1','$2a$10$kcz1B6yRGH1fOyONFQXg8OLEbrANr.nN6FeO47O8qeK4lGTexnDp.',2);
INSERT INTO appusers(id,username,password,authority) VALUES (3,'gymOwner2','$2a$10$kcz1B6yRGH1fOyONFQXg8OLEbrANr.nN6FeO47O8qeK4lGTexnDp.',2);

INSERT INTO gym_owners(id,first_name,last_name,user_id) VALUES (1, 'John', 'Doe', 2);
INSERT INTO gym_owners(id,first_name,last_name,user_id) VALUES (2, 'Jane', 'Doe', 3);

INSERT INTO gyms(id, name, address, telephone, plan, gym_owner) VALUES (1, 'Gym 1', 'Av. Palmera, 26', '955684230', 'PLATINUM', 1);
INSERT INTO gyms(id, name, address, telephone, plan, gym_owner) VALUES (2, 'Gym 2', 'Av. Torneo, 52', '955634232', 'GOLD', 2);
INSERT INTO gyms(id, name, address, telephone, plan, gym_owner) VALUES (3, 'Gym 3', 'Av. Reina Mercedes, 70', '955382238', 'BASIC', 2);

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
INSERT INTO instructors(id, first_name,last_name,city, gym, user_id) VALUES (3, 'Linda', 'Douglas','Sevilla', 2, 16);
INSERT INTO instructors(id, first_name,last_name,city, gym, user_id) VALUES (4, 'Rafael', 'Ortega','Badajoz', 2, 17);
INSERT INTO instructors(id, first_name,last_name,city, gym, user_id) VALUES (5, 'Henry', 'Stevens','Badajoz', 3, 18);

INSERT INTO authorities(id,authority) VALUES (5,'MEMBER');

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
