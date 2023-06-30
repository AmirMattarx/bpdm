CREATE TABLE name_parts (
  id BIGINT NOT NULL,
  created_at TIMESTAMP WITH time zone NOT NULL,
  updated_at TIMESTAMP WITH time zone NOT NULL,
  uuid UUID NOT NULL,
  address_id BIGINT NULL,
  site_id BIGINT NULL,
  legal_entity_id BIGINT NULL,
  name_part VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX address_index_name_parts ON name_parts (address_id);

CREATE INDEX site_index_name_parts ON name_parts (site_id);

CREATE INDEX legal_entity_name_parts ON name_parts (legal_entity_id);

ALTER TABLE IF EXISTS name_parts
ADD CONSTRAINT uuid_name_parts_uk UNIQUE (uuid);

ALTER TABLE IF EXISTS name_parts
ADD CONSTRAINT fk_address_name_parts FOREIGN KEY (address_id) REFERENCES logistic_addresses,
ADD CONSTRAINT fk_legal_entity_name_parts FOREIGN KEY (legal_entity_id) REFERENCES legal_entities,
ADD CONSTRAINT fk_sites_name_parts FOREIGN KEY (site_id) REFERENCES sites;

INSERT INTO name_parts (id, address_id, name_part, created_at, updated_at, uuid)
SELECT nextval('bpdm_sequence'), id, name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, uuid
FROM logistic_addresses WHERE name IS NOT NULL;

INSERT INTO name_parts (id, site_id, name_part, created_at, updated_at, uuid)
SELECT nextval('bpdm_sequence'), id, name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, uuid
FROM sites WHERE name IS NOT NULL;

INSERT INTO name_parts (id, legal_entity_id, name_part, created_at, updated_at, uuid)
SELECT nextval('bpdm_sequence'), id, name_shortname, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, uuid
FROM legal_entities WHERE name_shortname IS NOT NULL;

ALTER TABLE logistic_addresses
DROP COLUMN IF EXISTS name;

ALTER TABLE sites
DROP COLUMN IF EXISTS name;

ALTER TABLE legal_entities
DROP COLUMN IF EXISTS name_value;