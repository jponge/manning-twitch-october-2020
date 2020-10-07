CREATE TABLE IF NOT EXISTS temperature_records
(
    uuid   VARCHAR,
    tstamp timestamptz,
    value  DOUBLE PRECISION,
    PRIMARY KEY (uuid, tstamp)
);
