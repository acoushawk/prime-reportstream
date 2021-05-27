/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/
CREATE TABLE test_data
(
    -- the message id, ties back to the item_lineage table
    tracking_id VARCHAR(128) NULL,

    -- equipment information
    equipment_model VARCHAR(512) NULL,
    equipment_manufacturer VARCHAR(512) NULL,

    -- test result info
    abnormal_flag VARCHAR(32) NULL,
    abnormal_flag_code VARCHAR(32) NULL,
    test_result VARCHAR(512) NULL,
    test_result_code VARCHAR(32) NULL,

    -- ordering facility information
    ordering_facility_name VARCHAR(512) NULL,
    ordering_facility_city VARCHAR(128) NULL,
    ordering_facility_county VARCHAR(128) NULL,
    ordering_facility_state VARCHAR(128) NULL,
    ordering_facility_state_code VARCHAR(16) NULL,
    ordering_facility_postal_code VARCHAR(32) NULL,

    -- ordering provider information
    ordering_provider_name VARCHAR(512) NULL,
    ordering_provider_id VARCHAR(128) NULL,
    ordering_provider_state VARCHAR(128) NULL,
    ordering_provider_state_code VARCHAR(16) NULL,
    ordering_provider_postal_code VARCHAR(32) NULL,

    -- deidentified patient data
    patient_state VARCHAR(128) NULL,
    patient_state_code VARCHAR(16) NULL,
    patient_county VARCHAR(128) NULL,
    patient_postal_code VARCHAR(32) NULL,
    patient_age VARCHAR(64) NULL,
    patient_gender VARCHAR(32) NULL,
    patient_gender_code VARCHAR(32) NULL,
    patient_ethnicity VARCHAR(64) NULL,
    patient_ethnicity_code VARCHAR(32) NULL,
    patient_race VARCHAR(64) NULL,
    patient_race_code VARCHAR(32) NULL,
    patient_tribal_citizenship VARCHAR(128) NULL,
    patient_tribal_citizenship_code VARCHAR(32) NULL,
    specimen_collection_date_time DATE NULL,

    -- testing lab information
    testing_lab_city VARCHAR(128) NULL,
    testing_lab_county VARCHAR(128) NULL,
    testing_lab_name VARCHAR(512) NULL,
    testing_lab_clia VARCHAR(64) NULL,

    -- the client who sent in the information (simple_report, etc)
    client_id VARCHAR(128) NULL
);

COMMENT ON TABLE test_data IS 'A collection of deidentified data from tests to allow us to pull metrics';

CREATE INDEX ON test_data(tracking_id);
