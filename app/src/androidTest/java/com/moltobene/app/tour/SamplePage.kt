package com.moltobene.app.tour

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeIds
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.share.RecipeJsonLd
import com.moltobene.app.data.web.PageLoader
import com.moltobene.app.data.web.WebPage
import java.io.File

/**
 * Nachgestellte Rezeptseite für „Aus Link übernehmen“ im Rundgang (#55): selbst geschrieben, wie ein Koch-Portal
 * mit eingebetteten Rezeptdaten. Der Rundgang hängt so nicht vom echten Internet ab.
 */
object SamplePage {
    const val URL = "https://rezepte.example/zitronen-risotto?utm_source=rundgang"
    const val CLEAN_URL = "https://rezepte.example/zitronen-risotto"
    const val TITLE = "Zitronen-Risotto"

    private val HTML = """
        <!doctype html>
        <html lang="de"><head>
          <title>Zitronen-Risotto | Beispiel-Kochportal</title>
          <meta property="og:site_name" content="Beispiel-Kochportal">
          <script type="application/ld+json">
          {"@context":"https://schema.org","@graph":[
            {"@type":"WebSite","name":"Beispiel-Kochportal"},
            {"@type":"Recipe","name":"Zitronen-Risotto","inLanguage":"de",
             "description":"Cremig, frisch und in einer halben Stunde fertig.",
             "recipeYield":["4","4 Portionen"],"prepTime":"PT10M","totalTime":"PT35M",
             "image":{"@type":"ImageObject","url":"/bilder/zitronen-risotto.jpg","width":1200},
             "recipeIngredient":["320 g Risottoreis","1 Zwiebel","1 l Gemüsebrühe","1 Bio-Zitrone","50 g Parmesan"],
             "recipeInstructions":[
               {"@type":"HowToStep","text":"Zwiebel fein würfeln und in Olivenöl glasig dünsten."},
               {"@type":"HowToStep","text":"Reis dazugeben und unter Rühren nach und nach die Brühe angießen."},
               {"@type":"HowToStep","text":"Zitronenschale und Parmesan unterrühren."}]}
          ]}
          </script>
        </head><body><article><h1>Zitronen-Risotto</h1><p>Eine lange Geschichte vor dem Rezept …</p></article></body></html>
    """.trimIndent()

    /** Geteilter Link zu einem Video, wie ihn die YouTube-App weitergibt – mit „si=…“ zum Verfolgen der Weitergabe. */
    const val VIDEO_URL = "https://youtu.be/AbCdEfGhIjK?si=rundgang"
    const val VIDEO_WATCH_URL = "https://www.youtube.com/watch?v=AbCdEfGhIjK"
    /** Die Links aus der Beschreibung, wie die Auswahl sie zeigt: ohne „https://“, Startseiten zuletzt. */
    const val VIDEO_RECIPE_LINK = "rezepte.example/zitronen-risotto"
    const val VIDEO_BLOG_LINK = "blog.rezepte.example"
    const val VIDEO_INGREDIENT = "1 Bio-Zitrone"

    /**
     * Nachgestellte YouTube-Videoseite (selbst geschrieben, nach dem Aufbau echter Videoseiten): In der Beschreibung
     * stehen nur die Zutaten, ein Link zum ganzen Rezept – der führt zur nachgestellten Rezeptseite oben –, die
     * Startseite eines Blogs und Instagram.
     */
    private val VIDEO_HTML = """
        <!doctype html>
        <html lang="de"><head>
          <title>Zitronen-Risotto - YouTube</title>
          <meta property="og:image" content="https://i.ytimg.com/vi/AbCdEfGhIjK/maxresdefault.jpg">
        </head><body>
          <script>var ytInitialPlayerResponse = null;</script>
          <script>var ytInitialPlayerResponse = {"playabilityStatus":{"status":"OK"},"videoDetails":{"videoId":"AbCdEfGhIjK","title":"Zitronen-Risotto mit Parmesan 🍋 #shorts","author":"Beispielküche","shortDescription":"Cremig und frisch in einer halben Stunde.\n\n0:00 Intro\n0:20 Kochen\n\nZutaten für 4 Portionen:\n320 g Risottoreis\n1 Bio-Zitrone\n50 g Parmesan\n\nMehr von uns: https://blog.rezepte.example/\nDas ganze Rezept mit allen Schritten: https://rezepte.example/zitronen-risotto\n\nFolge uns auf Instagram: https://instagram.com/beispielkueche\n\n#risotto #rundgang"}};</script>
        </body></html>
    """.trimIndent()

    /**
     * Lader, der statt ins Internet zu gehen die nachgestellten Seiten und ein gezeichnetes Foto liefert:
     * für YouTube die Videoseite, sonst die Rezeptseite.
     */
    fun loader(context: Context): PageLoader = object : PageLoader {
        override suspend fun loadPage(url: String): WebPage =
            if (url.contains("youtube.com")) {
                WebPage(url, VIDEO_HTML.toByteArray(), "UTF-8")
            } else {
                WebPage(CLEAN_URL, HTML.toByteArray(), "UTF-8")
            }

        override suspend fun loadImage(url: String, target: File) = SampleRecipes.drawPagePhoto(context, target)
    }

    const val FILE_TITLE = "Gemüse-Curry"
    const val FILE_INGREDIENT = "400 g Blumenkohl"

    /**
     * Rezeptdatei für „Aus Datei übernehmen“ (#54), genau so, wie Moltobene sie mit „Als Rezeptdatei teilen“ schreibt –
     * mit eingebettetem, gezeichnetem Foto. Sie liegt im Download-Ordner, also wie bei WhatsApp oder E-Mail bei
     * einer anderen App (content://media/…).
     */
    fun recipeFile(context: Context): Uri {
        val photo = File(context.cacheDir, "rezeptdatei-foto-${System.nanoTime()}.jpg")
        SampleRecipes.drawPagePhoto(context, photo)
        val recipe = Recipe(
            id = RecipeIds.newId(),
            title = FILE_TITLE,
            language = "de",
            servings = 3,
            prepMinutes = 15,
            totalMinutes = 40,
            source = RecipeSource(SourceType.BOOK, name = "Familienkochbuch", page = "12"),
            ingredients = listOf(Ingredient(FILE_INGREDIENT), Ingredient("1 Dose Kokosmilch"), Ingredient("2 EL Currypaste")),
            steps = listOf("Gemüse klein schneiden.", "Mit Currypaste anbraten.", "Kokosmilch angießen und köcheln lassen."),
            createdAt = 0,
            updatedAt = 0,
        )
        val json = try {
            RecipeJsonLd.build(recipe, untitled = FILE_TITLE, photoJpeg = photo.readBytes()) { "S. $it" }
        } finally {
            photo.delete()
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "gemuese-curry-${System.nanoTime()}.json")
            put(MediaStore.Downloads.MIME_TYPE, RecipeJsonLd.MIME_TYPE)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "Download-Ordner nicht beschreibbar" }
        requireNotNull(resolver.openOutputStream(uri)).use { it.write(json.toByteArray()) }
        return uri
    }

    fun deleteRecipeFile(context: Context, uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }
}
