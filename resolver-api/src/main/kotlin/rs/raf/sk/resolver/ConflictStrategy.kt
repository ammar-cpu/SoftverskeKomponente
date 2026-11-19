package rs.raf.sk.resolver

import rs.raf.sk.model.ValidationException

/**
 * Pravilo kojim se bira jedna verzija kada se ista komponenta (ista organizacija i projekat)
 * u stablu zavisnosti pojavi u više verzija.
 */
interface ConflictStrategy {

    /**
     * Od svih pronađenih verzija istog projekta bira jednu.
     *
     * @param candidates sve pronađene verzije jednog projekta; lista nije prazna
     * @return izabrana verzija
     * @throws ValidationException ako neka verzija nema ispravan format
     */
    fun choose(candidates: List<Candidate>): Candidate

    companion object {
        /**
         * Vraća strategiju po imenu.
         *
         * @param name "newest" ili "nearest"
         * @return strategija sa tim imenom
         * @throws ValidationException ako strategija sa tim imenom ne postoji
         */
        @JvmStatic
        fun byName(name: String): ConflictStrategy = when (name) {
            "newest" -> NewestVersionStrategy
            "nearest" -> NearestWinsStrategy
            else -> throw ValidationException("Unknown strategy: $name (expected newest or nearest)")
        }
    }
}
