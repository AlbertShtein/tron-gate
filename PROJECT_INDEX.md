# Project Index: Tron Gate

Generated: 2026-02-08

## Project Overview

Kotlin/Spring Boot gRPC microservice for Tron blockchain wallet management. Creates wallets, queries on-chain asset balances (TRX, USDT), scans TRON blocks for wallet-related transactions, and streams events to subscribers in real-time. Private keys are AES-256-GCM encrypted at rest with HMAC integrity verification.

## Tech Stack

- **Language**: Kotlin 1.9.25, Java 21
- **Framework**: Spring Boot 3.5.6, Spring gRPC 0.11.0
- **Build**: Gradle (Kotlin DSL), Protobuf plugin
- **Database**: PostgreSQL (Alpine), JPA/Hibernate, Liquibase migrations
- **Blockchain**: Trident SDK 0.10.0 (Tron network)
- **Transport**: gRPC with mTLS (port 8443)
- **Containerization**: Docker Compose (PostgreSQL service)

## Project Structure

```
tron-gate/
├── src/main/kotlin/ashtein/trongate/
│   ├── TronGateApplication.kt          # Spring Boot entry point (@EnableScheduling)
│   ├── config/
│   │   └── TronClientConfig.kt         # Read-only ApiWrapper bean for block scanner
│   ├── grpc/service/
│   │   └── GrpcServerService.kt        # gRPC endpoint implementations (3 RPCs)
│   ├── model/
│   │   ├── Wallet.kt                   # JPA entity (PK=BYTEA address, encrypted private key, HMAC sign)
│   │   ├── WalletEvent.kt             # JPA entity (tx events: TRX/TRC20/CONTRACT, IN/OUT, UUIDv7 PK)
│   │   ├── BlockCursor.kt             # JPA entity (scanner position tracking)
│   │   └── Signable.kt                # Interface for HMAC-signed entities
│   ├── repository/
│   │   ├── WalletRepository.kt         # JPA repository for Wallet (PK: ByteArray)
│   │   ├── WalletEventRepository.kt    # JPA repository for WalletEvent (dedup check + block range query)
│   │   └── BlockCursorRepository.kt    # JPA repository for BlockCursor
│   ├── service/scanner/
│   │   ├── BlockScannerService.kt      # Scheduled block scanner (TRX transfers, TRC-20, contracts)
│   │   ├── AddressCacheService.kt      # In-memory address cache (ConcurrentHashSet<ByteBuffer>)
│   │   └── EventBroadcastService.kt    # Pub/sub for real-time event streaming to gRPC clients
│   ├── service/wallet/
│   │   ├── WalletService.kt            # Core wallet logic (create, getAssets)
│   │   ├── AdapterInterface.kt         # Base adapter interface (getType)
│   │   ├── AssetsAdapterInterface.kt   # Asset balance adapter (getBalance via ApiWrapper)
│   │   └── adapter/
│   │       ├── TrxAdapter.kt           # Native TRX balance query
│   │       └── UsdtAdapter.kt          # TRC-20 USDT balance via contract call (balanceOf)
│   ├── util/
│   │   ├── UUIDv7.kt                   # Monotonic UUIDv7 generator (thread-safe, lock-free CAS)
│   │   ├── EncryptedStringConverter.kt # AES-256-GCM JPA converter for private keys
│   │   ├── IntegrityCheckListener.kt   # HMAC-SHA256 entity integrity verification (@PrePersist/@PostLoad)
│   │   └── TronAddressUtil.kt          # Base58Check encode/decode for Tron addresses
│   └── vo/
│       └── Private.kt                  # Value object wrapping private key bytes (toHex)
├── src/main/proto/
│   └── trongate.proto                  # gRPC service definition (3 RPCs)
├── src/main/resources/
│   ├── application.yaml                # Spring config (DB, gRPC, SSL, Tron nodes, scanner interval)
│   └── db/changelog/
│       ├── db.changelog.yaml           # Liquibase master changelog
│       └── changelog-001-init.yaml     # All tables: wallet, block_cursor, wallet_event
├── src/test/kotlin/ashtein/trongate/
│   └── TronGateApplicationTests.kt     # Placeholder test
├── postgres/
│   ├── Dockerfile                      # PostgreSQL Alpine with init scripts
│   └── init-db.sql                     # DB/table/user creation with least-privilege grants
├── build.gradle.kts                    # Build config with dependencies
├── compose.yml                         # Docker Compose (PostgreSQL service)
└── settings.gradle.kts                 # Root project name: "trongate"
```

