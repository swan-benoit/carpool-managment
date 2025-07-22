INSERT INTO absencedays (weekday, weektype, id) VALUES (3, 0, 1);
INSERT INTO absencedays (weekday, weektype, id) VALUES (2, 1, 2);
INSERT INTO absencedays (weekday, weektype, id) VALUES (2, 0, 3);
INSERT INTO absencedays (weekday, weektype, id) VALUES (3, 1, 4);

ALTER SEQUENCE absencedays_id_seq RESTART WITH 5;

INSERT INTO child (id, name) VALUES (1, 'Mael');
INSERT INTO child (id, name) VALUES (2, 'Luce');
INSERT INTO child (id, name) VALUES (3, 'Come');
INSERT INTO child (id, name) VALUES (4, 'Rose');
INSERT INTO child (id, name) VALUES (5, 'Issa');
INSERT INTO child (id, name) VALUES (6, 'Hedi');
INSERT INTO child (id, name) VALUES (7, 'Mélanie');
INSERT INTO child (id, name) VALUES (8, 'Anna');
INSERT INTO child (id, name) VALUES (9, 'Orion');
INSERT INTO child (id, name) VALUES (10, 'Marin');

ALTER SEQUENCE child_id_seq RESTART WITH 11;

INSERT INTO child_absencedays (child_id, absencedays_id) VALUES (8, 1);
INSERT INTO child_absencedays (child_id, absencedays_id) VALUES (8, 2);
INSERT INTO child_absencedays (child_id, absencedays_id) VALUES (8, 3);
INSERT INTO child_absencedays (child_id, absencedays_id) VALUES (8, 4);

INSERT INTO family (carcapacity, id, name) VALUES (7, 1, 'Romain et Virginie');
INSERT INTO family (carcapacity, id, name) VALUES (4, 2, 'Anne et Swan');
INSERT INTO family (carcapacity, id, name) VALUES (4, 3, 'Axel et Soizic');
INSERT INTO family (carcapacity, id, name) VALUES (4, 4, 'Abdou et Lydia');
INSERT INTO family (carcapacity, id, name) VALUES (4, 5, 'Sonia et Jean philippe');
INSERT INTO family (carcapacity, id, name) VALUES (4, 6, 'Cécile');
INSERT INTO family (carcapacity, id, name) VALUES (4, 7, 'Josephine et Mickaël');
INSERT INTO family (carcapacity, id, name) VALUES (4, 8, 'Lucie et Raphaël');

ALTER SEQUENCE family_id_seq RESTART WITH 9;

INSERT INTO family_child (family_id, children_id) VALUES (1, 1);
INSERT INTO family_child (family_id, children_id) VALUES (2, 2);
INSERT INTO family_child (family_id, children_id) VALUES (3, 3);
INSERT INTO family_child (family_id, children_id) VALUES (3, 4);
INSERT INTO family_child (family_id, children_id) VALUES (4, 6);
INSERT INTO family_child (family_id, children_id) VALUES (4, 5);
INSERT INTO family_child (family_id, children_id) VALUES (5, 7);
INSERT INTO family_child (family_id, children_id) VALUES (6, 8);
INSERT INTO family_child (family_id, children_id) VALUES (7, 9);
INSERT INTO family_child (family_id, children_id) VALUES (8, 10);
