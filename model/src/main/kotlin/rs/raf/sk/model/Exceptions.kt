package rs.raf.sk.model

/** Osnovna greška za sve probleme sa repozitorijumom. */
open class RepositoryException(message: String) : RuntimeException(message)

/** Komponenta ili podaci ne prolaze provere pri postavljanju. */
class ValidationException(message: String) : RepositoryException(message)

/** Tražena komponenta ne postoji u repozitorijumu. */
class ComponentNotFoundException(message: String) : RepositoryException(message)

