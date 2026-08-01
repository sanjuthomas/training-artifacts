### Functional questions
* Who is the sending party? Is this feed coming from a custodian (e.g. Apex) or from another wealth manager/partner platform?

* Does the master data already exist on our side? Can I assume that the client (party), sub-accounts, and advisor already exist in Firm’s CRM / account master, and that this feed is enriching or updating holdings rather than creating those entities from scratch?

* Is this a full (master) snapshot or a delta feed? It looks like a full snapshot—there’s no holding lifecycle (adds/updates/deletes)—but I’d like to confirm. That choice drives upsert and deletion semantics.

* What’s the intended use of market_value and price in the feed? By the time we persist the data, markets may have moved. If this is for EOD/EOW reporting or a point-in-time snapshot, that makes sense—but I want to confirm the business purpose, so we model and freshness expectations correctly. Should we treat these as as-of last_updated, and store that timestamp with the snapshot?

### Technical questions
* What does the webhook payload look like exactly? Is it only a URL, or does it also include auth material, a checksum, batch ID, timestamp, etc.?

* How do we verify file integrity? Ideally we’d have a checksum for the ZIP, and preferably per-file integrity as well (e.g. a checksum in each file header, plus a separate checksum manifest for the ZIP). Do you already have a preferred approach? I’d prefer checksums for the ZIP and ideally per file—happy to align with whatever the partner already provides.

* Is there an SLO for processing latency? From webhook receipt to data being queryable, how long is acceptable?

* What’s the holdings cardinality? We’ve discussed ~1,000 clients and ~2 accounts each—can you share average and max holdings per account?

### Assumptions (unless you tell me otherwise)
* One JSON file ↔ one client (party).
* Party ↔ advisor is 1:1 in this feed—the payload has a single advisor_id and no advisor attributes beyond the ID.
* Instrument identity is imperfect over time. Tickers can change (e.g. ETF/mutual fund rebrands), so a proper design would lean on a security master (CUSIP, etc.). I’m not building a full security-master system for this exercise, but I’ll key holdings in a way that doesn’t assume ticker is immutable.
* Design for growth. The firm is scaling; exact growth may be hard to predict, so I’ll bias the design toward a growing client base (async processing, idempotent upserts, clear failure isolation) rather than optimizing only for the stated ~1k-client size.
