CREATE TABLE aggregate_report (
    name VARCHAR(64) NOT NULL,
    selectorPattern VARCHAR(256) NOT NULL,
    timeWindows VARCHAR(1024) NOT NULL,
    thresholds VARCHAR(1024) NOT NULL,
    PRIMARY KEY (name));
