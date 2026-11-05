# MADT Lab #4 — Code Generation session notes

**Session role:** AI-assisted code generation (planning equivalent to `/grill-me`).

## Student input and decisions
- Student asked to start the remaining three MADT laboratories early to reduce later workload.
- Student provided `MADT_4_Lab_Calculator_Viber.pdf` and asked the assistant to begin.
- The design reference is the Windows Calculator **Standard** mode image on page 1.
- Page 2 shows a separate history view and requires arithmetic, unary functions, and session history.
- The student previously worked with Java/XML Android projects and a Pixel 6 API 35 emulator; this generation uses those as the initial implementation defaults.

## AI design decisions — reviewable, not yet approved by student
- Java with XML Views, no third-party arithmetic library.
- A standalone `CalculatorEngine` class stores the calculation state and history.
- `1/x` is interpreted as the required fraction/reciprocal calculation because that is the labeled key in the provided Windows Standard reference.
- Percent is relative to the first operand of a pending binary operation.
- The history dialog lists calculations from the current session, most recent first; pressing an item recalls its result.
- Five basic memory controls (MC, MR, M+, M-, MS) are included to match the reference's memory row.

## Validation and boundaries
- Local plain-Java engine checks are performed by a standalone smoke runner.
- Android Studio Gradle Sync, app build, emulator launch, and visual comparison remain to be done on the student's machine.
- Code Review, README, GitHub publication and final list-of-sessions summary must happen in **separate AI sessions**, per the assignment.
- Do not present this document as the final required cross-session summary: the later sessions have not happened.
