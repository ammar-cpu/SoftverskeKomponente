package rs.raf.sk.resolver

import rs.raf.sk.model.ComponentId

/**
 * Jedna verzija komponente pronađena pri obilasku stabla zavisnosti.
 *
 * @param id komponenta u konkretnoj verziji
 * @param depth udaljenost od korena; komponente sa spiska imaju dubinu 1
 * @param order redni broj nailaska u obilasku, počev od 0
 */
data class Candidate(
    val id: ComponentId,
    val depth: Int,
    val order: Int
)
