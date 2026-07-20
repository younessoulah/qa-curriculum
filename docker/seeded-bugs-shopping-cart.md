⚠️ **AUTHOR-ONLY — DO NOT DISTRIBUTE TO LEARNERS.** This file names exact files, lines, and diffs. Anyone with read access to this repo (including a fork learners clone) can read the answers. See §0 before seeding.

# E3. The 15-bug injection catalog — react-shopping-cart-bughunt

> Revised 2026-07-16: verified every entry against the actual vendored code at
> `docker/react-shopping-cart-bughunt` (fork of jeffersonRibeiro/react-shopping-cart).
> The original catalog described some bugs generically; several didn't match how this
> codebase actually implements that behavior (e.g. remove-by-id already works correctly,
> there is no footer component). Locations, exact diffs, and two swapped bugs are below.

## 0. Before you seed — repo hygiene

This file currently lives in the **public, learner-facing repo** (`docker/seeded-bugs-shopping-cart.md`,
committed in `fa07efa`) alongside the app the learners will clone/fork. Once you inject
these bugs, this file is a full answer key sitting one directory above the buggy app.
Recommend one of:
- Move this file (and `docs/templates/bug-hunt-answer-key.md` once filled in) to a
  private repo before publishing the learner-facing fork, **or**
- Keep seeding on a branch (`seeded/cohort-XX`) that is squash-exported to a clean
  learner repo without history/docs, never merged back to `main` with this file present.

Inject on branch `seeded/cohort-01`, one commit per bug, commit message = bug ID only
(e.g. `SB-03`). This keeps the answer key auditable and lets you re-seed a fresh cohort
branch by cherry-picking a subset.

## 1. Catalog

| ID | Category | Severity | Difficulty | Summary |
|---|---|---|---|---|
| SB-01 | Functional | Critical | Easy | Cart total rounds down, losing cents |
| SB-02 | Functional | Critical | Medium | "Add to cart" adds the wrong product for one SKU |
| SB-03 | Functional | Major | Medium | Removing an item removes the wrong line |
| SB-04 | Functional | Major | Easy | Cart badge doesn't decrement on remove |
| SB-05 | Validation | Major | Medium | Size filter substring-matches instead of exact-matches |
| SB-06 | Functional | Major | Medium | Adding the same product twice creates two line items instead of merging quantity |
| SB-07 | Data | Major | Hard | One product's cart-line price silently differs from its grid price |
| SB-08 | UI/UX | Minor | Easy | Cart subtotal shows `€` instead of `$` regardless of currency |
| SB-09 | UI/UX | Minor | Hard | "Free shipping" tag logic is inverted |
| SB-10 | UI/UX | Minor | Easy | One product's images are actually another product's images |
| SB-11 | Content | Minor | Easy | Typo in the filter panel heading |
| SB-12 | Validation | Minor | Hard | Zero-result filter shows no "no products found" empty state |
| SB-13 | UI/UX | Trivial | Medium | Cart toggle button is clipped/misaligned at default viewport |
| SB-14 | Data | Trivial | Easy | Installment count shown to the shopper doesn't match the one used to compute the per-installment price |
| SB-15 | Functional | Major | Medium | Checkout alert total ignores quantity, unlike the on-screen subtotal |

> **Changed from the original draft:** SB-12 needs no code change (see below — it's a
> real, pre-existing gap in the clean app, use it as a free "already there" bug). SB-14
> was rewritten — the original ("footer year hardcoded") doesn't apply because this app
> has no footer component at all.

## 2. Per-bug injection detail

### SB-01 — Cart total rounds down
**File:** `src/contexts/cart-context/useCartTotal.ts`, inside `updateCartTotal`, the `totalPrice` reduce (~L16-19).
```diff
   const totalPrice = products.reduce((sum: number, product: ICartProduct) => {
-    sum += product.price * product.quantity;
+    sum += Math.floor(product.price * product.quantity);
     return sum;
   }, 0);
```
**Repro:** add any product priced with cents (e.g. $10.90) to the cart → subtotal shows $10 instead of $10.90.

