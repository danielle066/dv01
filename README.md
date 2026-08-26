# Loan Stats

DV01 take-home. Play 2.8 / Scala 2.12.

Reads Lending Club loan-level CSV data and returns aggregations over an API — group by grade, state, issue date, or FICO band, with optional filters on each.

## Setup and run

**Requirements:** JDK 11+, sbt

```bash
sbt run
```

Open http://localhost:9000 — there's a small page to try different slices.

Out of the box it loads `data/sample.csv` (12 rows) so you don't need the full file to get started. For the 2017Q4 tape, put `LoanStats_securev1_2017Q4.csv` in `data/` and run:

```powershell
# Windows
$env:LOAN_CSV="data/LoanStats_securev1_2017Q4.csv"
sbt run
```

```bash
# mac / linux
LOAN_CSV=data/LoanStats_securev1_2017Q4.csv sbt run
```

## Tests

```bash
sbt test
```

Covers the CSV parser, aggregation logic, and API endpoints. Uses `test/resources/loans-fixture.csv` (same shape as the real file, 12 rows).

## API

| Endpoint | Purpose |
|---|---|
| `GET /api/summary` | Group and filter loans |
| `GET /api/meta` | Distinct values for the UI dropdowns |

**`/api/summary` params**

| Param | Values |
|---|---|
| `groupBy` | `grade` (default), `state`, `date`, `fico` |
| `state` | e.g. `CA` |
| `grade` | `A`–`G` |
| `date` / `issueMonth` | `Dec-2017` or `2017-12` |
| `ficoBand` | `660-679`, `680-699`, … `800+` |

Filters stack with AND. Response has totals plus one row per bucket (loan count, balances, weighted rate/FICO).

```bash
curl "http://localhost:9000/api/summary?groupBy=grade&state=CA"
curl "http://localhost:9000/api/meta"
```

## Architecture

CSV is parsed once at startup and kept in memory. Each API call filters and groups that list.

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│  CSV file   │────▶│  LoanLoader  │────▶│ LoanRepository  │
└─────────────┘     └──────────────┘     └────────┬────────┘
                                                   │
┌─────────────┐     ┌──────────────┐              │
│  index.html │────▶│ ApiController│────▶ LoanService (filter + group)
└─────────────┘     └──────────────┘
```

**Main pieces**

| File | Role |
|---|---|
| `LoanLoader` | Parse Lending Club CSV (skip notes/total rows) |
| `LoanRepository` | Hold the tape in memory after startup |
| `LoanService` | Filter + aggregate by grade/state/date/FICO |
| `ApiController` | `/api/summary`, `/api/meta` |
| `Models.scala` | `Loan`, filters, FICO bands |

## Trade-offs

This was a ~4 hour take-home, so I focused on the API contract and tests over polish.

- **No database** — one quarter (~119k rows) is fine in memory. Multiple tapes or concurrent writes would need Postgres.
- **Load once, not per request** — parsing the CSV on every call would be slow for no real benefit here.
- **Play/Scala skeleton** — used the challenge starter instead of switching stacks. Same shape would work in Kotlin.
- **Money as `Double`** — ok for chart totals, not for production cashflows.
- **Rates in percent** — matches the source file (6.08, not 0.0608). Weighted averages use original balance.
- **Simple HTML UI** — enough to click through slices, not a product frontend.

Full CSV is not in git (~100MB). `data/sample.csv` and the test fixture are enough to run and verify.
