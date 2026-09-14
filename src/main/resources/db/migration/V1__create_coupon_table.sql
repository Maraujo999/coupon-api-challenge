CREATE TABLE coupon (
    id UUID PRIMARY KEY,
    code VARCHAR(6) NOT NULL CHECK (REGEXP_LIKE(code, '^[A-Za-z0-9]{6}$')),
    description CLOB NOT NULL CHECK (CHAR_LENGTH(TRIM(description)) > 0),
    discount_value DECFLOAT(1000) NOT NULL CHECK (discount_value >= 0.5 AND discount_value < CAST('Infinity' AS DECFLOAT)),
    expiration_date TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    status VARCHAR(8) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    published BOOLEAN NOT NULL,
    redeemed BOOLEAN NOT NULL,
    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP(9) WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_coupon_deletion CHECK (
        (status = 'DELETED' AND deleted_at IS NOT NULL AND deleted_at >= created_at)
        OR (status <> 'DELETED' AND deleted_at IS NULL)
    )
);
