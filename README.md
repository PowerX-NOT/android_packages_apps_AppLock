# App Lock (system integration)

System-level app protection mechanism with secure access verification UI.

## Components

| Layer | Package / service | Role |
|-------|-------------------|------|
| Settings + auth UI | `packages/apps/AppLock` | Dashboard, locked apps, privacy password, relock policy, unlock overlay |
| Framework | `AppLockService` / `AppLockManager` | Launch interception, session unlock, notifications |

## End-to-end test

1. Flash ROM with `AppLock` and framework `AppLockService`.
2. Open Settings → App Lock.
3. Set a privacy password (PIN, password, or pattern).
4. Enable App Lock and select apps to lock.
5. Launch a locked app.
6. Authenticate using the unlock overlay.
7. Confirm the app opens after successful authentication.

## Credits

- Based on work and source references from [AxionAOSP](https://github.com/AxionAOSP)
