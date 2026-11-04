import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

// Verzije plugin-a su samo ovde. Moduli ih koriste bez navođenja verzije.
plugins {
    kotlin("jvm") version "2.4.20" apply false
    kotlin("plugin.serialization") version "2.4.20" apply false
    id("org.jetbrains.dokka") version "2.2.0" apply false
}

// Podešavanja koja važe za sve module.
// Svaki modul u svom build.gradle.kts dodaje samo ono što je specifično za njega.
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")

    group = "rs.raf.sk"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    dependencies {
        "testImplementation"(kotlin("test"))
    }

    // Kotlin i Java deo build-a moraju ciljati istu verziju JVM-a, inače Gradle prijavi grešku.
    // 17 znači da projekat radi na JDK 17 i novijim.
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
