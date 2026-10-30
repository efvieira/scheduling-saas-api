CREATE TABLE businesses (
    id       UUID PRIMARY KEY,
    slug     VARCHAR(60)  NOT NULL UNIQUE,
    name     VARCHAR(120) NOT NULL,
    timezone VARCHAR(40)  NOT NULL,
    whatsapp VARCHAR(20)
);

CREATE TABLE staff_users (
    id            UUID PRIMARY KEY,
    business_id   UUID         NOT NULL REFERENCES businesses (id),
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL
);

CREATE TABLE services (
    id               UUID PRIMARY KEY,
    business_id      UUID           NOT NULL REFERENCES businesses (id),
    name             VARCHAR(120)   NOT NULL,
    duration_minutes INT            NOT NULL,
    price            NUMERIC(10, 2) NOT NULL,
    prepaid          BOOLEAN        NOT NULL,
    active           BOOLEAN        NOT NULL
);

CREATE TABLE professionals (
    id          UUID PRIMARY KEY,
    business_id UUID         NOT NULL REFERENCES businesses (id),
    name        VARCHAR(120) NOT NULL,
    active      BOOLEAN      NOT NULL
);

CREATE TABLE working_hours (
    professional_id UUID        NOT NULL REFERENCES professionals (id),
    day_of_week     VARCHAR(10) NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL
);

CREATE TABLE appointments (
    id               UUID PRIMARY KEY,
    business_id      UUID           NOT NULL REFERENCES businesses (id),
    professional_id  UUID           NOT NULL REFERENCES professionals (id),
    service_id       UUID           NOT NULL REFERENCES services (id),
    customer_name    VARCHAR(120)   NOT NULL,
    customer_phone   VARCHAR(20)    NOT NULL,
    start_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    price            NUMERIC(10, 2) NOT NULL,
    pix_txid         VARCHAR(35) UNIQUE,
    pix_copia_e_cola VARCHAR(512),
    reminder_sent_at TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT         NOT NULL
);

CREATE INDEX idx_appointments_professional ON appointments (professional_id, start_at);
CREATE INDEX idx_appointments_business_day ON appointments (business_id, start_at);
CREATE INDEX idx_appointments_reminder ON appointments (status, start_at);
