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
