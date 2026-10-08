package com.moltobene.app.data.web

import com.moltobene.app.data.ocr.AreaKind
import com.moltobene.app.data.ocr.RecipeTextParser
import com.moltobene.app.data.ocr.TextLanguage
import java.net.URL

/**
 * Rezept aus einer Videobeschreibung. Dort steht das Rezept zwischen Werbung, Links, Kapitelmarken und
 * Schlagwörtern – oft nur die Zutaten, manchmal gar nichts, manchmal Links zum Rezept auf einer Internetseite.
 * Deshalb wird vorsichtiger gelesen als bei „Aus Text übernehmen“: Zutaten nur nach einer Überschrift oder als
 * eindeutige Liste, Schritte nur nach einer Überschrift. Reines Kotlin, per Unit-Test prüfbar.
 */
object VideoDescription {

    /** Das Rezept aus der Beschreibung; null, wenn darin keines erkennbar ist. */
    fun recipe(video: YouTube.Video): WebRecipe? {
        val lines = recipeLines(video.description)
        if (lines.none { it.isNotEmpty() }) return null
        val text = lines.joinToString("\n")
        val language = TextLanguage.detect(text)
        val whole = RecipeTextParser.parse(text, language, typed = true)

        val ingredientsAt = lines.indexOfFirst { RecipeTextParser.isIngredientHeading(it) }
        val stepsAt = lines.indexOfFirst { RecipeTextParser.isStepsHeading(it) }
        val ingredients = when {
            ingredientsAt >= 0 -> {
                val end = if (stepsAt > ingredientsAt) stepsAt else listEnd(lines, ingredientsAt + 1)
                val section = lines.subList(ingredientsAt + 1, end)
                    .filterNot { RecipeTextParser.isIngredientHeading(it) }
                    .map(::asSubheading)
                RecipeTextParser.parseSection(AreaKind.INGREDIENTS, section.joinToString("\n"), language).ingredients
            }
            // Ohne Überschrift nur eine eindeutige Liste wie „12 Lasagneplatten“, „2 Kugeln Mozzarella“ …
            else -> whole.ingredients.takeIf { list -> list.count { AMOUNT_FIRST.containsMatchIn(it) } >= MIN_INGREDIENTS }.orEmpty()
        }
        // Erzählender Text ist in Beschreibungen selten eine Anleitung: Schritte nur nach einer Überschrift.
        val steps = if (stepsAt >= 0) {
            val end = if (ingredientsAt > stepsAt) ingredientsAt else lines.size
            RecipeTextParser.parse(lines.subList(stepsAt, end).joinToString("\n"), language, typed = true).steps
        } else {
            emptyList()
        }
        if (ingredients.size < MIN_INGREDIENTS && steps.isEmpty()) return null
        return WebRecipe(
            title = video.title,
            servings = whole.servings,
            servingsUnit = whole.servingsUnit,
            ingredients = ingredients,
            steps = steps,
            imageUrls = listOfNotNull(video.imageUrl),
            language = language,
            url = YouTube.watchUrl(video.id),
        )
    }

    /**
     * Alle Links der Beschreibung, die zu einem Rezept führen können – zur Auswahl, der wahrscheinlichste zuerst:
     * Rezept-Wörter im Link („/rezept/…“), dann davor („Das ganze Rezept: https://…“), Startseiten zuletzt.
     * Nicht: soziale Netzwerke, Shops, Werbung und andere Videos, denn dort steht nie das Rezept.
     */
    fun recipeLinks(description: String): List<String> {
        val lines = description.replace("\r", "").lines()
        val found = mutableListOf<Pair<String, Int>>()
        lines.forEachIndexed { index, line ->
            // Auch der eigene Blog wird manchmal als „Werbung“ markiert: Mit einem Rezept-Wort zählt die Zeile.
            if (ADVERTISING.containsMatchIn(line) && !RECIPE_WORD.containsMatchIn(line)) return@forEachIndexed
            for (match in LINK.findAll(line)) {
                val raw = match.value.trimEnd('.', ',', ')', '!', ';', ':', '»', '“', '"')
                val url = WebAddress.normalize(if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw) ?: continue
                val parsed = runCatching { URL(url) }.getOrNull() ?: continue
                val host = parsed.host.removePrefix("www.")
                if (EXCLUDED_HOSTS.any { host == it || host.endsWith(".$it") } || EXCLUDED_PREFIXES.any { host.startsWith(it) }) continue
                if (SHOP_NAME.containsMatchIn(host)) continue
                val around = (line.replace(match.value, " ") + " " + lines.getOrElse(index - 1) { "" })
                val score = when {
                    parsed.path.orEmpty().trim('/').isEmpty() && parsed.query == null -> -1
                    RECIPE_WORD.containsMatchIn(host + parsed.path) -> 2
                    RECIPE_WORD.containsMatchIn(around) || RECIPE_CONTEXT.containsMatchIn(around) -> 1
                    else -> 0
                }
                if (found.none { it.first == url }) found += url to score
            }
        }
        // sortedByDescending ist stabil: Bei gleicher Wertung bleibt die Reihenfolge der Beschreibung.
        return found.sortedByDescending { it.second }.map { it.first }.take(MAX_LINKS)
    }

