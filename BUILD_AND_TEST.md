# Build and test — MADT Lab #4 (Code Review revision)

## Open in Android Studio

1. Extract this folder to a short path without `&`, e.g. `C:\Users\Yaroslav\AndroidStudioProjects\MADT_Lab4_Calculator`.
2. In Android Studio choose **File > Open** and select the folder containing `settings.gradle.kts`.
3. Wait for Gradle Sync. Use **JDK 21** if Android Studio offers a JDK selection.
4. Start the existing **Pixel 6 API 35** emulator and run the **app** configuration.

## Test calculations

- `12 + 8 =` → `20`; open History to see `12 + 8 = 20`.
- `8` then `1/x` → `0.125`.
- `9` then `√x` → `3`.
- `5` then `x²` → `25`.
- `200 + 10 % =` → `220`.
- `200 × 10 % =` → `20`.
- `8 ÷ 0 =` → *Cannot divide by zero*.
- `3 +/-` then `√x` → *Invalid input*.
- `CE`, `C`, `⌫`, `+/−` and memory row MC/MR/M+/M−/MS/M⌄.

## Local test command

From the project root (Windows PowerShell):

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

The test source is `app/src/test/java/com/example/madtlab4/CalculatorEngineTest.java`.
60 tests (47 original + 13 Code Review regressions) passed in a plain-Java environment
using a minimal local JUnit-compatible harness. This does NOT replace Android
Studio's Gradle test run or on-device UI testing.

## Work remaining per assignment

1. **Completed:** separate AI Code Review session; two prioritized bugs fixed.
   See `CODE_REVIEW_NOTES.md` for details, limitations and verification.
2. **Verified by student on 5 November 2026:** Android Studio project opened; Windows
   `:app:testDebugUnitTest` completed with `BUILD SUCCESSFUL`; the reviewed app
   ran on Pixel 6 API 35 and screenshots show arithmetic and history dialog.
3. **Completed:** separate Documentation Generation session added `README.md`,
   emulator screenshots and a draft repository description. See
   `DOCUMENTATION_SESSION_NOTES.md`.
4. **Pending:** separate AI session for GitHub commit/push.
5. **Pending:** `ListOfsessions` summary emphasizing student input; submit MD
   and GitHub link to Moodle.

**Validation provenance:** the student's screenshots and PowerShell log show a successful
Gradle unit-test build and actual emulator launch for the Code Reviewed project. This
was not independently reproduced in the documentation environment. Full UI automation,
process-death recovery, and accessibility testing have not been claimed.
