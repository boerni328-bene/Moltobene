# Sicherheit

## Sicherheitslücken melden

Sicherheitslücken werden vertraulich gemeldet, nicht als öffentliches Issue: über [„Report a vulnerability“](https://github.com/boerni328-bene/Moltobene/security/advisories/new) im Reiter „Security“ dieses Repositorys. Die Meldung sehen nur die Verantwortlichen des Projekts.

Hilfreich sind die betroffene Version (Einstellungen → Info), eine kurze Beschreibung und die Schritte, mit denen sich das Problem nachvollziehen lässt.

## Unterstützte Versionen

Korrekturen gibt es nur für die jeweils neueste Version unter [Releases](https://github.com/boerni328-bene/Moltobene/releases/latest).

## Grundsätze

- Rezepte und Fotos verlassen das Handy nur, wenn sie selbst geteilt oder gesichert werden.
- Kein Konto, keine Werbung, kein Tracking, keine Analyse- oder Absturzdienste.
- Die App fordert nur Berechtigungen an, die eine Funktion zwingend braucht. Derzeit sind es keine.
- Jede Version wird beim Bauen auf Berechtigungen und bekannte Telemetrie-Bausteine geprüft (#49). Version 0.14.0 enthielt unbemerkt die Internet-Berechtigung und eine Telemetrie von Microsoft aus einem Baustein der Texterkennung (ONNX Runtime); ab 0.14.1 ist beides entfernt.
- Jede APK ist mit demselben Schlüssel signiert. Fingerabdruck und Prüfsummen stehen in der [README](README.md#echtheit-prüfen).

---

## Security (English)

Please report vulnerabilities privately via [“Report a vulnerability”](https://github.com/boerni328-bene/Moltobene/security/advisories/new) in the Security tab, not as a public issue. Only the latest release receives fixes.