    /**
     * Die Zeilen der Beschreibung ohne Beiwerk; Leerzeilen trennen die Absätze. Ein Absatz mit Link oder Werbung
     * fällt ganz weg – außer er enthält eine Überschrift wie „Zutaten“, dann nur die betroffenen Zeilen.
     */
    internal fun recipeLines(description: String): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in description.replace("\r", "").split(BLANK_LINE)) {
            val lines = paragraph.lines().map { withoutPictographs(it) }.filter { it.isNotEmpty() }
            val hasHeading = lines.any { RecipeTextParser.isIngredientHeading(it) || RecipeTextParser.isStepsHeading(it) }
            if (!hasHeading && lines.any(::isPromotion)) continue
            val kept = lines.filterNot { isPromotion(it) || CHAPTER.containsMatchIn(it) || isTagLine(it) }
            if (kept.isEmpty()) continue
            if (result.isNotEmpty()) result += ""
            result += kept
        }
        return result
    }

    /** Bildzeichen (Emojis, Sterne, Pfeile wie „►“) entfernt; „°“, „½“ oder „⌀“ bleiben. */
    internal fun withoutPictographs(text: String): String =
        text.replace(PICTOGRAPHS, " ").replace(SPACES, " ").trim()

    /**
     * Wo eine Liste von Zutaten ohne Überschrift für die Zubereitung endet: am ersten Absatz mit erzählendem Text
     * (lange Zeilen oder ganze Sätze), z. B. „Probiert es aus und lasst es euch schmecken!“.
     */
    private fun listEnd(lines: List<String>, from: Int): Int {
        var index = from
        var seenList = false
        while (index < lines.size) {
            val start = index
            while (index < lines.size && lines[index].isNotEmpty()) index++
            val paragraph = lines.subList(start, index)
            if (paragraph.isNotEmpty()) {
                val prose = paragraph.any { it.length > LIST_LINE || SENTENCE.containsMatchIn(it) }
                if (prose && seenList) return start
                if (!prose) seenList = true
            }
            index++
        }
        return lines.size
    }

    /** „Rezept 2: Pad Kra Pao“ in einer Beschreibung mit mehreren Rezepten wird zur Zwischenüberschrift. */
    private fun asSubheading(line: String): String =
        if (RECIPE_NUMBER.containsMatchIn(line) && !line.endsWith(":")) "$line:" else line

    private fun isPromotion(line: String): Boolean = LINK.containsMatchIn(line) || PROMOTION.containsMatchIn(line)

    /** Zeilen nur aus Schlagwörtern (#…) oder Erwähnungen (@…). */
    private fun isTagLine(line: String): Boolean =
        line.split(SPACES).filter { it.isNotEmpty() }.all { it.startsWith("#") || it.startsWith("@") }

    private const val MIN_INGREDIENTS = 2
    /** Zeilen einer Zutatenliste sind kurz; längere sind erzählender Text. */
    private const val LIST_LINE = 70

    private val BLANK_LINE = Regex("\\n\\s*\\n")
    private val SPACES = Regex("[\\s\\u00A0]+")
    private val SENTENCE = Regex("\\S+(\\s+\\S+){6,}[.!?]$")
    private val AMOUNT_FIRST = Regex("^(\\d|[½¼¾⅓⅔⅛])")
    private val CHAPTER = Regex("^\\(?\\d{1,2}:\\d{2}(:\\d{2})?\\)?(\\s|$)")
    private val RECIPE_NUMBER = Regex("^(rezept|recipe|ricetta|recette|receta)\\s*\\d{1,2}\\s*[:.–-]", RegexOption.IGNORE_CASE)
    private val LINK = Regex("(https?://|\\bwww\\.)[^\\s<>\"]+|\\b(amzn\\.to|amzlink\\.to|bit\\.ly|po\\.st)/\\S*", RegexOption.IGNORE_CASE)

    /** Werbung, Hinweise auf Kanal, Abo und soziale Netzwerke – in den Sprachen der Texterkennung. */
    private val PROMOTION = Regex(
        "(abonn|subscri|iscriv|suscr[ií]b|newsletter|affiliate|provision|commissione|werbung|anzeige|sponsor|#adv?\\b|" +
            "instagram|facebook|tiktok|pinterest|twitter|snapchat|patreon|amazon|rabattcode|gutschein|discount code|" +
            "codice sconto|code promo|promo code|glocke|campanella|notification bell|kommentier|comment below|commenta|" +
            "markiert gerne|tag a friend|tagga|folgt mir|folge uns|follow me|follow us|seguimi|seguici|suivez|s[ií]gueme|" +
            "\\bkanal\\b|\\bchannel\\b|\\bcanale\\b|\\bcha[iî]ne\\b|playlist|merch)",
        RegexOption.IGNORE_CASE,
    )

    /** Rezept-Wörter in einem Link oder davor: „/rezept/…“, „ricette.…“, „Full recipe here:“. */
    private val RECIPE_WORD = Regex("(rezept|recipe|ricett|recette|receta|recept)", RegexOption.IGNORE_CASE)
    private val RECIPE_CONTEXT = Regex("(zutaten|zubereitung|anleitung|ingredient|procedimento|preparazione|préparation|preparación)", RegexOption.IGNORE_CASE)

    /** Soziale Netzwerke, Video- und Musikdienste, Linksammlungen, Shops und ihre Kurzlinks. */
    private val EXCLUDED_HOSTS = listOf(
        "youtube.com", "youtu.be", "youtube-nocookie.com", "facebook.com", "fb.com", "fb.me", "instagram.com",
        "tiktok.com", "twitter.com", "x.com", "t.co", "threads.net", "bsky.app", "snapchat.com", "patreon.com",
        "linktr.ee", "linkin.bio", "beacons.ai", "spotify.com", "apple.com", "twitch.tv", "discord.gg", "discord.com",
        "whatsapp.com", "wa.me", "t.me", "telegram.me", "linkedin.com", "reddit.com", "vk.com", "steadyhq.com",
        "paypal.com", "paypal.me", "ko-fi.com", "buymeacoffee.com", "amzn.to", "amzn.eu", "amzlink.to", "geni.us",
        "etsy.com", "awin1.com", "rstyle.me", "liketk.it", "shopstyle.com", "po.st",
    )
    private val EXCLUDED_PREFIXES = listOf("pinterest.", "amazon.", "ebay.", "music.youtube.")

    /** Shops am Namen erkannt, z. B. „shop.example.de“ oder „example-store.com“. */
    private val SHOP_NAME = Regex("(^|[.-])(shop|store|merch)[a-z]*\\.|(shop|store)\\.[a-z]+$", RegexOption.IGNORE_CASE)

    /** Werbung in derselben Zeile: „Meine Pfanne (Werbung): https://…“, „Rabattcode …“. */
    private val ADVERTISING = Regex(
        "(werbung|anzeige|affiliate|partnerlink|provision|rabatt|gutschein|discount|promo|sponsor|codice sconto|" +
            "\\bcode\\b|\\bshop\\b|merch)",
        RegexOption.IGNORE_CASE,
    )

    private const val MAX_LINKS = 10

    /** Emojis und Bildzeichen, aber nicht „⌀“ (U+2300) oder Pfeile, die in Rezepten vorkommen. */
    private val PICTOGRAPHS = Regex(
        "[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{2B00}-\\x{2BFF}\\x{25A0}-\\x{25FF}\\x{231A}\\x{231B}" +
            "\\x{23E9}-\\x{23F3}\\x{23F8}-\\x{23FA}\\x{FE0E}\\x{FE0F}\\x{200D}\\x{20E3}\\x{E0020}-\\x{E007F}]",
    )
}
