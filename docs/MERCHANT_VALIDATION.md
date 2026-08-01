# Merchant Validation

## Goal

Choose one merchant path that can support a truthful end-to-end demo. Breadth is irrelevant.

## Candidate discovery sources

- Composio MCP Gateway
- UCP Checker
- E-commerce MCP directory
- Prava merchant spreadsheet

These are not guarantees.

## Validation checklist

For each merchant/path, verify:

| Capability | Evidence |
|---|---|
| product search/listing | observed live request and timestamp |
| stable product ID | observed response |
| current price/currency | observed response and timestamp |
| availability | observed or explicitly unknown |
| variant requirements | observed |
| delivery estimate | observed, or do not claim |
| quote/total | observed |
| checkout handoff | observed |
| Prava compatibility | complete test, not directory listing |
| return/result | observed |
| geographic limits | official response/docs |
| restricted product risk | reviewed |

## Selection score

Score 0–2:

- API reliability;
- product simplicity;
- checkout simplicity;
- sandbox compatibility;
- gift relevance;
- visual quality;
- delivery-data quality;
- support availability.

Pick the highest reliable option, not the most famous brand.

## Stored-value warning

Gift cards are visually recognizable but may introduce policy, merchant or checkout constraints. Do not commit to Microsoft/Xbox/Steam gift cards until the full flow is verified. The app concept may be updated to a physical gift without weakening WishTrace.

## Official-window evidence — 2026-08-01

| Merchant | Search | Exact variant | Price/stock | Delivery | Quote/checkout | Status |
|---|---|---|---|---|---|---|
| HyperX US | 10 live `gaming headset` results | Cloud III Black, `727A8AA` | $64.99 USD, available | unknown | unverified | primary; catalog pass |
| Turtle Beach USA | 5 live `gift card` results | own $50 digital card, `Gift-Card-50` | $50.00 USD, available | unknown | stored-value unverified | backup; card blocked |

- Every request supplied the public WishTrace UCP agent profile through `meta.ucp-agent.profile`.
- HyperX search request: `b0274ee3-4e99-4db4-8c7e-8df9bae7d9e0-1785598670`.
- Turtle Beach search request: `eeeebea9-ed97-4087-9ffd-dfa479a93886-1785598670`.
- HyperX lookup request: `5d190aa1-95a5-4fe9-b626-0ecb83f443d6-1785597811`.
- HyperX official `create_checkout` probe: request
  `448fec69-405e-4c5b-8642-9d2151f8a729-1785598905` returned MCP protocol error
  `Tool not found: create_checkout`. The advertised capability is not treated as a working tool.
- The public Cloudflare profile URL used for this proof is temporary test transport, not deployment
  or submission evidence.
- Both merchant profiles omitted the recommended cache header. The adapter records the deviation
  and does not cache those profiles.
- Exact normalized proof is stored in `artifacts/backend/ucp-live-proof-2026-08-01.json`.

Selection: HyperX remains primary because the physical-product path avoids stored-value policy risk.
Turtle Beach remains backup. Switch only if HyperX cannot produce a refreshed quote and browser
checkout attempt inside the 90-minute checkout gate.

## Fallback ladder

1. Primary live merchant.
2. Backup live merchant.
3. One fixed live SKU with refreshed price/availability when broad search is unstable.

If none works, stop at an honest unavailable state. There is no runtime catalog fixture fallback.

## Integration status template

```text
Merchant:
Mode: live
Search:
Product detail:
Price refresh:
Quote:
Checkout:
Prava session:
Final status:
Delivery claim supported:
Known failure:
Fallback trigger:
Last verified:
```

## Trigger to abandon a merchant

Switch when:

- no stable end-to-end path after 90 focused minutes;
- checkout requires unsupported identity or geography;
- price or SKU cannot be validated;
- callback/result cannot be reconciled;
- support uncertainty threatens the schedule.
