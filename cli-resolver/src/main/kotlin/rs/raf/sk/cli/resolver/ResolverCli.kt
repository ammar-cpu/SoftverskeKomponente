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
        val resolver = DependencyResolver.load(args[0])
        resolver.connect(args[1])
        resolver.strategy = ConflictStrategy.byName(strategyOption(args))
        val roots = DependencyResolver.readDependencyFile(File(args[3]))

        for (id in resolver.resolve(roots)) {
            println(id)
        }
    } else if (args.size == 4 || args.size == 5) {
        val resolver = DependencyResolver.load(args[0])
        resolver.connect(args[1])
        resolver.strategy = ConflictStrategy.byName(strategyOption(args))
        val roots = DependencyResolver.readDependencyFile(File(args[2]))

        val outputDir = File(args[3])
        if (outputDir.list()?.isNotEmpty() == true) {
            System.err.println("Warning: ${args[3]} is not empty; existing files are kept and files with the same name are overwritten")
        }

        val files = resolver.download(roots, outputDir)
        println("Downloaded ${files.size} file(s) to ${args[3]}:")
        for (file in files) {
            println("  ${file.name}")
        }
    } else {
        printUsage()
    }
}

private fun strategyOption(args: Array<String>): String =
    if (args.size == 5) args[4].removePrefix("--strategy=") else "nearest"

private fun printUsage() {
    println("Usage:")
    println("  cli-resolver <type> <location> <list-file> <output-dir> [--strategy=newest|nearest]")
    println("      download components from the list and their dependencies (default strategy: nearest)")
    println()
    println("  cli-resolver types")
    println("      list available repository types")
    println("  cli-resolver <type> <location> config")
    println("      print the repository config")
    println("  cli-resolver <type> <location> list <list-file>")
    println("      print the components from a dependency file")
    println("  cli-resolver <type> <location> tree <list-file>")
    println("      print all found versions with depth (temporary)")
    println("  cli-resolver <type> <location> resolve <list-file> [--strategy=newest|nearest]")
    println("      print the final list without downloading")
}
