# Contributing

## Development setup

1. Install JDK 17+, Android SDK tooling, and MongoDB or Docker.
2. Clone the repository.
3. Run `./run.sh backend` to start MongoDB and the API.
4. Open `android/` in Android Studio, or connect a device and run `./run.sh`.

Local credentials and generated files belong in ignored paths such as `.run/`,
`.env`, or `android/local.properties`. Never commit MongoDB connection strings,
JWT signing secrets, APK signing keys, or service-account files.

## Before opening a pull request

Run:

```bash
./backend/gradlew -p backend test shadowJar --console=plain
./android/gradlew -p android testDebugUnitTest lintDebug assembleDebug --console=plain
tests/run-sh-launch-test.sh
tests/android-backup-test.sh
```

Keep changes focused, add regression tests for behavior changes, and describe
manual verification in the pull request.
