buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Seit AGP 9 übersetzt das Android-Bauwerkzeug Kotlin selbst (eingebautes Kotlin) und bringt dafür
        // eine eigene, ältere Kotlin-Version mit. Diese Zeile hebt sie auf die Version aus libs.versions.toml,
        // passend zu den Kotlin-Plugins für Compose und Serialisierung.
        classpath(libs.kotlin.gradle.plugin)
        // Guava kommt nur über die Bauwerkzeuge mit (nicht in die App). Mindestens 33.7.2 wegen einer
        // Sicherheitslücke beim Einlesen gespeicherter Daten (Dependabot #10).
        constraints {
            classpath(libs.guava)
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// Ältere Guava-Versionen kommen außerdem über Werkzeuge im App-Modul mit (Room-Compiler, Lint, Robolectric),
// nicht in die App selbst. Alles unter der Version aus libs.versions.toml wird angehoben (Dependabot #10);
// neuere Versionen bleiben, die Variante (-jre/-android) bleibt erhalten.
subprojects {
    val guavaFixed = rootProject.libs.versions.guava.get() // z. B. "33.7.2-jre"
    fun guavaNumbers(version: String) = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    fun guavaOlder(version: String): Boolean {
        val a = guavaNumbers(version)
        val b = guavaNumbers(guavaFixed)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val version = requested.version
            if (requested.group == "com.google.guava" && requested.name == "guava" &&
                version != null && guavaOlder(version)
            ) {
                val variant = if (version.endsWith("-android")) "android" else "jre"
                useVersion(guavaFixed.substringBefore('-') + "-" + variant)
                because("Sicherheitslücke in älteren Guava-Versionen (Dependabot #10)")
            }
        }
    }
}
