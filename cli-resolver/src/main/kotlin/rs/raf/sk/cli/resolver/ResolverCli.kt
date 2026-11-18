package rs.raf.sk.cli.resolver

import rs.raf.sk.model.RepositoryException
import rs.raf.sk.resolver.ConflictStrategy
import rs.raf.sk.resolver.DependencyResolver
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        run(args)
    } catch (e: RepositoryException) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}

private fun run(args: Array<String>) {
    if (args.size == 1 && args[0] == "types") {
        println(DependencyResolver.availableTypes().joinToString(", "))
    } else if (args.size == 3 && args[2] == "config") {
        val resolver = DependencyResolver.load(args[0])
        resolver.connect(args[1])
        val config = resolver.readConfig()

        println("allowedExtensions: ${config.allowedExtensions.joinToString(", ")}")
        println("scopes:")
        for ((scope, transitive) in config.scopes) {
            println("  $scope: ${if (transitive) "transitive" else "not transitive"}")
        }
    } else if (args.size == 4 && args[2] == "list") {
        val resolver = DependencyResolver.load(args[0])
        resolver.connect(args[1])

        for (id in DependencyResolver.readDependencyFile(File(args[3]))) {
            println(id)
        }
    } else if (args.size == 4 && args[2] == "tree") {
        val resolver = DependencyResolver.load(args[0])
        resolver.connect(args[1])
        val roots = DependencyResolver.readDependencyFile(File(args[3]))

        for ((key, candidates) in resolver.collectCandidates(roots)) {
            println(key)
            for (candidate in candidates) {
                println("  ${candidate.id.version}  depth=${candidate.depth}  order=${candidate.order}")
            }
        }
    } else if ((args.size == 4 || args.size == 5) && args[2] == "resolve") {
        val strategyName = if (args.size == 5) args[4].removePrefix("--strategy=") else "nearest"

        val resolver = DependencyResolver.load(args[0])
        resolver.connect(args[1])
        resolver.strategy = ConflictStrategy.byName(strategyName)
        val roots = DependencyResolver.readDependencyFile(File(args[3]))

        for (id in resolver.resolve(roots)) {
            println(id)
        }
    } else {
        printUsage()
    }
}

private fun printUsage() {
    println("Usage:")
    println("  cli-resolver types                       list available repository types")
    println("  cli-resolver <type> <location> config    print the repository config")
    println("  cli-resolver <type> <location> list <file>   print the components from a dependency file")
    println("  cli-resolver <type> <location> tree <file>   print all found versions with depth (temporary)")
    println("  cli-resolver <type> <location> resolve <file> [--strategy=newest|nearest]")
    println("                                           print the final list (default strategy: nearest)")
}
