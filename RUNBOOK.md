# RUNBOOK — LiveKit Calls (Hetzner + HawkHost + Android)

Ovaj dokument opisuje kako ponovno postaviti i provjeriti `Calls` (LiveKit) end‑to‑end ako se server ili kod “sruši”.

## Arhitektura (opcija 1 — preporuka)

- **Android app** se uvijek spaja na: `wss://rtc.tmbv-hms.com`
- **Nginx (Hetzner)** terminira TLS i proxyja websocket na LiveKit: `http://127.0.0.1:7880`
- **LiveKit (Hetzner)** radi kao SFU (WebRTC), UDP range otvoren.
- **Backend (HawkHost)** izdaje **JWT** (HS256) s `iss=<api_key>` i potpisan s `api_secret` koji mora odgovarati `/etc/livekit.yaml`.

## 0) Preduvjeti

- DNS:
  - `rtc.tmbv-hms.com` **A record** → `46.225.108.6`
- Otvoreni portovi na Hetzneru (UFW):
  - TCP: `80`, `443`, `7880`, `7881`
  - UDP: `7881`, `50000-60000`

## SSH pristup (bez dijeljenja ključeva)

Ovdje su koraci kako pristupiti serverima preko SSH ključeva. **Ne stavljaj privatne ključeve u repo**; jedino javni ključ (`*.pub`) ide u `authorized_keys` na serveru.

### HawkHost (backend)

U tvom okruženju radi alias:

```bash
ssh hawkhost
```

To tipično znači da postoji unos u lokalnom `~/.ssh/config`. Provjeri tačno šta se koristi (ovo ne ispisuje privatni ključ):

```bash
ssh -G hawkhost | egrep '^(hostname|user|port|identityfile) '
```

Prema ranijem login promptu, HawkHost je:
- `User`: `tmbvhmsc`
- `HostName`: `nyc100.arandomserver.com`

Ako treba “eksplicitna” komanda bez aliasa (zamijeni prema outputu iz `ssh -G`):

```bash
ssh tmbvhmsc@nyc100.arandomserver.com
```

Dodavanje novog javnog ključa (na HawkHostu):
- dodaj sadržaj svog `~/.ssh/<key>.pub` u `~/.ssh/authorized_keys` na HawkHostu
- prava:
  - `chmod 700 ~/.ssh`
  - `chmod 600 ~/.ssh/authorized_keys`

### Hetzner (LiveKit / nginx)

Standardno (ako je root login omogućen i ključ je upisan):

```bash
ssh root@46.225.108.6
```

Gdje su ključevi na Hetzneru:
- `/root/.ssh/authorized_keys`

Provjera SSHD postavki (na Hetzneru):

```bash
sshd -T | egrep 'authorizedkeysfile|pubkeyauthentication|permitrootlogin|passwordauthentication'
```

Dodavanje novog javnog ključa (na Hetzneru):
- dodaj sadržaj svog `~/.ssh/<key>.pub` u `/root/.ssh/authorized_keys`
- prava:
  - `chmod 700 /root/.ssh`
  - `chmod 600 /root/.ssh/authorized_keys`

## 1) Hetzner — LiveKit servis

### 1.1 Provjera stanja

```bash
systemctl is-active livekit
systemctl status livekit --no-pager -l | sed -n '1,40p'
/usr/local/bin/livekit-server --version
```

### 1.2 Konfiguracija

File: `/etc/livekit.yaml`

Minimalno:

```yaml
port: 7880
bind_addresses:
  - 0.0.0.0
rtc:
  tcp_port: 7881
  port_range_start: 50000
  port_range_end: 60000
  use_external_ip: true
keys:
  goran: <64-hex HS256 secret>  # primjer: openssl rand -hex 32
```

Na serveru je praktično spremiti aktivni par u:
- `/root/livekit-keys.txt` (chmod 600)

Format:
```
api_key=goran
api_secret=<SECRET>
```

### 1.3 Firewall (UFW)

```bash
ufw status verbose
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 7880/tcp
ufw allow 7881/tcp
ufw allow 7881/udp
ufw allow 50000:60000/udp
```

### 1.4 Logovi (debug)

```bash
journalctl -u livekit -f
```

Traži:
- join/participant events
- `mediaTrack published` kad se uključi kamera/mikrofon

## 2) Hetzner — Nginx TLS reverse proxy (`wss://rtc.tmbv-hms.com`)

### 2.1 Site config

File: `/etc/nginx/sites-available/rtc.tmbv-hms.com` (symlink u `sites-enabled/`)

Minimalno (bitan je `Authorization` header i websocket upgrade):

```nginx
server {
    server_name rtc.tmbv-hms.com;

    location / {
        proxy_pass http://127.0.0.1:7880;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Authorization $http_authorization;

        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        proxy_read_timeout 3600;
        proxy_send_timeout 3600;
    }

    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/rtc.tmbv-hms.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/rtc.tmbv-hms.com/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

server {
    if ($host = rtc.tmbv-hms.com) { return 301 https://$host$request_uri; }
    listen 80;
    server_name rtc.tmbv-hms.com;
    return 404;
}
```

