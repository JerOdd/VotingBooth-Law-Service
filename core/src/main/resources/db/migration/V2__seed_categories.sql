INSERT INTO category (code, label) VALUES
    ('JUSTICE_INSTITUTIONS', 'Justice et institutions'),
    ('HEALTH_SOCIAL', 'Santé, travail et solidarités'),
    ('ECONOMY', 'Économie, agriculture et énergie'),
    ('BUDGET', 'Budget et fiscalité'),
    ('ENVIRONMENT', 'Écologie, transports et territoires'),
    ('EDUCATION_CULTURE', 'Éducation, culture et sport'),
    ('FOREIGN_AFFAIRS', 'Affaires étrangères'),
    ('DEFENSE', 'Défense');

-- The eight standing committees of the Assemblée nationale (AMO organes of type COMPER; their ids don't change between legislatures).
-- Special committees (CNPS) are created per text, so they get no rule.
INSERT INTO committee_category (organe_ref, category_id)
SELECT mapping.organe_ref, category.id
FROM (VALUES
    ('PO59051', 'JUSTICE_INSTITUTIONS'), -- Commission des lois
    ('PO420120', 'HEALTH_SOCIAL'),       -- Commission des affaires sociales
    ('PO419610', 'ECONOMY'),             -- Commission des affaires économiques
    ('PO59048', 'BUDGET'),               -- Commission des finances
    ('PO419865', 'ENVIRONMENT'),         -- Commission du développement durable et de l'aménagement du territoire
    ('PO419604', 'EDUCATION_CULTURE'),   -- Commission des affaires culturelles et de l'éducation
    ('PO59047', 'FOREIGN_AFFAIRS'),      -- Commission des affaires étrangères
    ('PO59046', 'DEFENSE')               -- Commission de la défense nationale et des forces armées
) AS mapping (organe_ref, category_code)
JOIN category ON category.code = mapping.category_code;
