INSERT INTO family (carcapacity, name) VALUES (7,  'Romain et Virginie');
INSERT INTO family (carcapacity, name) VALUES (4,  'Anne et Swan');
INSERT INTO family (carcapacity, name) VALUES (4,  'Axel et Soizic');
INSERT INTO family (carcapacity, name) VALUES (4,  'Abdou et Lydia');
INSERT INTO family (carcapacity, name) VALUES (4,  'Sonia et Jean philippe');
INSERT INTO family (carcapacity, name) VALUES (4,  'Cécile');

INSERT INTO child (name) VALUES ('Mael');
INSERT INTO child (name) VALUES ( 'Luce');
INSERT INTO child (name) VALUES ( 'Come');
INSERT INTO child (name) VALUES ( 'Rose');
INSERT INTO child (name) VALUES ( 'Issa');
INSERT INTO child (name) VALUES ( 'Hedi');
INSERT INTO child (name) VALUES ( 'Laetitia');
INSERT INTO child (name) VALUES ( 'Mélanie');
INSERT INTO child (name) VALUES ( 'Anna');

INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Romain et Virginie'), (SELECT id from child where name = 'Mael'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Anne et Swan'), (SELECT id from child where name = 'Luce'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Axel et Soizic'), (SELECT id from child where name = 'Come'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Axel et Soizic'), (SELECT id from child where name = 'Rose'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Abdou et Lydia'), (SELECT id from child where name = 'Hedi'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Abdou et Lydia'), (SELECT id from child where name = 'Issa'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Sonia et Jean philippe'), (SELECT id from child where name = 'Laetitia'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Sonia et Jean philippe'), (SELECT id from child where name = 'Mélanie'));
INSERT INTO family_child (family_id, children_id) VALUES ((SELECT id from family where name = 'Cécile'), (SELECT id from child where name = 'Anna'));


