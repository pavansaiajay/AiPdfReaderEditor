# ADR 0004: Semaphore-Bounded Native PdfRendererPool

## Status
Accepted

## Context
Android's PdfRenderer native C++ code is non-reentrant and causes fatal uncatchable SIGSEGV crashes when accessed concurrently.

## Decision
Guard all rendering through PdfRendererPool with Semaphore(4) and per-URI Mutex. Lifecycle-bound cleanup via DisposableEffect.onDispose.

## Consequences
- Complete elimination of native threading collisions.
- Memory and file descriptor safety.
