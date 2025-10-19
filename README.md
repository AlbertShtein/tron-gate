# 🔒 gRPC — mTLS (TLS 1.3) Setup

This project uses **mutual TLS (mTLS)** to secure gRPC communication.  
Both the **server** and **client** authenticate each other using certificates signed by a shared CA.

---

## 📂 Directory Structure

```
app/
├── src/
│   ├── main/
│   │   ├── kotlin/...
│   │   └── resources/
│   │       └── certs/
│   │           ├── localhost.p12
│   │           └── client.p12
├── build.gradle.kts
└── README.md
```

> The `src/main/resources/certs/` folder should contain all PEM files used for mTLS.

---

## 🧰 Generate Certificates (TLS 1.3–Compatible)

```bash
cd src/main/resources/certs

# ---- 1️⃣ Create Server Certificate ----
keytool -genkeypair \
  -alias localhost \
  -keyalg RSA \
  -keysize 2048 \
  -dname "CN=localhost, OU=Dev, O=Local, L=Local, ST=Local, C=US" \
  -validity 825 \
  -keystore localhost.p12 \
  -storetype PKCS12 \
  -storepass secret \
  -keypass secret \
  -ext SAN=dns:localhost,ip:127.0.0.1
# ---- 2️⃣ Create Client Certificate ----
keytool -genkeypair \
    -alias clientkey \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore client.p12 \
    -storepass secret \
    -keypass secret \
    -dname "CN=client.com, OU=IT, O=Company, L=City, ST=State, C=US" \
    -validity "365"
```

✅ **Resulting files:**

| File                          | Purpose |
|-------------------------------|----------|
| `localhost.p12` | Server credentials (for gRPC server) |
| `client.p12`    | Client credentials (for Postman or other gRPC clients) |

All certs are **TLS 1.3–compatible** and signed using **ECDSA P-256**.

---

## 🧪 Test with `grpcurl`

You can use [`grpcurl`](https://github.com/fullstorydev/grpcurl) to test the server with mTLS:

```bash
grpcurl   -proto your.proto   -cacert src/main/resources/certs/ca.crt   -cert src/main/resources/certs/client.crt   -key src/main/resources/certs/client.key   -authority localhost   -insecure   localhost:9090 your.Service/YourMethod
```

---

## 🧑‍💻 Using Postman for mTLS

Postman (v10+) supports **gRPC with mTLS**.  
Here’s how to make it trust your CA and send the client certificate.

### 1. Import CA certificate (trust root)

1. Open **Postman → Settings → Certificates**.  
2. Scroll to **CA Certificates**.  
3. Enable *“Enable CA Certificates”*.  
4. Click **Add CA Certificate** and select `ca.crt`.  
5. Restart Postman (recommended).

### 2. Configure client certificate

1. Still in **Settings → Certificates**, click **Add Client Certificate**.  
2. In the *Host* field, enter:  
   ```
   localhost
   ```
3. Set:
   - PRX file → `client.p12`  
   - Passphrase → `secret`
4. Save the configuration.

### 3. Make a gRPC request

1. Open a new **gRPC Request** in Postman.  
2. Enter the server URL:  
   ```
   grpcs://localhost:9090
   ```
3. Import your `.proto` file if needed.  
4. Select the **Method** (e.g., `your.Service/YourMethod`).  
5. Click **Invoke** — you should see a successful mTLS handshake.

---

## ✅ Troubleshooting

| Issue | Cause / Fix |
|-------|--------------|
| `SSLHandshakeException: unable to find valid certification path` | Client not trusting the server’s CA (`ca.crt` missing or misconfigured) |
| `UNAVAILABLE: io exception` | Client certificate missing or invalid |
| Postman shows `UNAVAILABLE` | Ensure you used `grpcs://` and imported both client + CA certs |
| gRPC not starting | Check cert file paths — they must exist at runtime |

---

## 🧩 Notes

- TLSv1.3 works automatically on **Java 11+** and **OpenSSL ≥ 1.1.1**.  
- Certificates here are **self-signed** for local development.  
  For production, use a proper CA or your organization’s PKI.  
- You can re-issue certificates periodically by rerunning the script above.

---

**Enjoy your secure gRPC server 🔐**
