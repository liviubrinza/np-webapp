INSERT INTO services (id, duration_minutes, active) VALUES (1, 30, true);
INSERT INTO services (id, duration_minutes, active) VALUES (2, 45, true);
INSERT INTO services (id, duration_minutes, active) VALUES (3, 20, true);

ALTER TABLE services ALTER COLUMN id RESTART WITH 4;

INSERT INTO service_translations (service_id, locale, name, description) VALUES
    (1, 'en', 'Document Authentication', 'Certification of copies and signatures for official and personal documents.'),
    (1, 'ro', 'Autentificare Documente', 'Certificarea copiilor și semnăturilor pentru documente oficiale și personale.'),
    (1, 'hu', 'Dokumentum-hitelesítés', 'Másolatok és aláírások hitelesítése hivatalos és személyes iratokhoz.'),

    (2, 'en', 'Power of Attorney', 'Drafting and notarizing general or special powers of attorney.'),
    (2, 'ro', 'Procură', 'Redactarea și autentificarea procurilor generale sau speciale.'),
    (2, 'hu', 'Meghatalmazás', 'Általános vagy különleges meghatalmazások szerkesztése és hitelesítése.'),

    (3, 'en', 'Certified Translation Notarization', 'Notarization of certified translations for use with authorities abroad.'),
    (3, 'ro', 'Legalizare Traduceri', 'Legalizarea traducerilor autorizate pentru utilizare la autorități din străinătate.'),
    (3, 'hu', 'Hiteles fordítás közjegyzői hitelesítése', 'Hiteles fordítások közjegyzői hitelesítése külföldi hatóságok számára.');
