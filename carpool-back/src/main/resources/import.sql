INSERT INTO family (carcapacity, name) VALUES (7,  'Romain et Virginie');
INSERT INTO family (carcapacity, name) VALUES (4,  'Anne et Swan');
INSERT INTO family (carcapacity, name) VALUES (4,  'Axel et Soizic');
INSERT INTO family (carcapacity, name) VALUES (4,  'Abdou et Lydia');
INSERT INTO family (carcapacity, name) VALUES (4,  'Sonia et Jean philippe');
INSERT INTO family (carcapacity, name) VALUES (4,  'Cécile');
INSERT INTO family (carcapacity, name) VALUES (4,  'Josephine et Mickaël');
INSERT INTO family (carcapacity, name) VALUES (4,  'Lucie et Raphaël');
INSERT INTO family (carcapacity, name) VALUES (4,  'Marie et Fabien');

INSERT INTO child (name) VALUES ('Mael');
INSERT INTO child (name) VALUES ( 'Luce');
INSERT INTO child (name) VALUES ( 'Come');
INSERT INTO child (name) VALUES ( 'Rose');
INSERT INTO child (name) VALUES ( 'Issa');
INSERT INTO child (name) VALUES ( 'Hedi');
INSERT INTO child (name) VALUES ( 'Mélanie');
INSERT INTO child (name) VALUES ( 'Anna');
INSERT INTO child (name) VALUES ( 'Orion');
INSERT INTO child (name) VALUES ( 'Marin');
INSERT INTO child (name) VALUES ( 'Léonie');

INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Romain et Virginie'), (SELECT id from child where name = 'Mael'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Anne et Swan'), (SELECT id from child where name = 'Luce'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Axel et Soizic'), (SELECT id from child where name = 'Come'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Axel et Soizic'), (SELECT id from child where name = 'Rose'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Abdou et Lydia'), (SELECT id from child where name = 'Hedi'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Abdou et Lydia'), (SELECT id from child where name = 'Issa'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Sonia et Jean philippe'), (SELECT id from child where name = 'Mélanie'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Cécile'), (SELECT id from child where name = 'Anna'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Josephine et Mickaël'), (SELECT id from child where name = 'Orion'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Lucie et Raphaël'), (SELECT id from child where name = 'Marin'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Marie et Fabien'), (SELECT id from child where name = 'Léonie'));

INSERT INTO absencedays (weekday, weektype) VALUES (3, 0);
INSERT INTO absencedays (weekday, weektype) VALUES (2, 1);
INSERT INTO absencedays (weekday, weektype) VALUES (2, 0);
INSERT INTO absencedays (weekday, weektype) VALUES (3, 1);

INSERT INTO child_absencedays (child_id, absencedays_id) VALUES ((SELECT id from child where name = 'Anna'), (SELECT id from absencedays where weekday = 3 and weektype = 0));
INSERT INTO child_absencedays (child_id, absencedays_id) VALUES ((SELECT id from child where name = 'Anna'), (SELECT id from absencedays where weekday = 2 and weektype = 1));
INSERT INTO child_absencedays (child_id, absencedays_id) VALUES ((SELECT id from child where name = 'Anna'), (SELECT id from absencedays where weekday = 2 and weektype = 0));
INSERT INTO child_absencedays (child_id, absencedays_id) VALUES ((SELECT id from child where name = 'Anna'), (SELECT id from absencedays where weekday = 3 and weektype = 1));


