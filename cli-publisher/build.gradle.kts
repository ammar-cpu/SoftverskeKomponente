plugins {
    application
}

dependencies {
    // CLI se kompajlira samo protiv specifikacije
    implementation(project(":publisher-api"))

    // Implementacije su potrebne tek pri pokretanju i učitavaju se preko ServiceLoader-a.
    // CLI kod nigde ne pominje ove klase, isto kao što JDBC kod ne pominje MySQL drajver.
    runtimeOnly(project(":local-publisher"))
    runtimeOnly(project(":network-publisher"))
    runtimeOnly(project(":gdrive-publisher"))
}

application {
    // fun main() u fajlu PublisherCli.kt se kompajlira u klasu PublisherCliKt
    mainClass.set("rs.raf.sk.cli.publisher.PublisherCliKt")
}