## gRPC API (trongate.proto)

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `CreateWallet` | `EmptyDto` | `WalletResponseDto {address}` | Generate keypair, persist encrypted, return base58check address |
| `GetAssets` | `WalletAddressDto {address}` | `stream AssetsResponseDto {type, balance}` | Query on-chain balances (TRX, USDT) |
| `SubscribeEvents` | `SubscribeEventsRequest {last_block_number}` | `stream WalletEventDto` | Stream wallet events (catch-up + real-time) |

### SubscribeEvents Protocol

- `last_block_number >= 0`: replay missed events from DB, then stream real-time
- `last_block_number == 0`: all historical events + real-time
- `last_block_number < 0`: real-time only, no history replay

## Block Scanner

The `BlockScannerService` runs on a configurable interval (default 3s) and:

1. Reads current cursor position from `block_cursor` table
2. Fetches latest block number from Tron network
3. Iterates blocks `[cursor+1 .. latest]`, processing each transaction:
   - **TransferContract** (native TRX): extracts from/to/amount, matches against address cache
   - **TriggerSmartContract** (TRC-20): parses `Transfer` event logs (topic0 = ERC-20 Transfer signature), extracts token transfers
   - **Generic contracts**: extracts owner address from protobuf payload
4. Deduplicates events by `(tx_hash, wallet_address, direction)` unique constraint
5. Persists `WalletEvent` and broadcasts to all subscribed gRPC clients via `EventBroadcastService`
6. Updates cursor after each block

## Database Schema (3 tables)

| Table | Key Columns | Purpose |
|-------|-------------|---------|
| `wallet` | `address` (BYTEA PK), `private` (BYTEA encrypted), `sign` (BYTEA HMAC) | Wallet storage (append-only) |
| `wallet_event` | `id` (UUID PK), `wallet_address`, `tx_hash`, `block_number`, `event_type`, `direction`, `amount` | Transaction events |
| `block_cursor` | `id` (VARCHAR PK = "scanner"), `block_number`, `updated_at` | Scanner position |

Unique constraint: `(tx_hash, wallet_address, direction)` on `wallet_event`
Index: `idx_wallet_event_wallet_address` on `wallet_event(wallet_address)`

## Security Architecture

- **Encryption**: Private keys encrypted with AES-256-GCM before DB storage (`EncryptedStringConverter`)
- **Integrity**: HMAC-SHA256 signature on `address + private_key` bytes, verified on every entity load (`IntegrityCheckListener`)
- **Transport**: mTLS on gRPC (client cert required, JKS keystores)
- **Database**: Least-privilege users (`trongate` = SELECT+INSERT+UPDATE on cursor, `read` = SELECT only)
- **Key material**: AES/HMAC key via `L256_BASE64_KEY` env var

## Configuration (Environment Variables)

| Variable | Purpose |
|----------|---------|
| `L256_BASE64_KEY` | AES-256 + HMAC key (base64) |
| `DATABASE_URI` | JDBC PostgreSQL connection string |
| `DATABASE_USER` | Database username |
| `DATABASE_PASSWORD` | Database password |
| `FULLNODE_URL` | Tron full node gRPC endpoint |
| `FULLNODE_SOLIDITY_URL` | Tron solidity node gRPC endpoint |

App config in `application.yaml`: scanner interval (`parameters.scanner.interval`: 3000ms), USDT contract address (`TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf`), gRPC port 8443.

## Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-data-jpa` | JPA/Hibernate ORM |
| `spring-grpc-spring-boot-starter` | gRPC server integration |
| `liquibase-core` | Database schema migrations |
| `io.github.tronprotocol:trident:0.10.0` | Tron blockchain SDK |
| `org.postgresql:postgresql` | PostgreSQL JDBC driver |
| `spring-boot-starter-actuator` | Health/metrics endpoints |
| `com.google.protobuf` | Protobuf code generation |

## Quick Start

1. `cp .env.dist .env` and configure values
2. `docker compose up -d` (PostgreSQL)
3. `./gradlew bootRun` (application on gRPC port 8443, Liquibase auto-applies migrations)

## Test Coverage

- 1 test file (`TronGateApplicationTests.kt`) - context load placeholder
