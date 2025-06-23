INSERT INTO family (carcapacity, name) VALUES (7,  'Romain et Virginie');
INSERT INTO family (carcapacity, name) VALUES (4,  'Anne et Swan');
INSERT INTO family (carcapacity, name) VALUES (4,  'Axel et Soizic');
INSERT INTO family (carcapacity, name) VALUES (4,  'Abdou et Lydia');
INSERT INTO family (carcapacity, name) VALUES (4,  'Sonia et Jean philippe');

INSERT INTO child (family_id, name) VALUES ((SELECT id from family where family.name = 'Romain et Virginie'),  'Mael');
INSERT INTO child (family_id, name) VALUES ((SELECT id from family where family.name = 'Anne et Swan'),  'Luce');
INSERT INTO child (family_id, name) VALUES ((SELECT id from family where family.name = 'Axel et Soizic'),  'Come');
INSERT INTO child (family_id, name) VALUES ((SELECT id from family where family.name = 'Axel et Soizic'),  'Rose');
INSERT INTO child (family_id, name) VALUES ((SELECT id from family where family.name = 'Abdou et Lydia'),  'Issa');
INSERT INTO child (family_id, name) VALUES ((SELECT id from family where family.name = 'Abdou et Lydia'),  'Hedi');
INSERT INTO child (family_id, name) VALUES ((SELECT id from family where family.name = 'Sonia et Jean philippe'),  'Laetitia');
INSERT INTO child (family_id, name) VALUES ((SELECT id from family where family.name = 'Sonia et Jean philippe'),  'Mélanie');


