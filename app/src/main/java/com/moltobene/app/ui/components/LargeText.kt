package com.moltobene.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/** Ab dieser Schriftgröße (Einstellung des Handys) stehen Schaltflächen untereinander statt nebeneinander (#53). */
private const val LARGE_FONT_SCALE = 1.5f

/** Ist die Schrift des Handys groß eingestellt? Dann brauchen Texte und Schaltflächen mehr Breite. */
@Composable
fun isLargeText(): Boolean = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
