package rs.raf.sk.resolver

/**
 * Bira verziju najbližu korenu. Ako su dve verzije na istoj dubini,
 * bira onu koja je prva pronađena.
 */
object NearestWinsStrategy : ConflictStrategy {

    /**
     * Bira verziju sa najmanjom dubinom.
     *
     * @param candidates sve pronađene verzije jednog projekta; lista nije prazna
     * @return kandidat sa najmanjom dubinom, a kod iste dubine onaj sa manjim redom nailaska
     */
    override fun choose(candidates: List<Candidate>): Candidate {
        var best = candidates.first()
        for (candidate in candidates) {
            if (candidate.depth < best.depth || (candidate.depth == best.depth && candidate.order < best.order)) {
                best = candidate
            }
        }
        return best
    }
}
