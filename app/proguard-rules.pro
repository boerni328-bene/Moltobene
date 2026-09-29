# Projektspezifische Schutzregeln für R8 kommen hier hinein.
# Sie verhindern, dass R8 Code entfernt, der nur indirekt benutzt wird
# (z. B. beim Lesen und Schreiben von Sicherungsdateien).

# Zeilennummern in Fehlermeldungen behalten; die Zuordnungsdatei (mapping.txt)
# wird beim Build auf GitHub mit abgelegt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
