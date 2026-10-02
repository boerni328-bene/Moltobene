package com.moltobene.app.ui.whatsnew

/**
 * „Neu in Version …“ (#17): erscheint einmal nach einem Update, ohne Internet.
 * Die Texte stehen als `whats_new_items` in beiden strings.xml. Bei einer Version mit neuen
 * sichtbaren Funktionen Texte, [VERSION_CODE] und [VERSION_NAME] gemeinsam anpassen.
 */
object WhatsNew {
    /** versionCode der Version, zu der die Texte gehören. */
    const val VERSION_CODE = 23

    /** Wird im Titel angezeigt. */
    const val VERSION_NAME = "0.10.0"

    /**
     * @param lastSeenVersionCode zuletzt erledigte Version; null, wenn die App das noch nie gemerkt hat
     * @param isFreshInstall frisch installiert – dann ist alles neu und die Übersicht überflüssig
     */
    fun shouldShow(lastSeenVersionCode: Int?, isFreshInstall: Boolean, versionCode: Int = VERSION_CODE): Boolean =
        if (lastSeenVersionCode == null) !isFreshInstall else lastSeenVersionCode < versionCode
}
