CREATE TABLE IF NOT EXISTS customer
(
    c_id           INT8           NOT NULL,
    c_d_id         INT8           NOT NULL,
    c_w_id         INT8           NOT NULL,
    c_first        VARCHAR(16)    NOT NULL,
    c_middle       CHAR(2)        NOT NULL,
    c_last         VARCHAR(16)    NOT NULL,
    c_street_1     VARCHAR(20)    NOT NULL,
    c_street_2     VARCHAR(20)    NOT NULL,
    c_city         VARCHAR(20)    NOT NULL,
    c_state        CHAR(2)        NOT NULL,
    c_zip          CHAR(9)        NOT NULL,
    c_phone        CHAR(16)       NOT NULL,
    c_since        TIMESTAMP      NOT NULL,
    c_credit       CHAR(2)        NOT NULL,
    c_credit_lim   DECIMAL(12, 2) NOT NULL,
    c_discount     DECIMAL(4, 4)  NOT NULL,
    c_balance      DECIMAL(12, 2) NOT NULL,
    c_ytd_payment  DECIMAL(12, 2) NOT NULL,
    c_payment_cnt  INT8           NOT NULL,
    c_delivery_cnt INT8           NOT NULL,
    c_data         VARCHAR(500)   NOT NULL,

    CONSTRAINT "primary" PRIMARY KEY (c_w_id, c_d_id, c_id)
);

CREATE INDEX customer_idx ON customer (c_w_id , c_d_id , c_last , c_first);
