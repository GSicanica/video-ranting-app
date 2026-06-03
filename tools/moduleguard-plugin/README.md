# ModuleGuard Plugin

Local IntelliJ/Android Studio plugin to enforce module dependency rules.

Build/Run:

```bash
cd tools/moduleguard-plugin
./gradlew runIde
```

Rules file:
- Project root: `moduleguard.rules.json`

MVP:
- Tool window with violations list
- Build file annotations for disallowed `project(":module")` deps
