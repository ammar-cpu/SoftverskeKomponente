plugins {
    application
}

dependencies {
    implementation(project(":model"))
}

application {
    // fun main() u fajlu NetworkServer.kt se kompajlira u klasu NetworkServerKt
    mainClass.set("rs.raf.sk.server.NetworkServerKt")
}
