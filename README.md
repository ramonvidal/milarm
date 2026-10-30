# Indews: Xiaomi Smart Band 10 app

A starter app for the **Xiaomi Smart Band 10**, which runs **Xiaomi Vela**. Vela apps are written
in JavaScript as `.ux` files (template + script + style) and packaged into a `.rpk` file.

The sample app is a tap counter:
- tap the orange dial to count up (short vibration)
- tap **Reset** to clear (long vibration)
- the count is saved on the band and survives restarts

## Project layout

```
src/
  manifest.json        app id, name, icon, permissions (features), pages
  app.ux               app lifecycle
  common/logo.png      app icon
  pages/index/index.ux the counter screen
```

The screen is designed for the Band 10's 212 × 520 px display (`designWidth: 212`).

## Build

Requires Node.js 18+.

```bash
npm install
npm run build     # debug build   -> dist/com.indews.counter.debug.1.0.0.rpk
npm run release   # release build -> dist/ (signed with keys in ./sign if present)
```

## Run on the band

- **Recommended:** open this folder in Xiaomi's [AIoT-IDE](https://iot.mi.com/vela/quickapp/en/tools/)
  (a VS Code–based IDE). It has a Vela emulator and can install to a real device.
- **Sideloading the `.rpk`:** Xiaomi has no public app store for third-party Band 10 apps, so `.rpk`
  files are usually installed with community tools, such as AstroBox. Check that the tool
  supports the Band 10 before using it.

## Next steps

- Add pages under `src/pages/` and register them in `manifest.json` → `router.pages`.
- Add the system APIs you use to `manifest.json` → `features` (e.g. `system.sensor`, `system.fetch`).
- Docs: [Xiaomi Vela JS app framework](https://iot.mi.com/vela/quickapp/en/)
