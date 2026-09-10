---
name: android-security
description: Security guidelines for Firebase, Scoped Storage, Keystore, PII, and API keys.
---

# Android Security & Privacy Guidelines

## Rules:
1. **Never Commit Hardcoded Secrets**: API keys, OAuth client secrets, or private certificates must never appear in Git repositories or raw code.
2. **Encrypted Storage**: Use Android Keystore / EncryptedSharedPreferences for sensitive tokens if required.
3. **Scoped Storage Compliance**: Zero access to arbitrary filesystem paths without SAF or MediaStore APIs.
4. **No Sensitive Logging**: Never log passwords, tokens, full document contents, or personal identifiable information (PII) to Logcat or Crashlytics.
5. **Secure Network**: Enforce HTTPS and network security configurations.
