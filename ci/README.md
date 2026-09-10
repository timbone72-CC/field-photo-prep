# Stable test signing asset

`field-photo-prep-test.jks` is intentionally a **non-production** Android signing key for Field Photo Prep debug/test APKs.

Purpose: keep the same package signature across CI runs so test APKs can update one another without uninstalling and losing app-private test state.

This key is not secret and must not be trusted as a production identity.

- alias: `field-photo-prep-test`
- store/key password: `field-photo-prep-test`
- certificate SHA-256: `2C:0A:96:16:FD:81:93:33:ED:98:B3:35:93:FB:E5:97:E1:20:E9:59:36:10:3C:6B:7A:76:27:0E:98:5D:3F:BA`
- keystore SHA-256: `9de15c655cb50e86fbc84bbd48b1f44c1fbb3a6dd6de50a7077ca8cc00a31197`

Never reuse this key for a production/release build. A real release must use a separate secured release-signing identity. Replacing this test key will intentionally break update compatibility with previously stable-signed test APKs.
