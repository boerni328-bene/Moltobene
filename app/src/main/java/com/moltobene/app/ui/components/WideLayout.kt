package com.moltobene.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ab dieser Breite (dp) stehen Zutaten und Zubereitung nebeneinander (Tablet, Handy quer; #91). */
const val WIDE_LAYOUT_MIN_DP = 720

/** Größte Breite (dp) für Texte und Formulare; breitere Fenster lassen links und rechts Platz. */
val READING_WIDTH: Dp = 720.dp

/** Ist das Fenster breit genug für zwei Spalten? Gilt für das Fenster, nicht für den Bildschirm (Mehrfenster, Falt-Handy). */
@Composable
fun isWideLayout(): Boolean = LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_MIN_DP

/**
 * Hält den Inhalt auf lesbarer Breite und setzt ihn mittig (#91). Nach `fillMaxSize()` anzuhängen; auf schmalen
 * Bildschirmen ändert sich nichts.
 */
fun Modifier.readingWidth(max: Dp = READING_WIDTH): Modifier =
    wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max).fillMaxWidth()
