## Functional Questions

| # | Question | Sanju's Assumption | Bob's comment |
|---|---|---|---|
| 1 | What is the purpose of this feed? Is it an OLTP/transactional use case or an OLAP/reporting use case? | Firm is neither a custodian nor a broker-dealer, so this is treated as an **opening-positions feed from the custodian**. The timestamp `"last_updated": "2025-03-02T14:30:00Z"` (~9:30 AM ET) supports that reading: it looks like a morning settled-positions drop, not continuous trade capture. Even though we insert into a normalized transactional store, the primary use is positions and dashboards. Each morning, settled positions arrive from the custodian; per-account summaries and portfolio-page positions should match the custodian. For this solution, the feed is the **source of truth**. | TODO |
| 2 | The feed includes `client_id`, `first_name`, and `last_name`. In production, clients, accounts, and advisors would typically be onboarded into Firm first, with custodian client IDs and account numbers mapped to Firm IDs. For this interview solution, should we create missing entities from the feed instead? | For this interview solution, treat the feed as the source of truth and **create** client, account, holding, security, and advisor rows when they are not already in the database, rather than requiring a separate onboarding or id-mapping step. | TODO |
| 3 | Is this a full (master) snapshot or a delta feed? It appears to be a full snapshot. | Treat it as the custodian’s **master opening-balance feed** (full snapshot), not a delta. | TODO |
| 4 | What is the intended use of `market_value` and `price` in the feed? | Because this is treated as an opening-balance feed, `price` is assumed to be the **prior trading day’s closing price**. Store `market_value` and `price` as provided so we can calculate portfolio change over time. | TODO |
| 5 | What does account status mean ("status": "ACTIVE")? Are there other statuses? | I have improvised and see the details below. | TODO |

### Operational and Verification Statuses
| Canonical status | Maps from (examples) | Holdings |
|------------------|----------------------|----------|
| `ACTIVE` | Active | Snapshot as today |
| `PENDING` | Pending / Under Review | Keep (follow feed list) |
| `UPDATING` | Account Updated | Keep |
| `INACTIVE` | Inactive / Restricted | Keep |
| `SUSPENDED` | Suspended | Keep — book still owned; just can’t trade/login |
| `CLOSED` | Closed | **No current holdings** — close all on load |

## Technical questions

| # | Question | Sanju's Solution | Bob's comment |
|---|---|---|---|
| 1 | What does the webhook payload look like exactly? Is it only a URL, or does it also include auth material, a checksum, timestamp, etc.? | The webhook is more than a URL. `POST /webhooks/v1/feeds/client-portfolio` requires a **Bearer JWT**. The JSON body includes `event_id`, `event_type` (`feed.arrived`), `occurred_at`, `source`, and a `feed` object with `absolute_path` (landing zip path), `checksum` (hex), and `checksum_algorithm` (`SHA-256`). Auth is in the header, not the body. ingest-api recomputes the checksum against the file at `absolute_path` before writing `ops.webhook_event`; mismatch or missing file rejects without persisting. | TODO |
| 2 | How do we verify file integrity? | The producer (harness) computes a **SHA-256** hex digest of the landing zip and sends it in the webhook as `feed.checksum` with `feed.checksum_algorithm = "SHA-256"`. ingest-api reads the file at `absolute_path`, recomputes the digest with the declared algorithm, and compares. **Match** → persist to `ops.webhook_event` (`ACCEPTED`) and return `202`. **Mismatch** → `422 rejected`, no DB write. **Missing/unreadable file** → `400 rejected`. Unsupported algorithms fail validation (`400`) with the accepted list. Integrity is checked **before** any ops persistence. | TODO |
| 3 | Is there an SLO for processing latency? | Yes. I created one: feeds of **1,000 clients or fewer**, end-to-end processing (feed arrived → data available for query) must complete in **under 30 seconds for 99% of occasions**, measured over a **rolling 30-day window**. | TODO |
| 4 | What is the holdings cardinality? | I came up with this number: At least **1** holding per account/client context in a valid feed file; worst case up to **1,000** holdings. | TODO |

## Privacy/Security Questions

| # | Question | Sanju's Assumption | Bob's comment |
|---|---|---|---|
| 1 | The feed payload includes PII fields (`first_name`, `last_name`, and `email`). Should we handle those with care? | Yes. In production, those fields should be **encrypted at rest**, with controlled access to encryption/decryption keys. For this interview solution, I am **not** implementing field-level encryption, but I am calling out that these PII fields were identified. | TODO |
| 2 | What are the service-level security requirements? | No formal requirements were given. For the local services in this solution, access is secured with **Keycloak** and **OAuth 2.0** (Authorization Code + PKCE for UIs; JWT bearer tokens for APIs). | TODO |

### Assumptions (unless you tell me otherwise)
* One JSON file ↔ one client (party).
* Webhook will be invoked only after the file is completely written into the file system/object store.
* Party ↔ advisor is 1:1 in this feed; the payload has a single advisor_id and no advisor attributes beyond the ID.
* Instrument identity is imperfect over time. Tickers can change (e.g. ETF/mutual fund rebrands), so a proper design would lean on a security master (CUSIP, etc.). I’m not building a full security-master system for this exercise, but I’ll key holdings in a way that doesn’t assume the ticker is immutable.
* Design for growth. The firm is scaling; exact growth may be hard to predict, so I’ll bias the design toward a growing client base (async processing, idempotent upserts, clear failure isolation) rather than optimizing only for the stated ~1k-client size.