### 2.2 Nginx provjera + reload

```bash
nginx -t
systemctl reload nginx
systemctl is-active nginx
```

### 2.3 Access log (RTC)

Log file:
- `/var/log/nginx/rtc-access.log`

Praktični tail za Android:
```bash
tail -f /var/log/nginx/rtc-access.log | grep 'okhttp/4.12.0'
```

Očekivanje kad radi:
- `/rtc` → `101 Switching Protocols`
- `/rtc/validate` → `200`

## 3) HawkHost — backend token endpoint

Android app (po logovima) koristi backend:
- `https://tmbv-hms.com/aYOUTUBEocjenivanje5`

### 3.0 Psalm matchmaking (muško + žensko + isti omiljeni psalm)

Matchmaking se radi na backendu u `POST /api/psalms/get-room.php`:
- app šalje `gender` (`male`/`female`) i `favoritePsalm`
- backend vraća `room` koji je isti za par (muško+žensko) sa istim `favoritePsalm`
- backend koristi DB tabelu `psalm_call_queue` (TTL 5 min)
- backend dozvoljava pozive samo ako je `notMarried=true`

DB migracija:
- `BACKEND/migrations/018_add_psalm_call_queue.sql`

### 3.0.1 Dostupnost (datum/vrijeme) za kontakt po omiljenom psalmu

Backend endpoints:
- `POST /api/psalms/set-availability.php` (spremi dostupnost korisnika)
- `POST /api/psalms/list-availability.php` (lista dostupnosti suprotnog spola za isti psalm)

DB migracija:
- `BACKEND/migrations/019_add_psalm_call_availability.sql`

Ako vidiš `500` na `POST /api/psalms/get-room.php`:
- provjeri error log na HawkHostu (`api/psalms/error_log` ili globalni `logs/error.log`)

Primjer (na HawkHostu, koristi iste DB kredencijale kao aplikacija iz `.env`/`config.production.php`):
```bash
mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < /home/tmbvhmsc/public_html/aYOUTUBEocjenivanje5/migrations/018_add_psalm_call_queue.sql
```

### 3.1 LiveKit settings (source of truth)

File:
- `/home/tmbvhmsc/public_html/aYOUTUBEocjenivanje5/config/app_settings.json`

Mora sadržati:

```json
{
  "livekit_url": "wss://rtc.tmbv-hms.com",
  "livekit_api_key": "goran",
  "livekit_api_secret": "<MORA biti isti kao /etc/livekit.yaml keys.goran>"
}
```

**Napomena:** Ako ovdje ostane `devkey` ili pogrešan secret, Hetzner će vraćati `401` (invalid token).

### 3.2 Token endpoint

Endpoint:
- `POST /api/livekit/token.php`

Mora vratiti JSON:
```json
{
  "success": true,
  "data": {
    "url": "wss://rtc.tmbv-hms.com",
    "token": "eyJ....",
    "expires_in": 3600
  }
}
```

JWT payload minimalno:
- `iss = livekit_api_key`
- `sub = identity` (unique per device)
- `video.room = <room>`
- `video.roomJoin = true`

### 3.3 Verifikacija (bez Androida)

1) Uzmi token s backend-a (moraš imati validan `userToken` ako endpoint to traži).
2) Validiraj preko Hetzner nginx:

```bash
curl -sS -H "Authorization: Bearer <JWT>" \
  "https://rtc.tmbv-hms.com/rtc/validate?protocol=13"
```

Očekivanje: `success`

## 4) Android — Calls feature

### 4.1 Ključni moduli/fajlovi

- UI:
  - `composeApp/src/androidMain/kotlin/com/youtube/rating/android/ui/screens/CallsScreen.kt`
- ViewModel:
  - `composeApp/src/androidMain/kotlin/com/youtube/rating/android/viewmodel/CallsViewModel.kt`
- Manifest/network:
  - `androidApp/src/main/AndroidManifest.xml`
  - `androidApp/src/main/res/xml/network_security_config.xml`

### 4.2 Šta mora važiti u aplikaciji

- URL za LiveKit je **WSS**:
  - `wss://rtc.tmbv-hms.com`
- Token mora biti **JWT** (`x.y.z`, bez razmaka).
- `identity` mora biti unique po uređaju (npr. Install ID).

### 4.3 Tipični simptomi i uzrok

- Nginx log `/rtc` = `401` i `auth_present=1`:
  - token je poslan, ali je pogrešno potpisan (`iss`/secret ne matcha).
- Nginx log `/rtc` = `401` i `auth_present=0`:
  - token nije poslan (token prazan ili connect nije pozvan).
- Nginx log `/rtc` = `101`, ali nema video:
  - publish/subscription problem ili permisije (camera/mic).

## 5) “Jedan komad” smoke test

Kad sve radi, u realnom vremenu vidiš:

- Hetzner:
  - `tail -f /var/log/nginx/rtc-access.log | grep okhttp` → `/rtc` = `101`
  - `journalctl -u livekit -f` → join + `mediaTrack published`
- Android:
  - u `Calls` tabu remote video se pojavi čim drugi uđe u room.
