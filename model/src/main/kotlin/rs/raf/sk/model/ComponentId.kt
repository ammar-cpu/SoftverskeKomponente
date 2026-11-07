package rs.raf.sk.model

/**
 * Jedinstvena oznaka komponente: organizacija, projekat i verzija.
 * Tekstualni oblik je "organizacija:projekat:verzija", npr. "rs.raf:utils:2.1.0".
 * Nijedan deo ne sme biti prazan niti sadržati ':' ili '/'.
 *
 * @throws ValidationException ako neki deo nije ispravan
 */
data class ComponentId (
    val organization: String,
    val project: String,
    val version: String
) {
    init {
        checkPart("organization", organization)
        checkPart("project", project)
        checkPart("version", version)
    }

    private fun checkPart(name: String, value: String) {
        if (value.isBlank() || value.contains(":") || value.contains("/")) {
            throw ValidationException("Invalid $name: '$value'")
        }
    }

    /** Vraća oznaku u obliku "organizacija:projekat:verzija". */
    override fun toString() = "$organization:$project:$version"

    companion object {
        /**
         * Pravi oznaku od teksta oblika "organizacija:projekat:verzija".
         * Razmaci oko delova se ignorišu.
         *
         * @throws ValidationException ako tekst nema tačno tri dela ili neki deo nije ispravan
         */
        fun parse(text: String): ComponentId {
            val parts = text.split(":")

            if (parts.size != 3) {
                throw ValidationException("ID needs to have 3 parts (organization:project:version), but was '$text'")
            }

            val organization = parts[0].trim()
            val project = parts[1].trim()
            val version = parts[2].trim()

            return ComponentId(organization, project, version)
        }
    }
}