### SB-02 — "Add to cart" adds the wrong product
**File:** `src/components/Products/Product/Product.tsx`, `handleAddProduct` (~L43-46).
Pick one SKU (e.g. the first product in `products.json`) and swap its `id` on add:
```diff
   const handleAddProduct = () => {
-    addProduct({ ...product, quantity: 1 });
+    addProduct({
+      ...product,
+      id: product.sku === 8552515751438644 ? 999 : product.id,
+      quantity: 1,
+    });
     openCart();
   };
```
**Repro:** add "Cropped Stay Groovy off white" → cart identity no longer matches the grid product (quantity merging/removal for it breaks too, which is a nice secondary symptom for the learner to notice).

### SB-03 — Removing an item removes the wrong line
**File:** `src/contexts/cart-context/useCartProducts.ts`, `removeProduct` (~L42-45). Today it correctly filters by `id`; break it into an index-based removal that's off by one:
```diff
   const removeProduct = (productToRemove: ICartProduct) => {
-    const updatedProducts = products.filter(
-      (product: ICartProduct) => product.id !== productToRemove.id
-    );
+    const targetIndex = products.indexOf(productToRemove) - 1;
+    const updatedProducts = products.filter(
+      (_product: ICartProduct, index: number) => index !== targetIndex
+    );
```
**Repro:** with 3+ items in the cart, click remove on the 2nd or 3rd line — a different line disappears. Removing the very first item removes nothing (index -1 matches no item), which is itself a good secondary symptom.

### SB-04 — Cart badge doesn't decrement on remove
**File:** same function, one line down (~L47-48). Drop the total recompute on remove only:
```diff
   const removeProduct = (productToRemove: ICartProduct) => {
     ...
     setProducts(updatedProducts);
-    updateCartTotal(updatedProducts);
   };
```
**Repro:** add 2 items, remove 1 — the cart icon badge still shows the old count until the cart is reopened or another add/increase/decrease action fires.

### SB-05 — Size filter substring-matches
**File:** `src/contexts/products-context/useProducts.tsx`, `filterProducts` (~L33-37).
```diff
   filteredProducts = products.filter((p: IProduct) =>
     filters.find((filter: string) =>
-      p.availableSizes.find((size: string) => size === filter)
+      p.availableSizes.find((size: string) => filter.includes(size))
     )
   );
```
**Repro:** check only `XL` in the filter panel → products whose only available size is `X` or `L` (not `XL`) also appear, because `"XL".includes("X")` and `"XL".includes("L")` are both true.

### SB-06 — Duplicate add creates two line items
**File:** `src/contexts/cart-context/useCartProducts.ts`, `addProduct`'s `isProductAlreadyInCart` check (~L26-28). Compare a field that will never realistically match instead of `id`:
```diff
   const isProductAlreadyInCart = products.some(
-    (product: ICartProduct) => newProduct.id === product.id
+    (product: ICartProduct) => newProduct.sku === product.id
   );
```
**Repro:** add the same product twice from the grid → two separate lines in the cart instead of one line with quantity 2 (since `sku` is a large number that never equals an `id`, `isProductAlreadyInCart` is always false).

### SB-07 — Cart-line price silently differs from grid price
**File:** `src/components/Cart/CartProducts/CartProduct/CartProduct.tsx`, the price display (~L46-47). Hardcode a stale price for one SKU while the real `total.totalPrice` (used at checkout) still uses the correct price:
```diff
   <S.Price>
-    <p>{`${currencyFormat}  ${formatPrice(price, currencyId)}`}</p>
+    <p>{`${currencyFormat}  ${formatPrice(
+      sku === 39876704341265610 ? price * 0.9 : price,
+      currencyId
+    )}`}</p>
```
**Repro:** add "Basic Cactus White T-shirt" — grid shows $13.25, cart line shows $11.93, but the subtotal at the bottom of the cart (and the checkout alert) is still computed from the real $13.25. High-value find: proves the display bug doesn't touch the total, which is the subtle part.

