# Hosting restful-booker-platform on a free Oracle Cloud VM

This deploys the **whole restful-booker-platform stack** (6 Java services + the Next.js
frontend, with the seeded bugs from `seeded/cohort-01`) onto a single always-free Oracle
Cloud VM, so learners get a URL instead of running Docker locally.

> The shopping-cart app does **not** go here — it's hosted on Vercel (see
> `docker/react-shopping-cart-bughunt/vercel.json` and the notes in the repo). This VM is
> only for restful-booker-platform, which can't run on Vercel (long-running JVM services).

## ⚠️ Two things to understand before you start

1. **It's one shared instance.** Every learner hits the same app and the same data. When
   one learner creates/deletes a booking, room, or message, everyone sees it. The compose
   override `docker-compose.oracle.yml` sets `dbRefresh=120`, so each service resets its
   database to the seed state every 2 hours to limit the drift. Tune or disable it there.
   This is the trade-off for a single free instance vs. the per-learner local Docker setup.
2. **This app is deliberately vulnerable.** It ships with a seeded auth-bypass (RBP-04 —
   any password logs in) and other intentional defects. Do not leave it open to the whole
   internet longer than the cohort needs it. Restrict the cloud firewall rule to your
   learners' IP range if you can, and tear the VM down when the cohort ends.

## 1. Provision the VM (Oracle Cloud console)

- Sign up for Oracle Cloud (the **Always Free** tier; signup needs a card for identity
  verification but always-free resources are not charged).
- **Compute → Instances → Create instance:**
  - Shape: **VM.Standard.A1.Flex** (Ampere ARM — this is the always-free one). Give it
    **2 OCPU / 12 GB RAM** (the stack's max heap is ~2.3 GB; this leaves headroom and stays
    inside the 4 OCPU / 24 GB always-free allowance).
  - Image: **Ubuntu 22.04** (or 24.04), **aarch64/ARM**. The stack's images are all
    multi-arch and build natively on ARM.
  - Add your SSH public key.
- After it boots, note the **public IP**.

## 2. Open port 80 in the cloud firewall

Only port 80 needs to be public — the browser only talks to the frontend, which proxies
to the Java services over the internal Docker network (server-side Next.js rewrites), so
ports 3000–3006 stay closed to the internet.

- **Networking → Virtual Cloud Networks → (your VCN) → (your subnet) → Security List →
  Add Ingress Rule:**
  - Source CIDR: `0.0.0.0/0` (or, better, your learners' office/VPN CIDR)
  - IP Protocol: TCP, Destination Port: **80**
- Port 22 (SSH) is already open by default.

## 3. Set up the VM

SSH in (`ssh ubuntu@<public-ip>`), then:

```sh
# --- Docker Engine + Compose plugin ---
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
# log out and back in so the docker group applies, then continue

# --- Open port 80 on the host firewall ---
# Oracle's Ubuntu images ship a restrictive iptables ruleset; add 80 before the REJECT.
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo netfilter-persistent save
```

## 4. Get the code

The repo is private (it contains answer keys), so authenticate however you prefer — a
GitHub deploy key or a personal access token. Then:

```sh
git clone https://github.com/younessoulah/qa-curriculum.git
cd qa-curriculum
git checkout seeded/cohort-01
cd docker/restful-booker-platform
```

## 5. Build the six service jars

Each Java service's Docker image copies a pre-built `*-exec.jar`, so the jars must be
built first. The Next.js frontend builds itself inside its own image, so you do **not**
need Node on the host.

**Option A — build in a container (only Docker required, recommended):**
```sh
docker run --rm -v "$PWD":/build -v "$HOME/.m2":/root/.m2 -w /build \
  maven:3.9-eclipse-temurin-26 \
  mvn clean install -DskipTests -pl auth,booking,room,message,report,branding
```
> If that exact image tag has moved, pick any current `maven:*-eclipse-temurin-26` tag
> from Docker Hub — the app targets Java 26.

**Option B — build with a host toolchain (verified path):** install Temurin 26 + Maven,
then run the same `mvn` line without the container wrapper.
```sh
sudo mkdir -p /etc/apt/keyrings
wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release; echo $VERSION_CODENAME) main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt-get update && sudo apt-get install -y temurin-26-jdk maven
mvn clean install -DskipTests -pl auth,booking,room,message,report,branding
```

## 6. Start the stack

```sh
cd ..   # into docker/
docker compose -f docker-compose.yml -f docker-compose.oracle.yml up -d --build
```

First build takes a few minutes (it compiles the Next.js frontend and pulls JRE images).
Then:

```sh
docker compose -f docker-compose.yml -f docker-compose.oracle.yml ps    # all "Up"
curl -s -o /dev/null -w "%{http_code}\n" http://localhost/               # 200
```

## 7. Access

- Learners: **http://<public-ip>/**
- Admin panel login: **admin / password** (remember RBP-04 means any password works — that's a seeded bug).

## 8. Day-to-day

```sh
# logs
docker compose -f docker-compose.yml -f docker-compose.oracle.yml logs -f rbp-booking
# restart after pulling updates / re-seeding
git pull && git checkout seeded/cohort-01
cd restful-booker-platform && mvn clean install -DskipTests -pl auth,booking,room,message,report,branding && cd ..
docker compose -f docker-compose.yml -f docker-compose.oracle.yml up -d --build
# stop everything
docker compose -f docker-compose.yml -f docker-compose.oracle.yml down
```

## 9. Optional polish — a domain + HTTPS

Point a domain's A record at the public IP and put a small reverse proxy in front of
port 80 (Caddy is the least effort — it auto-provisions Let's Encrypt certs). This is not
required for the bug hunt; http on the public IP is enough to get learners testing.
