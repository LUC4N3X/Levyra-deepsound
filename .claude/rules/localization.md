---
paths:
  - "app/src/main/java/com/luc4n3x/levyra/ui/i18n/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/ui/**/*.kt"
---

# Localization

- Put user-visible text in the existing Levyra string catalog instead of hardcoding it in composables or ViewModels.
- Preserve the fallback language and normalization behavior.
- Update every required language implementation or provide an intentional fallback in the same change.
- Keep formatting placeholders consistent across languages.
- Check long text, RTL layout, plural/number formatting, and accessibility labels when the change affects them.
- Do not claim language switching was tested unless it was exercised on a running app or instrumentation environment.
