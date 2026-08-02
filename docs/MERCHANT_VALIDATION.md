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
| Jackbox Games | official UCP profile + product/cart | $5 card, `GC20221246` | $5.00 USD, available | purchaser email then manual forward; timing unknown | live cart and card form; payment pending | primary; stored-value gated |
| HyperX US | 10 live `gaming headset` results | Cloud III Black, `727A8AA` | $64.99 USD, available | unknown | live checkout form, shipping address required | retired for this user |
| Turtle Beach USA | 5 live `gift card` results | own $50 digital card, `Gift-Card-50` | $50.00 USD, available | digital email | live card form; payment pending | rejected: $50 minimum |

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
- Jackbox UCP profile: `https://checkout.jackboxgames.com/.well-known/ucp`, version
  `2026-04-08`, MCP endpoint `jackbox-games.myshopify.com/api/ucp/mcp`.
- Jackbox product `gid://shopify/Product/6734381809798`, variant
  `gid://shopify/ProductVariant/39783705149574`, SKU `GC20221246`.
- A live one-item cart returned `$5.00`, `gift_card=true`, and `requires_shipping=false`.
  Checkout rendered the exact $5 total, contact, card and billing fields without shipping.
- The API's production `JackboxPlaywrightCheckoutGateway` then passed a second live, non-payment
  quote with synthetic US billing: item 500, shipping 0, tax 0 and total 500 USD minor units. Its
  Windows Proactor worker runs beside the psycopg Selector loop; no payment button was clicked.
- Jackbox says the purchaser receives the card by email and forwards it to the recipient. It can be
  used in the Jackbox shop for merchandise or Steam codes; it is not a Steam/Xbox/Amazon wallet card,
  and its region restriction remains material.
- No payment data was entered and no order was created. Evidence:
  `artifacts/backend/jackbox-digital-checkout-probe-2026-08-01.png` and
  `artifacts/backend/jackbox-runtime-quote-2026-08-01.json`.

Selection: Jackbox supersedes HyperX because it satisfies the user's no-shipping constraint, matches
Zaid's gaming interest, and provides the exact $5 acceptance target. It remains disabled at runtime
until Prava confirms stored-value eligibility. If Prava disallows it or the supported geography does
not fit the real cardholder/recipient, stop and select another observed digital SKU; do not fall back
to an invented or uncontrolled card.

## Exact digital expansion — 2026-08-03

| Product | Product ID | Variant ID | Live cart | Runtime status |
|---|---|---|---|---|
| Jackbox Games Gift Card — $5 | `6734381809798` | `39783705149574` | $5.00; digital; no shipping | eligible at $5+ |
| Quiplash 2 InterLASHional | `6882537799814` | `40190131404934` | $9.99; digital; no shipping | eligible at $10+ |
| Drawful 2 | `2549185675344` | `21892043538512` | $9.99; digital; no shipping | eligible at $10+ |
| Quiplash | `2549174173776` | `21891973906512` | $9.99; digital; no shipping | eligible at $10+ |

The live catalog returned additional products, but they remain discovery-only and deterministically
`UNSUPPORTED_CHECKOUT` until their exact product/variant cart behavior is proven. Merchant-wide UCP
checkout advertising is not treated as product-level proof. Amazon Incentives remains gated behind
partner onboarding and a prefunded account; Steam and Microsoft digital gifts remain account/region
dependent and have no verified WishTrace checkout adapter. They are not runtime options.

For repeat discovery, a product that was either the prior primary ranked recommendation or selected
into a mandate recedes only when another independently eligible live product exists. This prevents
back/re-enter loops from repeatedly leading with the same gift without randomizing rankings or
inventing catalog breadth. When all verified products have been seen, the best valid product may
reappear honestly.

## Fallback ladder

1. One fixed Jackbox $5 live SKU with refreshed price and no-shipping checkout.
2. Another support-approved, observed low-value digital SKU.
3. Honest unavailable state.

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
