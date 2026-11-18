package rs.raf.sk.resolver

import rs.raf.sk.model.ValidationException
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// readDependencyFile po dizajnu čita lokalni fajl, pa ovde koristimo pravi privremeni fajl
class DependencyFileTest {

    private val file = File.createTempFile("spisak", ".txt")

    @AfterTest
    fun deleteFile() {
        file.delete()
    }

    @Test
    fun skipsBlankLinesAndComments() {
        file.writeText(
            """
            # komentar

               rs.raf:app:1.0.0
                # uvučen komentar
            rs.raf:utils:2.0.0
            """.trimIndent()
        )

        val result = DependencyResolver.readDependencyFile(file)

        assertEquals(ids("rs.raf:app:1.0.0", "rs.raf:utils:2.0.0"), result)
    }

    @Test
    fun rejectsInvalidLineWithLineNumber() {
        file.writeText("rs.raf:app:1.0.0\n\nrs.raf:utils\n")

        val e = assertFailsWith<ValidationException> {
            DependencyResolver.readDependencyFile(file)
        }
        assertTrue("line 3" in e.message!!)
    }
}
