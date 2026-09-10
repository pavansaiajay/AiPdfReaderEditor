---
name: android-project-context
description: Project-specific domain rules, economy invariants, PDF memory safety, and native ad placement guidelines for AiPdfReaderEditor.
---

# AiPdfReaderEditor: Project Context & Invariants

## 1. The Economy & Monetization Invariant
- **Offline Utilities are 100% FREE**: Merge, Split, Extract, Delete, Reorder, Rotate, Compress, Scan, Encrypt, Decrypt, Watermark, Flatten, HTML to PDF, and Text Stripper must NEVER check or deduct credits.
- **Paid AI Features are Token-Metered**: Chat, Summarize, and OCR check minimum balance (`credits >= 1`) beforehand, halt with `InsufficientCredits` if empty, and deduct credits based on Gemini `usageMetadata` strictly AFTER successful response generation:
  $$\text{Cost} = \max\left(1, \left\lceil \frac{\text{Input Tokens} \times 1.0 + \text{Output Tokens} \times 3.0}{2,500} \right\rceil\right)$$
- **Rewarded Ads**: Watching an ad awards **5 Credits**.
- **Daily Bonus**: Grants **5 to 10 Credits** on the first launch of each day.
- **Blended Ads**: Ads must be styled as native cards inside recent files or tool grids with an `[ Ad ]` badge. NO persistent bottom banner bars.

## 2. Memory & PDF Safety Invariants
- **PdfRenderer Concurrency**: Android's `PdfRenderer` is strictly non-reentrant. Always route rendering through `PdfRendererPool` with `Semaphore(4)` and per-URI `Mutex`.
- **Safe Bitmap LRU Cache**: Byte-counted LRU memory cache (max 1/8th JVM memory). Never call `.recycle()` in `entryRemoved` to prevent Compose Canvas render crashes.
- **Large Image OOM Defense**: Downsample images in `imagesToPdf` using `calculateInSampleSize` to enforce $\le 2048 \times 2048$ dimensions and $\le 16\text{ MB}$ allocation.
