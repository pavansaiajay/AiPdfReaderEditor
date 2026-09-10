---
name: android-performance
description: Performance optimization for app startup, memory, battery, and background execution.
---

# Android System Performance

## Rules:
1. **Cold Startup Target**: Under 1.5 seconds. Defer heavy SDK initialization from Application.onCreate().
2. **Memory Footprint**:
   - Cap in-memory image and thumbnail caches at 1/8th JVM heap.
   - Guard against Bitmap retention and Native PdfRenderer leaks.
3. **Battery & Background Work**:
   - Use WorkManager with constraints (charging, battery not low, Wi-Fi) for heavy background batch tasks.
   - Avoid polling or wakelocks.
4. **Build Optimization**: Enable R8 shrinking and ProGuard minification for release builds.
