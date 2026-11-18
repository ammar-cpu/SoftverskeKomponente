package rs.raf.sk.model

/** Osnovna greška za sve probleme sa repozitorijumom. */
open class RepositoryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Komponenta ili podaci ne prolaze provere pri postavljanju. */
class ValidationException(message: String, cause: Throwable? = null) : RepositoryException(message, cause)

/** Tražena komponenta ne postoji u repozitorijumu. */
class ComponentNotFoundException(message: String, cause: Throwable? = null) : RepositoryException(message, cause)

