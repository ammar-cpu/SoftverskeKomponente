package rs.raf.sk.resolver

import rs.raf.sk.model.ComponentId

/**
 * Pravilo kojim se bira jedna verzija kada se ista komponenta (ista organizacija i projekat)
 * u stablu zavisnosti pojavi u dve različite verzije.
 */
interface ConflictStrategy {

    /**
     * Bira koja od dve verzije iste komponente ostaje.
     *
     * @param existing verzija koja je već izabrana ranije
     * @param existingDepth dubina na kojoj je pronađena `existing` (komponente sa spiska imaju dubinu 1)
     * @param candidate verzija na koju se upravo naišlo
     * @param candidateDepth dubina na kojoj je pronađena `candidate`
     * @return izabrana verzija, `existing` ili `candidate`
     * @throws rs.raf.sk.model.ValidationException ako neka verzija nema ispravan format
     */
    fun choose(
        existing: ComponentId,
        existingDepth: Int,
        candidate: ComponentId,
        candidateDepth: Int
    ): ComponentId
}
