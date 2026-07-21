E3. The 15-bug injection catalog

Inject on the seeded/cohort-01 branch, one commit per bug, commit message = the bug's ID only (e.g., SB-03) — this makes your answer key auditable and lets you cleanly rebuild or re-seed.

| ID    | Category   | Severity | Injection |
|--------|------------|----------|-----------|
| SB-01 | Functional | Critical | Cart total: apply price rounding down to integer (e.g., `Math.floor`) so totals are wrong for any non-round price. |
| SB-02 | Functional | Critical | "Add to cart" on one specific product adds a different product (swap an ID). |
| SB-03 | Functional | Major | Removing an item from the cart removes the wrong item (off-by-one index). |
| SB-04 | Functional | Major | Cart badge doesn't decrement on remove (only increments). |
| SB-05 | Validation | Major | Size filter: selecting `XL` also returns `L` items (substring match instead of exact). |
| SB-06 | Functional | Major | Adding the same product twice creates two separate line items instead of increasing the quantity to 2. |
| SB-07 | Data | Major | One product's price displays differently in the product grid versus the cart (stale hardcoded price in one view). |
| SB-08 | UI/UX | Minor | Currency symbol is incorrect on the cart subtotal only (`€` instead of `$`). |
| SB-09 | UI/UX | Minor | "Free shipping" tag logic is inverted—it appears on products that do not qualify. |
| SB-10 | UI/UX | Minor | One product image points to a different product's image. |
| SB-11 | Content | Minor | Typo in a visible heading (e.g., **"Prodcuts"** instead of **"Products"**). |
| SB-12 | Validation | Minor | Filters can return zero results, but no "No products found" empty-state message is displayed. |
| SB-13 | UI/UX | Trivial | Cart drawer close (`X`) button is misaligned or partially off-screen at the default viewport size. |
| SB-14 | Content | Trivial | Footer year is hardcoded to an outdated year. |
| SB-15 | Functional | Major | Checkout subtotal ignores product quantity and charges each line item as if the quantity were `1`. |