# 🔒 PRIVATE — Bug Hunt Answer Key — react-shopping-cart-bughunt — Cohort 01

> Injected on branch `seeded/cohort-01`, one commit per bug, commit message = `chore(SB-XX): ...`.
> Full injection diffs and repro steps: `docker/seeded-bugs-shopping-cart.md`.

## Seeded bugs

| ID | Category | Expected Severity | Difficulty | Location (file/component) | Injected commit |
|---|---|---|---|---|---|
| SB-01 | Functional | Critical | Easy | `src/contexts/cart-context/useCartTotal.ts` | 644d3d5 |
| SB-02 | Functional | Critical | Medium | `src/components/Products/Product/Product.tsx` | 5a5b38c |
| SB-03 | Functional | Major | Medium | `src/contexts/cart-context/useCartProducts.ts` (removeProduct) | 0bbe1c9 |
| SB-04 | Functional | Major | Easy | `src/contexts/cart-context/useCartProducts.ts` (removeProduct) | 86b40b5 |
| SB-05 | Validation | Major | Medium | `src/contexts/products-context/useProducts.tsx` | 60ad478 |
| SB-06 | Functional | Major | Medium | `src/contexts/cart-context/useCartProducts.ts` (addProduct) | f452c51 |
| SB-07 | Data | Major | Hard | `src/components/Cart/CartProducts/CartProduct/CartProduct.tsx` | 4bb2aa3 |
| SB-08 | UI/UX | Minor | Easy | `src/contexts/cart-context/useCartTotal.ts` | e81a22a |
| SB-09 | UI/UX | Minor | Hard | `src/components/Products/Product/Product.tsx` | dabf1e2 |
| SB-10 | UI/UX | Minor | Easy | `src/static/json/products.json` | 0213b1d |
| SB-11 | Content | Minor | Easy | `src/components/Filter/Filter.tsx` | 3330edd |
| SB-12 | Validation | Minor | Hard | (no injection — pre-existing gap, see below) | — |
| SB-13 | UI/UX | Trivial | Medium | `src/components/Cart/style.ts` | f31ab97 |
| SB-14 | Data | Trivial | Easy | `src/components/Products/Product/Product.tsx` | d471d7f |
| SB-15 | Functional | Major | Medium | `src/components/Cart/Cart.tsx` | 07b196a |

## Per-bug detail

### SB-01
- **Repro:** add any product with cents in its price (e.g. $10.90) to the cart.
- **Expected:** subtotal reflects the exact price (e.g. $10.90).
- **Actual:** subtotal is floored to the nearest dollar (e.g. $10).

### SB-02
- **Repro:** add "Cropped Stay Groovy off white" (sku 8552515751438644) to the cart.
- **Expected:** the cart line matches the product added.
- **Actual:** the added item's identity is swapped, breaking later quantity merge/removal for it.

### SB-03
- **Repro:** add 3+ different products, click remove on the 2nd or 3rd line.
- **Expected:** the clicked line is removed.
- **Actual:** a different line is removed (off-by-one index); removing the first item removes nothing.

### SB-04
- **Repro:** add 2 items, remove 1, look at the cart icon badge without reopening the cart.
- **Expected:** badge count decrements immediately.
- **Actual:** badge stays at the old count until another add/increase/decrease fires.

### SB-05
- **Repro:** check only size `XL` in the filter panel.
- **Expected:** only products whose available sizes include exactly `XL`.
- **Actual:** products with size `X` or `L` alone also appear (substring match).

### SB-06
- **Repro:** add the same product twice from the grid.
- **Expected:** one cart line with quantity 2.
- **Actual:** two separate lines, each quantity 1.

### SB-07
- **Repro:** add "Basic Cactus White T-shirt" (sku 39876704341265610) to the cart.
- **Expected:** cart line price matches the $13.25 grid price.
- **Actual:** cart line shows $11.93 (10% off), but the subtotal/checkout total still uses $13.25.

### SB-08
- **Repro:** open the cart with any item in it.
- **Expected:** subtotal shows `$`, matching the grid.
- **Actual:** subtotal shows `€`.

### SB-09
- **Repro:** cross-reference the "Free shipping" badge on grid cards against `isFreeShipping` in `products.json`.
- **Expected:** badge shown only when `isFreeShipping: true`.
- **Actual:** badge logic is inverted.

### SB-10
- **Repro:** open "Skater Black Sweatshirt" (id 4).
- **Expected:** its own product photos.
- **Actual:** shows "Black Tule Oversized"'s photos in both grid and cart; title/price/cart behavior unaffected.

### SB-11
- **Repro:** look at the filter panel heading.
- **Expected:** "Sizes:".
- **Actual:** "Szies:".

### SB-12
- **Repro:** filter to a size combination with zero matching products.
- **Expected:** a "no products found" empty state.
- **Actual:** grid is empty, header just reads "0 Product(s) found" — no dedicated empty state.
- **Note:** this is a real, pre-existing gap in the unmodified app — no commit injected it. Still a valid, scoreable find.

### SB-13
- **Repro:** load the app at the default (mobile-first, <768px) viewport.
- **Expected:** cart toggle button fully visible.
- **Actual:** clipped at the top edge.

### SB-14
- **Repro:** view any product with installments (e.g. "or 10 x $X").
- **Expected:** the shown count matches what was used to compute $X.
- **Actual:** label shows one more installment than was used (`installments + 1`), so the math doesn't reconcile.

### SB-15
- **Repro:** set a product's cart quantity to 2+, note the on-screen subtotal, then click Checkout.
- **Expected:** the alert's subtotal matches the on-screen subtotal.
- **Actual:** the alert recomputes ignoring quantity (each line counted once), giving a lower total.

## Scoring sheet — <learner name>

| Learner ticket | Matched seeded ID | Valid? | Severity match? | Report quality (0–3) | Notes |
|---|---|---|---|---|---|

**Scoring formula** (from `qa-curriculum-map.md` §Phase 1)
- Detection rate = matched seeded bugs / 15 → 35%
- Report quality = avg(quality 0–3)/3 → 30%
- Severity accuracy = severity matches / valid tickets → 15%
- Cleanliness = 10% minus deductions (each duplicate −2%, each invalid −2%, floor 0)
- Session notes quality (charters followed, notes usable) 0–3 → 10%

**Escapes & extras**
- Seeded bugs found by nobody → review difficulty rating for next cohort.
- Real (non-seeded) bugs found → bonus +2% each, cap +6%.
