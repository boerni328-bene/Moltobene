# Sichert die Artefakte der GitHub-Builds auf den PC und raeumt sie danach auf GitHub auf.
# Nur ASCII im Skript, damit Windows PowerShell 5.1 es ohne Zeichensatz-Probleme liest.
#
# Aufruf:
#   powershell -File .github\scripts\artefakte-sichern.ps1                 # nur sichern
#   powershell -File .github\scripts\artefakte-sichern.ps1 -Loeschen        # sichern, dann auf GitHub loeschen
#
# Regeln:
# - Eine Datei wird nur auf GitHub geloescht, wenn die Kopie auf dem PC vorhanden ist und
#   genau die Groesse hat, die GitHub meldet.
# - Je Art (z. B. "moltobene", "bildschirmfotos") bleiben die neuesten -Behalten Artefakte auf GitHub.
# - Bereits gesicherte Dateien werden nie erneut geladen: Die Sicherung waechst nur.
param(
    [string]$Repo = "boerni328-bene/Moltobene",
    [string]$Ziel = "D:\Claude\Sicherung\github-artefakte",
    [int]$Behalten = 1,
    [switch]$Loeschen
)

$ErrorActionPreference = "Stop"
$gh = "C:\Program Files\GitHub CLI\gh.exe"
New-Item -ItemType Directory -Force -Path $Ziel | Out-Null
$protokoll = Join-Path $Ziel "protokoll.txt"
function Log($text) {
    $zeile = "{0:yyyy-MM-dd HH:mm:ss}  {1}" -f (Get-Date), $text
    Write-Host $zeile
    Add-Content -Path $protokoll -Value $zeile
}

# Alle Artefakte holen (neueste zuerst), abgelaufene gibt es nicht mehr herunterzuladen.
$zeilen = & $gh api "repos/$Repo/actions/artifacts?per_page=100" --paginate `
    -q '.artifacts[] | select(.expired==false) | [.id,.size_in_bytes,.created_at,.name] | @tsv'
$liste = foreach ($z in $zeilen) {
    $t = $z -split "`t"
    [pscustomobject]@{ Id = $t[0]; Groesse = [int64]$t[1]; Datum = $t[2]; Name = $t[3] }
}
Log ("GitHub meldet {0} Artefakte." -f @($liste).Count)

$gesichert = 0; $uebersprungen = 0; $fehler = 0
foreach ($a in $liste) {
    $ordner = Join-Path $Ziel $a.Name
    New-Item -ItemType Directory -Force -Path $ordner | Out-Null
    $datei = Join-Path $ordner ("{0}_{1}.zip" -f $a.Datum.Substring(0, 10), $a.Id)
    if ((Test-Path $datei) -and ((Get-Item $datei).Length -eq $a.Groesse)) { $uebersprungen++; continue }
    $teil = "$datei.part"
    # cmd-Umleitung ist binaersicher (PowerShell 5.1 wuerde die Zip-Datei beschaedigen).
    cmd /c "`"$gh`" api repos/$Repo/actions/artifacts/$($a.Id)/zip > `"$teil`" 2>nul"
    if ((Test-Path $teil) -and ((Get-Item $teil).Length -eq $a.Groesse)) {
        Move-Item -Force $teil $datei
        $gesichert++
    } else {
        Remove-Item -Force $teil -ErrorAction SilentlyContinue
        $fehler++
        Log ("FEHLER beim Sichern von {0} (Id {1})." -f $a.Name, $a.Id)
    }
}
Log ("Gesichert: {0}, schon vorhanden: {1}, Fehler: {2}." -f $gesichert, $uebersprungen, $fehler)

if ($Loeschen) {
    $geloescht = 0
    # Die Namen enden auf die Version ("moltobene-0.54.0"): Zur Art zaehlt nur der Teil davor.
    foreach ($gruppe in ($liste | Group-Object { $_.Name -replace '-\d+\.\d+\.\d+$', '' })) {
        $alt = $gruppe.Group | Sort-Object Datum -Descending | Select-Object -Skip $Behalten
        foreach ($a in $alt) {
            $datei = Join-Path (Join-Path $Ziel $a.Name) ("{0}_{1}.zip" -f $a.Datum.Substring(0, 10), $a.Id)
            if ((Test-Path $datei) -and ((Get-Item $datei).Length -eq $a.Groesse)) {
                & $gh api -X DELETE "repos/$Repo/actions/artifacts/$($a.Id)" | Out-Null
                $geloescht++
            }
        }
    }
    Log ("Auf GitHub geloescht: {0} (je Art bleiben die neuesten {1})." -f $geloescht, $Behalten)
}
