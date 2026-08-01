### Functional questions
* What is the purpose of this feed? Is this an OLTP/transactional or OLAP/reporting use case? 
* Who is the sending party? Is this feed coming from a custodian (e.g. Apex) or from another wealth manager/partner platform?
* We've client_id, first_name, and last_name on the feed, so we can create the party if they don't exist in our DB. In that case, I assume the source of truth is not our system and client_id is issued by the second party, and we just keep a few party attributes. Same for accounts, too.
* Is this a full (master) snapshot or a delta feed? It looks like a full snapshot; there’s no holding lifecycle (adds/updates/deletes), but I’d like to confirm. That choice drives upsert and deletion semantics.
* What’s the intended use of market_value and price in the feed? By the time we persist the data, markets may have moved. If this is for EOD reporting or a point-in-time snapshot, that makes sense, but I want to confirm the business purpose, so we model and freshness expectations correctly.

### Technical questions
* What does the webhook payload look like exactly? Is it only a URL, or does it also include auth material, a checksum, timestamp, etc.?
* How do we verify file integrity? Ideally we’d have a checksum for the ZIP, and preferably per-file integrity as well, but I haven't seen any header or checksum in the JSON file, so my solution will just cover the ZIP file integrity alone.
* Is there an SLO for processing latency? From webhook receipt to data being queryable, how long is acceptable?
* What’s the holdings cardinality? We’ve discussed ~1,000 clients and ~2 accounts each: can you share average and max holdings per account?

### Privacy/security questions
PII in the feed: The payload includes three PII fields: first_name, last_name, and email. Do these need to be encrypted at rest in our database? If the answer is yes, then we need to come up with an encryption/decryption-as-a-service model. That also means we will need a secret store to safely store keys. 
For the local services I build, I will secure them using Keycloak and OAuth 2.0 authorization code flow. 

### Assumptions (unless you tell me otherwise)
* One JSON file ↔ one client (party).
* Webhook will be invoked only after the file is completely written into the file system/object store.
* Party ↔ advisor is 1:1 in this feed; the payload has a single advisor_id and no advisor attributes beyond the ID.
* Instrument identity is imperfect over time. Tickers can change (e.g. ETF/mutual fund rebrands), so a proper design would lean on a security master (CUSIP, etc.). I’m not building a full security-master system for this exercise, but I’ll key holdings in a way that doesn’t assume the ticker is immutable.
* Design for growth. The firm is scaling; exact growth may be hard to predict, so I’ll bias the design toward a growing client base (async processing, idempotent upserts, clear failure isolation) rather than optimizing only for the stated ~1k-client size.
