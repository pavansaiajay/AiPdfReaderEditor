# E2E Test Infra: AiPdfReaderEditor Android Application

## Test Philosophy
- Opaque-box, requirement-driven testing based on `ORIGINAL_REQUEST.md`.
- Methodology: Category-Partition + Boundary Value Analysis (BVA) + Pairwise Interaction Testing + Real-World Workload Scenarios.
- Verification Target: All 35 inventoried features across Monetization (R1), Scoped Storage & Room Sync (R2), Memory & Lifecycles (R3), and Flicker-Free UI & Gestures (R4).

## Test Suite Architecture
- **Unit & Logic Verification**: JVM unit tests under `app/src/test/java/pavansaiajayx/aipdfreadereditor/` testing ViewModels, CreditManager, Storage flows, and Room DAOs.
- **Instrumented & Component Tests**: Android tests under `app/src/androidTest/java/` or automated verification scripts testing Compose rendering, gestures, and UI states.
- **Build Verification**: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.

## Test Tier Inventory
| Tier | Description | Coverage Target |
|------|-------------|-----------------|
| Tier 1 | Feature Coverage (Free offline tools, AI credit gating, storage saving, Room sync, thumbnail drawing) | >= 50 test cases |
| Tier 2 | Boundary & Corner Cases (0 credits, max pages, empty selections, invalid passwords, stream EOF, memory limits) | >= 40 test cases |
| Tier 3 | Cross-Feature Combinations (Merge -> Annotate -> Save -> History, Split -> OCR -> Export) | >= 15 test cases |
| Tier 4 | Real-World Workload Scenarios (Full lifecycle user workflows: scanning, annotating, batch splitting, history access) | >= 10 test cases |
| Tier 5 | Adversarial Coverage Hardening (Stress testing, resource leak audits, concurrency stress) | Continuous hardening |

## Key Verification Commands
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
