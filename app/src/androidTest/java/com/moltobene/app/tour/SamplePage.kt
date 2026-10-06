package com.moltobene.app.tour

import android.content.Context
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

    /** Lader, der statt ins Internet zu gehen die nachgestellte Seite und ein gezeichnetes Foto liefert. */
    fun loader(context: Context): PageLoader = object : PageLoader {
        override suspend fun loadPage(url: String): WebPage = WebPage(CLEAN_URL, HTML.toByteArray(), "UTF-8")

        override suspend fun loadImage(url: String, target: File) = SampleRecipes.drawPagePhoto(context, target)
    }
}
