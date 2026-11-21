# Build the WifiSignalWave APK without GitHub Actions

## Fast zero-cost alternative: Google Colab

GitHub Actions jobs currently fail before any steps run (zero runner usage). For this public repository, standard runners are normally free; however account-level billing/authorization locks can still prevent their operation. A real source compilation has not been verified.

1. Open the [Google Colab notebook](../tools/build-debug-colab.ipynb) (from GitHub choose **Open in Colab** or use the Colab GitHub opener).
2. In Google Colab, use **Runtime → Run all** and accept the runtime connection. Use a **free runtime** only.
3. The notebook clones the **development branch** from GitHub, installs JDK 17, Google's Android SDK 34, and a checksum-verified Gradle 8.2.1 distribution, executes source checks and JUnit tests, then assembles `app-debug.apk`.
4. A download is started **only if** a nonempty APK is produced, with SHA-256 displayed for verification.

The notebook has not been run by this assistant, so it is an independently testable build route rather than a certified APK.

## Repair GitHub's runner access

Sign in as the repository owner at [GitHub Billing & Licensing](https://github.com/settings/billing). Review the account's payment restrictions/authorization holds and Actions budgets. Because the repository is public and uses `ubuntu-latest`, standard hosted Actions should not consume paid minutes. If Actions is locked despite no payment due, contact [GitHub Support](https://support.github.com) with the failing run URL: https://github.com/Spiritjcrow/WifiSignalWave/actions/runs/44913833619 .

**Do not change budgets, add a payment method or authorize charges merely to build the public-repository APK.** Account billing lock details are accessible to the account owner, not via the repository connector.
