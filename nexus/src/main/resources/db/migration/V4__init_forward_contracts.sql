-- domain/contract/ForwardContract.java — the UNIQUE constraint on
-- listing_id is the actual atomicity guarantee behind
-- BidListingServiceImpl.convertToContract(): it is structurally
-- impossible for one listing to spawn two competing contracts,
-- enforced by the database, not just application-level checks.
CREATE TABLE forward_contracts (
                                   id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   listing_id              BIGINT NOT NULL UNIQUE REFERENCES bid_listings (id),
                                   locked_price            NUMERIC(12, 2) NOT NULL,
                                   delivery_deadline       DATE NOT NULL,
                                   status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- domain/contract/Order.java — same one-to-one-via-unique-constraint
-- pattern as forward_contracts.listing_id above, for the same reason:
-- ForwardContractServiceImpl.createOrder() must never be able to spawn
-- two orders for one contract even under concurrent invocation.
CREATE TABLE orders (
                        id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        contract_id     BIGINT NOT NULL UNIQUE REFERENCES forward_contracts (id),
                        created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- domain/contract/OrderFulfillment.java — the One-to-Many child of
-- Order reflecting that bulk agri-procurement is delivered in
-- tranches, not a single atomic shipment.
CREATE TABLE order_fulfillments (
                                    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    order_id                BIGINT NOT NULL REFERENCES orders (id),
                                    tranche_quantity_kg     NUMERIC(12, 2) NOT NULL,
                                    delivered_at            TIMESTAMP WITH TIME ZONE,
                                    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_order_fulfillments_order_id ON order_fulfillments (order_id);
CREATE INDEX idx_order_fulfillments_status ON order_fulfillments (status);