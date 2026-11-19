package rs.raf.sk.cli.publisher

import rs.raf.sk.model.ComponentId
import rs.raf.sk.model.Dependency
import rs.raf.sk.model.RepoConfig
import rs.raf.sk.model.RepositoryException
import rs.raf.sk.model.ValidationException
import rs.raf.sk.model.parseConfig
import rs.raf.sk.publisher.Publisher
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

/** Konfiguracija koja se koristi kad se init pozove bez config fajla. */
private val DEFAULT_CONFIG = RepoConfig(
    allowedExtensions = listOf("jar", "zip"),
    scopes = mapOf("compile" to true, "runtime" to true, "test" to false, "provided" to false)
)

fun main(args: Array<String>) {
    try {
        run(args)
    } catch (e : RepositoryException) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}

private fun run(args: Array<String>) {
    if (args.size == 1 && args[0] == "types") {
        println(Publisher.availableTypes().joinToString(", "))
        return
    }
    if (args.size < 3) {
        printUsage()
        return
    }

    val publisher = Publisher.load(args[0])
    publisher.connect(args[1])
    val rest = args.drop(3)  // sve posle komande

    when (args[2]) {
        "init" -> {
            val config = if(rest.isEmpty()) DEFAULT_CONFIG else readConfigFile(File(rest[0]))
            publisher.init(config)
            println("Repository created at ${args[1]}")
        }

        "config" -> {
            val config = publisher.getConfig()
            println("allowedExtensions: ${config.allowedExtensions.joinToString(", ")}")
            println("scopes:")
            for ((scope, transitive) in config.scopes) {
                println(" $scope: ${if (transitive) "transitive" else "not transitive"}")
            }
        }

        "set-config" -> {
            if (rest.size != 1) return printUsage()
            publisher.setConfig(readConfigFile(File(rest[0])))
            println("Config updated")
        }

        "deploy" -> {
            if (rest.size !in 2..3) return printUsage()
            val file = File(rest[0])
            if (!file.isFile) {
                throw RepositoryException("File not found: ${file.path}")
            }
            val id = ComponentId.parse(rest[1])
            val dependencies = if (rest.size == 3) readDependencies(File(rest[2])) else emptyList()

            val metadata = publisher.deploy(id, file.extension, file.readBytes(), dependencies)
            println("Deployed $id as ${metadata.file} (${metadata.dependencies.size} dependencies)")
        }

        "organizations" -> {
            for (organization in publisher.listOrganizations()) {
                println(organization)
            }
        }

        "projects" -> {
            if (rest.size != 1) return printUsage()
            for (project in publisher.listProjects(rest[0])) {
                println(project)
            }
        }

        "versions" -> {
            if (rest.size != 2) return printUsage()
            for (version in publisher.listVersions(rest[0], rest[1])) {
                println(version)
            }
        }

        "deps" -> {
            if (rest.size != 1) return printUsage()
            val id = ComponentId.parse(rest[0])
            for (dep in publisher.getDependencies(id)) {
                val suffix = if (dep.optional) " optional" else ""
                println("${dep.id} ${dep.scope}$suffix")
            }
        }

        else -> printUsage()
    }
}

/** Cita config.json sa lokalnog diska. */
private fun readConfigFile(file: File): RepoConfig {
    val text = try {
        file.readText()
    } catch (e: IOException) {
        throw RepositoryException("Cannot read config file: ${file.path}", e)
    }
    return parseConfig(text)
}

/**
 * Cita fajl sa zavisnostima. Svaka linija je "organizacija:projekat:verzija scope[optional]".
 * Prazne linije i linije koje pocinju sa '#' se preskacu.
 */
private fun readDependencies(file: File): List<Dependency> {
    val lines = try {
        file.readLines()
    } catch (e: IOException) {
        throw RepositoryException("Cannot read dependencies file: ${file.path}", e)
    }

    val result = mutableListOf<Dependency>()
    for ((index, rawLine) in lines.withIndex()) {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) {
            continue
        }

        val where = "${file.path}, line ${index + 1}"
        val parts = line.split(Regex("\\s+"))
        if (parts.size !in 2..3 || (parts.size == 3 && parts[2] != "optional")){
            throw ValidationException("$where: expected 'organization:project:version scope [optional]'")
        }

        val id = try {
            ComponentId.parse(parts[0])
        } catch (e: ValidationException) {
            throw ValidationException("$where: ${e.message}", e)
        }
        result.add(Dependency(id.organization, id.project, id.version, parts[1], optional = parts.size == 3))
    }
    return result
}

private fun printUsage() {
    println("Usage:")
    println("  cli-publisher types")
    println("  cli-publisher <type> <location> init [config.json]")
    println("  cli-publisher <type> <location> config")
    println("  cli-publisher <type> <location> set-config <config.json>")
    println("  cli-publisher <type> <location> deploy <file> <organization:project:version> [dependencies.txt]")
    println("  cli-publisher <type> <location> organizations")
    println("  cli-publisher <type> <location> projects <organization>")
    println("  cli-publisher <type> <location> versions <organization> <project>")
    println("  cli-publisher <type> <location> deps <organization:project:version>")
}
