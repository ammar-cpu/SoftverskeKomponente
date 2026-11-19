package rs.raf.sk.resolver

import rs.raf.sk.model.compareVersions

/**
 * Bira najnoviju verziju. Ako su dve verzije iste (npr. "1.2" i "1.2.0"),
 * bira onu koja je prva pronađena.
 */
object NewestVersionStrategy : ConflictStrategy {

    /**
     * Bira najnoviju verziju projekta.
     *
     * @param candidates sve pronađene verzije jednog projekta; lista nije prazna
     * @return kandidat sa najnovijom verzijom
     * @throws rs.raf.sk.model.ValidationException ako neka verzija nema ispravan format
     */
    override fun choose(candidates: List<Candidate>): Candidate {
        var best = candidates.first()
        for (candidate in candidates) {
            val comparison = compareVersions(candidate.id.version, best.id.version)
            if (comparison > 0 || (comparison == 0 && candidate.order < best.order)) {
                best = candidate
            }
        }
        return best
    }
}
