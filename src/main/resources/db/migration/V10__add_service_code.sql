ALTER TABLE services ADD COLUMN code VARCHAR(100);

UPDATE services SET code = 'document-authentication' WHERE id = 1;
UPDATE services SET code = 'power-of-attorney' WHERE id = 2;
UPDATE services SET code = 'certified-translation-notarization' WHERE id = 3;
UPDATE services SET code = 'deeds-and-contracts' WHERE id = 4;
UPDATE services SET code = 'probate-proceedings' WHERE id = 5;

ALTER TABLE services ALTER COLUMN code SET NOT NULL;
ALTER TABLE services ADD CONSTRAINT uq_services_code UNIQUE (code);
