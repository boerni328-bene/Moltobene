# Projektspezifische Schutzregeln für R8 kommen hier hinein.
# Sie verhindern, dass R8 Code entfernt, der nur indirekt benutzt wird
# (z. B. beim Lesen und Schreiben von Sicherungsdateien).

# Zeilennummern in Fehlermeldungen behalten; die Zuordnungsdatei (mapping.txt)
# wird beim Build auf GitHub mit abgelegt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Navigationsziele: werden über ihren Klassennamen und ihre Serialisierung gefunden.
-keep class com.moltobene.app.ui.navigation.*Route { *; }
-keep class com.moltobene.app.ui.navigation.*Route$* { *; }

# Aufbau der Sicherungsdatei: Feldnamen und Serialisierer müssen erhalten bleiben,
# damit alte Sicherungen immer lesbar bleiben.
-keep class com.moltobene.app.data.backup.Backup* { *; }
-keep class com.moltobene.app.data.backup.Backup*$* { *; }

# Texterkennung (ONNX Runtime): der native Teil greift über feste Namen auf diese Klassen zu.
-keep class ai.onnxruntime.** { *; }