### SB-08 — Cart subtotal shows the wrong currency symbol
**File:** `src/contexts/cart-context/useCartTotal.ts`, the `total` object (~L34-36).
```diff
   const total = {
     productQuantity,
     installments,
     totalPrice,
     currencyId: 'USD',
-    currencyFormat: '$',
+    currencyFormat: '€',
   };
```
**Repro:** grid prices show `$`, cart subtotal shows `€` — nothing else in the cart or checkout alert changes, since they all read `total.currencyFormat`.

### SB-09 — "Free shipping" tag is inverted
**File:** `src/components/Products/Product/Product.tsx` (~L57).
```diff
-  {isFreeShipping && <S.Stopper>Free shipping</S.Stopper>}
+  {!isFreeShipping && <S.Stopper>Free shipping</S.Stopper>}
```
**Repro:** cross-reference `src/static/json/products.json` `isFreeShipping` values against which grid cards show the badge — they're all backwards. Rated Hard because it requires the learner to check the data, not just eyeball the UI.

### SB-10 — One product's images belong to another product
**File:** `src/static/json/products.json`. Images resolve from the `sku` field (`static/products/${sku}-1-product.webp` etc. in `Product/style.ts` and `CartProduct.tsx`), while cart identity uses `id`. Change only the `sku` value of one product entry to another existing product's `sku`, leaving `id`/`title`/`price` untouched, so the images swap but everything else (cart merge/remove logic) behaves normally.
**Repro:** open the product whose `sku` was changed — it shows a different product's photos in both the grid and cart, but its title/price/cart behavior are correct (isolates this as a pure asset-mapping bug).

### SB-11 — Typo in the filter panel heading
**File:** `src/components/Filter/Filter.tsx` (~L32).
```diff
-  <S.Title>Sizes:</S.Title>
+  <S.Title>Szies:</S.Title>
```

### SB-12 — Zero-result filter shows no empty state (no code change needed)
Verified in `src/components/App/App.tsx` (~L33) and `src/components/Products/Products.tsx`:
filtering to a size no product has leaves the grid empty and the header line just reads
`"0 Product(s) found"` — there is no dedicated empty-state message/illustration. **This
is already true of the clean, unmodified app** — no injection required. List it in the
answer key as-is; it's a legitimate, naturally-occurring finding and a good check that
learners are testing edge cases, not just the seeded diffs.

### SB-13 — Cart toggle button clipped/misaligned at default viewport
**File:** `src/components/Cart/style.ts`, `CartButton` (~L10).
```diff
   position: absolute;
-  top: 0;
+  top: -6px;
   left: 0;
```
**Repro:** at the default (mobile-first, <768px) viewport the cart toggle button is visibly clipped at the top edge; resizing to tablet width doesn't fix it since the offset is on the button itself, not a breakpoint-only rule.

### SB-14 — Installment count shown doesn't match the one used to price it
**File:** `src/components/Products/Product/Product.tsx`, the installment label (~L32-34).
```diff
   <S.Installment>
-    <span>or {installments} x</span>
+    <span>or {installments + 1} x</span>
     <b>
       {currencyFormat}
       {formatPrice(installmentPrice, currencyId)}
     </b>
   </S.Installment>
```
**Repro:** any product with installments — the label says "or 10 x $X" but `installmentPrice` (`price / installments`) was computed with 9, so `10 × $X ≠ price`. Requires the learner to actually do the multiplication, good for a "Data/Trivial" category since nothing looks visually wrong.

### SB-15 — Checkout alert total ignores quantity
**File:** `src/components/Cart/Cart.tsx`, `handleCheckout` (~L11-22). Recompute the alert's total from `products` directly instead of reusing the already-correct `total.totalPrice`:
```diff
   const handleCheckout = () => {
     if (total.productQuantity) {
+      const subtotalIgnoringQty = products.reduce(
+        (sum, product) => sum + product.price,
+        0
+      );
       alert(
         `Checkout - Subtotal: ${total.currencyFormat} ${formatPrice(
-          total.totalPrice,
+          subtotalIgnoringQty,
           total.currencyId
         )}`
       );
```
(`products` is already destructured from `useCart()` at the top of the component — no new import needed.)
**Repro:** set any product's quantity to 2+ in the cart — the on-screen subtotal is correct, but clicking "Checkout" pops an alert with a lower total that only counts each distinct line once.
