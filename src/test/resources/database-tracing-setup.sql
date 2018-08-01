CREATE TABLE serien (
  id   INT          NOT NULL IDENTITY,
  name VARCHAR(255) NOT NULL,
  jahr INT
);

INSERT INTO serien (name, jahr) VALUES ('Babylon 5', 1995);
INSERT INTO serien (name, jahr) VALUES ('Westworld', 2016);
INSERT INTO serien (name, jahr) VALUES ('Raumpatrouille Orion', 1966);
INSERT INTO serien (name, jahr) VALUES ('Dark', 2017);