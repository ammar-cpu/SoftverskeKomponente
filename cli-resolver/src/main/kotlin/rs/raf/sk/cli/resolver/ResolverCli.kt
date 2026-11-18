package rs.raf.sk.cli.resolver

import rs.raf.sk.model.RepositoryException
import rs.raf.sk.resolver.DependencyResolver
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
    } else {
        printUsage()
    }
}

private fun printUsage() {
    println("Usage:")
    println("  cli-resolver types                       list available repository types")
    println("  cli-resolver <type> <location> config    print the repository config")
}
