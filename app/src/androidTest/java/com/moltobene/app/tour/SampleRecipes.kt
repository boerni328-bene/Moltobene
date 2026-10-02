package com.moltobene.app.tour

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import com.moltobene.app.AppContainer
import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeIds
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Beispielrezepte für den Rundgang: selbst geschrieben, in verschiedenen Sprachen, mit und ohne Foto,
 * mit Entwurf und sehr langem Titel. Die Fotos werden gezeichnet – keine fremden Inhalte im Repository.
 */
object SampleRecipes {
    const val TOMATO_SAUCE = "Tomatensoße mit Basilikum"

    /** Steht in der Sammlung weit oben – auch mit 200 % Schrift ohne Blättern zu sehen. */
    const val POTATO_SALAD = "Kartoffelsalat mit Gurke"
    const val LENTIL_SOUP = "Linsensuppe"

    /** Speichert alle auf einmal, damit die Sammlung sie gemeinsam anzeigt (wie nach dem Wiederherstellen). */
    suspend fun addTo(container: AppContainer, context: Context) {
        val now = System.currentTimeMillis()
        val recipes = samples().mapIndexed { index, sample ->
            val photoIds = sample.photo?.let { colors ->
                val file = drawPhoto(context, colors)
                try {
                    listOf(container.photoStore.importFromUri(Uri.fromFile(file)))
                } finally {
                    file.delete()
                }
            }.orEmpty()
            val time = now - index
            sample.recipe.copy(photoIds = photoIds, createdAt = time, updatedAt = time)
        }
        container.repository.saveAll(recipes)
    }

    private class Sample(val recipe: Recipe, val photo: PhotoColors? = null)

    /** Farben des gezeichneten Fotos: Tisch (Verlauf), Gericht und Beilage. */
    private class PhotoColors(val tableStart: Long, val tableEnd: Long, val food: Long, val garnish: Long)

