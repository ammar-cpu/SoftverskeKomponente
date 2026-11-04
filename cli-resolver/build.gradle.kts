plugins {
    application
}

dependencies {
    implementation(project(":resolver-api"))

    runtimeOnly(project(":local-resolver"))
    runtimeOnly(project(":network-resolver"))
    runtimeOnly(project(":gdrive-resolver"))
}

application {
    // fun main() u fajlu ResolverCli.kt se kompajlira u klasu ResolverCliKt
    mainClass.set("rs.raf.sk.cli.resolver.ResolverCliKt")
}
