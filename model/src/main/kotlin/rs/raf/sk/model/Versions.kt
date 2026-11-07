package rs.raf.sk.model

/**
 * Proverava da li verzija ima ispravan format:
 * samo brojevi razdvojeni tackama, npr. "1", "1.2", "1.2.0"
 */
fun isValidVersion(version: String): Boolean {
    val parts = version.split(".")
    return parts.all { part -> part.isNotEmpty() && part.all { it.isDigit() } }
}

/**
 * Poredi dve verzije deo po deo, kao brojeve.
 * Vraća negativan broj ako je version1 starija, 0 ako su iste, pozitivan ako je novija.
 * Deo koji fali se tretira kao 0, pa su "1.2" i "1.2.0" iste.
 */
fun compareVersions(version1: String, version2: String): Int {

    val p1 = toNumbers(version1)
    val p2 = toNumbers(version2)

    val size = maxOf(p1.size, p2.size)

    for (i in 0 until size) {
        val x = p1.getOrElse(i) { 0 }
        val y = p2.getOrElse(i) { 0 }

        if (x != y) {
            return x - y
        }
    }

    return 0
}

/**
 * Vraca noviju verziju od dve verzije. Ako su iste, vraca prvu.
 */
fun newerVersion(version1: String, version2: String): String =
    if (compareVersions(version1, version2) >= 0) version1 else version2


private fun toNumbers(version: String): List<Int> {
    if (!isValidVersion(version)) {
        throw ValidationException("Invalid version: $version")
    }

    return version.split(".").map{ it.toInt() }
}