    private fun recipe(
        title: String,
        language: String,
        servings: Int,
        ingredients: List<String>,
        steps: List<String>,
        servingsUnit: String? = null,
        source: RecipeSource? = null,
        notes: String = "",
        isDraft: Boolean = false,
    ) = Recipe(
        id = RecipeIds.newId(),
        title = title,
        language = language,
        servings = servings,
        servingsUnit = servingsUnit,
        source = source,
        notes = notes,
        isDraft = isDraft,
        // Zeilen mit Doppelpunkt am Ende sind Zwischenüberschriften, wie beim Eintippen.
        ingredients = ingredients.map { Ingredient(text = it, isHeading = it.endsWith(":")) },
        steps = steps,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun samples() = listOf(
        Sample(
            recipe(
                title = TOMATO_SAUCE,
                language = "de",
                servings = 4,
                source = RecipeSource(SourceType.BOOK, name = "Omas Kochbuch", page = "47"),
                ingredients = listOf(
                    "800 g reife Tomaten", "1 Zwiebel", "2 Knoblauchzehen", "3 EL Olivenöl", "1 Bund Basilikum",
                    "Salz, Pfeffer",
                ),
                steps = listOf(
                    "Zwiebel und Knoblauch fein hacken und im Olivenöl glasig dünsten.",
                    "Tomaten würfeln, dazugeben und 20 Minuten offen köcheln lassen.",
                    "Basilikum zupfen, unterrühren und mit Salz und Pfeffer abschmecken.",
                ),
                notes = "Passt zu Spaghetti und Gnocchi. Hält sich im Kühlschrank drei Tage.",
            ),
            PhotoColors(0xFF8D6E63, 0xFF4E342E, food = 0xFFC62828, garnish = 0xFF2E7D32),
        ),
        Sample(
            recipe(
                title = "Pfannkuchen",
                language = "de",
                servings = 8,
                servingsUnit = "Stück",
                source = RecipeSource(SourceType.PERSON, name = "Tante Ute"),
                ingredients = listOf(
                    "Für den Teig:", "250 g Mehl", "500 ml Milch", "3 Eier", "1 Prise Salz", "Zum Ausbacken:", "Butter",
                ),
                steps = listOf(
                    "Mehl, Milch, Eier und Salz glatt rühren und 15 Minuten ruhen lassen.",
                    "Etwas Butter in einer Pfanne erhitzen und eine Kelle Teig dünn verteilen.",
                    "Von beiden Seiten goldbraun backen.",
                ),
            ),
            PhotoColors(0xFF90A4AE, 0xFF455A64, food = 0xFFF2C14E, garnish = 0xFF6D4C41),
        ),
        Sample(
            recipe(
                title = POTATO_SALAD,
                language = "de",
                servings = 4,
                source = RecipeSource(SourceType.WEB, url = "https://example.org/rezepte/kartoffelsalat"),
                ingredients = listOf(
                    "1 kg festkochende Kartoffeln", "1 Salatgurke", "1 rote Zwiebel", "150 ml Gemüsebrühe",
                    "3 EL Weißweinessig", "4 EL Öl", "1 TL Senf", "Dill",
                ),
                steps = listOf(
                    "Kartoffeln kochen, pellen und noch warm in Scheiben schneiden.",
                    "Brühe erhitzen, mit Essig, Öl und Senf verrühren und über die Kartoffeln gießen.",
                    "Gurke hobeln, Zwiebel würfeln, alles mischen und mindestens eine Stunde ziehen lassen.",
                ),
            ),
        ),
        Sample(
            recipe(
                title = "Risotto ai funghi",
                language = "it",
                servings = 2,
                source = RecipeSource(SourceType.BOOK, name = "Cucina di casa", page = "112"),
                ingredients = listOf(
                    "160 g di riso Carnaroli", "250 g di funghi champignon", "1 scalogno", "700 ml di brodo vegetale",
                    "50 g di parmigiano grattugiato", "30 g di burro",
                ),
                steps = listOf(
                    "Rosolare lo scalogno nel burro, aggiungere i funghi e cuocere per 5 minuti.",
                    "Tostare il riso, poi aggiungere il brodo caldo poco alla volta, mescolando.",
                    "Dopo circa 18 minuti mantecare con il parmigiano e il burro rimasto.",
                ),
            ),
            PhotoColors(0xFF6D4C41, 0xFF3E2723, food = 0xFFF3E5AB, garnish = 0xFF8D6E63),
        ),
        Sample(
            recipe(
                title = "Lemon drizzle cake",
                language = "en",
                servings = 1,
                servingsUnit = "loaf tin",
                ingredients = listOf(
                    "200 g soft butter", "200 g sugar", "3 eggs", "200 g self-raising flour", "2 tbsp milk",
                    "zest of 2 lemons", "For the drizzle:", "juice of 2 lemons", "100 g icing sugar",
                ),
                steps = listOf(
                    "Heat the oven to 180 °C and line a loaf tin with baking paper.",
                    "Beat butter and sugar until pale, then add the eggs one by one.",
                    "Fold in flour, milk and lemon zest, fill the tin and bake for about 50 minutes.",
                    "Stir lemon juice and icing sugar together and pour over the warm cake.",
                ),
            ),
            PhotoColors(0xFFECEFF1, 0xFFB0BEC5, food = 0xFFFFE082, garnish = 0xFFF9A825),
        ),
        Sample(
            recipe(
                title = LENTIL_SOUP,
                language = "de",
                servings = 4,
                isDraft = true,
                ingredients = listOf(
                    "250 g Tellerlinsen", "1 Bund Suppengemüse", "2 Kartoffeln", "1,5 l Gemüsebrühe", "2 EL Essig",
                ),
                steps = listOf(
                    "Suppengemüse und Kartoffeln würfeln.",
                    "Mit Linsen und Brühe 30 Minuten köcheln lassen.",
                    "Mit Essig abschmecken.",
                ),
            ),
        ),
        Sample(
            recipe(
                title = "Ofengemüse mit Feta",
                language = "de",
                servings = 3,
                ingredients = listOf(
                    "2 Paprika", "1 Zucchini", "1 rote Zwiebel", "400 g Kürbis", "200 g Feta", "3 EL Olivenöl",
                    "1 TL Thymian",
                ),
                steps = listOf(
                    "Backofen auf 200 °C vorheizen.",
                    "Gemüse in mundgerechte Stücke schneiden und mit Öl und Thymian mischen.",
                    "Auf einem Blech 25 Minuten rösten, Feta darüberbröseln und weitere 10 Minuten backen.",
                ),
                notes = "Schmeckt am nächsten Tag kalt als Salat.",
            ),
            PhotoColors(0xFF37474F, 0xFF263238, food = 0xFFEF6C00, garnish = 0xFF558B2F),
        ),
        Sample(
            recipe(
                title = "Gefüllte Paprika mit Reis, Hackfleisch und würziger Tomaten-Kräuter-Soße",
                language = "de",
                servings = 4,
                ingredients = listOf(
                    "4 Paprika", "125 g Reis", "400 g Hackfleisch", "1 Zwiebel", "400 g passierte Tomaten",
                    "Kräuter nach Wahl",
                ),
                steps = listOf(
                    "Reis vorkochen.",
                    "Hackfleisch mit der Zwiebel anbraten, mit dem Reis mischen und in die ausgehöhlten Paprika füllen.",
                    "In der Tomatensoße bei 180 °C etwa 40 Minuten schmoren.",
                ),
            ),
        ),
    )

    /** Zeichnet ein einfaches „Foto“: Teller mit Gericht auf einem Tisch. */
    private fun drawPhoto(context: Context, colors: PhotoColors): File {
        val width = 1200f
        val height = 900f
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, width, height, colors.tableStart.toInt(), colors.tableEnd.toInt(), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null

        val cx = width / 2
        val cy = height / 2
        paint.color = 0x55000000
        canvas.drawCircle(cx + 14f, cy + 18f, 360f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, 360f, paint)
        paint.color = colors.food.toInt()
        canvas.drawCircle(cx, cy, 260f, paint)
        paint.color = colors.garnish.toInt()
        repeat(7) { i ->
            val angle = i * 2 * PI / 7
            canvas.drawCircle(cx + 150f * cos(angle).toFloat(), cy + 150f * sin(angle).toFloat(), 36f, paint)
        }

        val file = File(context.cacheDir, "beispielfoto-${System.nanoTime()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }
}
