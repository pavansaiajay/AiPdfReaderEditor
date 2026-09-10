# ADR 0002: Token-Cost Dynamic AI Credit Economy

## Status
Accepted

## Context
Fixed credit fees (e.g. 5 credits per summary) were unfair to users for brief prompts and misaligned with cloud LLM billing based on tokens.

## Decision
Calculate credit deduction dynamically based on usageMetadata (input + output tokens) returned by Firebase Vertex AI:
Cost = max(1, ceil((InputTokens * 1.0 + OutputTokens * 3.0) / 2500))
Pre-flight check gates at minimum balance >= 1. Deductions occur strictly after successful API responses.

## Consequences
- Transparent, fair billing.
- Failed queries cost 0 credits.
- Offline tools remain 100% free with zero credit checks.
