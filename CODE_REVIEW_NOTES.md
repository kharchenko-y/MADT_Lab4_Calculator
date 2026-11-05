# MADT Lab #4 — Code Review Notes

**Session:** Independent AI Code Review — 5 November 2026  
**Variant:** Calculator App (Viber), Java/XML Android project

## Student input
The student provided the generated Android Studio ZIP and requested an independent
review of architecture, arithmetic, percentages, zero division, memory, calculation
history, activity lifecycle, and design compliance. The student specifically required
that the AI implement **exactly two prioritized functional fixes** without manual code
editing, preserve unaffected behavior, and leave README/GitHub tasks for other sessions.

## Review coverage and findings
- **Architecture:** `MainActivity` handles UI; `CalculatorEngine` owns calculation state,
  memory and history. The separation is appropriate for this lab.
- **Arithmetic and percentages:** `BigDecimal` is used without third-party calculation
  libraries. Basic arithmetic, percentages, reciprocal (1/x), square and square root
  were checked. Zero division and negative square roots use explicit error states.
- **Memory and history:** MC, MR, M+, M−, MS, and memory display are wired. History is
  newest-first, limited to 100 entries, and clearable; selection recalls the result.
- **Activity lifecycle:** the engine is `Serializable` and is saved/restored via
  `onSaveInstanceState`. Pure-Java serialization regression checks passed; Android
  process recreation on an emulator was **not** tested in this review environment.
- **UI and assignment:** 6×4 Standard-style keypad and memory row correspond to the
  assignment reference. All button labels use `@string` resources. History appears
  as an Android dialog rather than a dedicated screen; no pixel-level emulator
  screenshot comparison was possible during this review.

## Fix 1 — HIGH: Incorrect sign change of the second operand
**Before:** `5`, `+`, `+/−`, `2`, `=` produced **−47**, because the sign key changed
`5` (the previous operand) to `-5`, then appended `2` to give `-52`.  
**Change:** when a binary operator is waiting for a right operand, the sign key starts
`-0` as the new operand. Subsequent digit/decimal input works normally.  
**After:** the same sequence produces **3**; negative fractions and sign toggling
were covered by regression tests.

## Fix 2 — HIGH: Incorrect numeric-entry state after operations
**Before:** after `5 + √9`, typing `2` appended to `3`, so `=` produced **37** instead
of **7**. After `200 + 10%`, typing `2` produced **402** instead of **202**.
A related stale repeat-operation state made `5 + 3 =`, then `4 =` produce **7**.
**Change:** an explicit "replace on next digit" flag for calculated right operands;
typing a fresh number now replaces rather than appends. The pending operand remains
usable by `=` or chained operators. Starting a new calculation after `=` also clears
the previous repeat operation, while successive `=` presses still repeat correctly.
**After:** all the above input sequences produce the expected values.

## Validation
- **60/60 Java engine tests passed:** the original 47 tests plus 13 new regression
  tests; compiled with Java 21 and executed in an isolated minimal
  JUnit-compatible test harness (not the Android Gradle runner).
- **XML/static UI checks passed:** 8 XML files parse; 36 unique view IDs resolve;
  all button labels reference string resources (50 strings total).
- **Android build/AVD validation:** not executed here because Android SDK, Gradle
  distribution/cache, and emulator are unavailable. The student previously verified
  that the original ZIP launched on Pixel 6 API 35; the reviewed ZIP still requires
  an Android Studio Gradle Sync/build and a quick manual emulator smoke test.

## Remaining limits (not part of the two selected fixes)
- Extremely large results (e.g., repeatedly squaring 9 ten or more times) can make
  `Math.sqrt(double)` produce infinity and cause `NumberFormatException` on `√x`.
  This edge case is **not fixed** in this two-change review and is a documented
  stability limitation, not a claim that all inputs are safe.
- Runtime history and memory are preserved across Activity state recreation, but
  intentionally not persisted across a fresh app restart. Layout responsiveness
  at nondefault font/display sizes was not tested on a device.

## Files affected
- `CalculatorEngine.java`: the two prioritized state-transition corrections.
- `CalculatorEngineTest.java`: 13 regression tests.
- `CODE_REVIEW_NOTES.md`, `BUILD_AND_TEST.md`, `SESSION_HANDOFF.md`: review and
  verification record. No README or GitHub actions performed.
