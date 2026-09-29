package com.moltobene.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp

// Schrift: die Standardschrift von Android in der Material-3-Größenskala.
// Kein eigener Schriftsatz – spart Speicher und passt zu „modern und schlicht“.
// Schriftgrößen immer über MaterialTheme.typography, damit sie mit der Systemeinstellung „Schriftgröße“ mitwachsen.
val MoltobeneTypography = Typography()

// Ecken: dezent gerundet, einheitlich für Karten, Felder und Schaltflächen.
val MoltobeneShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// Abstände: feste Stufen statt beliebiger Werte.
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
}
