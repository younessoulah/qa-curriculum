# Local bug-hunt apps

Two apps live here, both meant to run fully locally via Docker — no shared hosted
instance, no dependency on a third-party demo site staying up or keeping its data
consistent. Each learner runs their own copy on their own machine.

- `react-shopping-cart-bughunt/` — a small React/TypeScript shopping cart.
- `restful-booker-platform/` — a multi-service Bed & Breakfast booking platform
  (auth, booking, room, message, report, branding + a Next.js frontend), each
  service a separate Spring Boot app.

## Prerequisites

- Docker (with Compose v2, i.e. `docker compose`, not the standalone `docker-compose`)
- JDK 26+, Maven 3.9+, Node 20+, npm — only needed to *build* `restful-booker-platform`'s
  jars before the first `docker compose up` (see below); not needed to run
  `react-shopping-cart-bughunt`, which builds entirely inside its own Dockerfile.

## First-time setup

`restful-booker-platform`'s Dockerfiles each expect a pre-built `target/*-exec.jar` —
they don't run Maven themselves. Build the jars once locally first:

```sh
cd docker/restful-booker-platform
mvn clean install
cd ../..
```

> On a `seeded/*` branch, some of this project's own unit/integration tests are
> **expected to fail** here — that's not a build break, it's the seeded bug doing its
> job (the test still asserts the *original* correct behavior). `seeded/*` branches
> switch this command to `mvn clean install -DskipTests` for exactly that reason; on
> `main` the tests should stay green.

## Running everything

```sh
docker compose -f docker/docker-compose.yml up --build
```

| App | URL | Notes |
|---|---|---|
| restful-booker-platform frontend | http://localhost:80 | |
| restful-booker-platform admin login | — | username `admin`, password `password` |
| restful-booker-platform booking API | http://localhost:3000 | |
| restful-booker-platform room API | http://localhost:3001 | |
| restful-booker-platform branding API | http://localhost:3002 | |
| restful-booker-platform auth API | http://localhost:3004 | |
| restful-booker-platform report API | http://localhost:3005 | |
| restful-booker-platform message API | http://localhost:3006 | |
| react-shopping-cart-bughunt | http://localhost:4000 | remapped off port 3000 to avoid colliding with `rbp-booking` |

Each app also has its own `docker-compose.yml` (`restful-booker-platform/docker-compose.yml`)
if you only want to run that one on its own.

## Hosting for learners (instead of local Docker)

Learners can also be pointed at hosted instances of the buggy apps:

- **react-shopping-cart-bughunt → Vercel.** It's a static SPA with no backend and no
  shared server state (the cart lives entirely in the browser), so a single deployment
  serves every learner safely. Config is in `react-shopping-cart-bughunt/vercel.json`;
  in the Vercel project set Root Directory = `docker/react-shopping-cart-bughunt` and
  Production Branch = `seeded/cohort-01`. Keep the GitHub repo **private** (it holds the
  answer keys — Vercel deploys private repos fine).
- **restful-booker-platform → a free VM (not Vercel).** Its long-running Java services
  can't run on Vercel. Host the whole compose stack on an Oracle Cloud Always-Free VM —
  step-by-step in `docker/deploy-oracle-vm.md`. Note it's one shared instance (data is
  shared across learners; `docker-compose.oracle.yml` resets it periodically) and the app
  is deliberately vulnerable, so restrict access and tear it down after the cohort.

## Seeded-bug branches

Bugs are injected on dedicated branches (e.g. `seeded/cohort-01`), one commit per bug
ID, never merged back to `main` — `main` stays the clean reference. See
`docker/seeded-bugs-shopping-cart.md` and `docker/seeded-bugs-restful-booker-platform.md`
for the full catalogs (author-only — do not hand these to learners).
