-- domain/auction/BidListing.java — note the "version" column backing
-- the @Version optimistic lock that BidServiceImpl relies on for
-- concurrency safety during hot auctions.
CREATE TABLE bid_listings (
                              id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              crop_lot_id             BIGINT NOT NULL REFERENCES crop_lots (id),
                              reserve_price           NUMERIC(12, 2) NOT NULL,
                              current_highest_bid     NUMERIC(12, 2),
                              auction_open_time       TIMESTAMP WITH TIME ZONE NOT NULL,
                              auction_close_time      TIMESTAMP WITH TIME ZONE NOT NULL,
                              status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                              version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_bid_listings_status ON bid_listings (status);
CREATE INDEX idx_bid_listings_close_time ON bid_listings (auction_close_time);
CREATE INDEX idx_bid_listings_status_close_time ON bid_listings (status, auction_close_time);

-- domain/auction/Bid.java — the composite index here is what backs
-- BidRepository.findRecentBids()'s keyset pagination query
-- (WHERE listing_id = ? AND bid_timestamp < ? ORDER BY bid_timestamp DESC).
CREATE TABLE bids (
                      id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      listing_id          BIGINT NOT NULL REFERENCES bid_listings (id),
                      distributor_id      BIGINT NOT NULL REFERENCES distributor_profiles (id),
                      amount              NUMERIC(12, 2) NOT NULL,
                      bid_timestamp       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bid_listing_timestamp ON bids (listing_id, bid_timestamp);