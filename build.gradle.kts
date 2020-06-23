import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.3.71"
    id("org.openjfx.javafxplugin") version "0.0.8"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.displee:rs-cache-library:6.4")
    api("com.google.code.gson:gson:2.8.5")
    api("org.bitbucket.akornilov.kotlin:binary-streams:0.33")
    api(files("lib/jogamp-fat.jar"))
    api(files("lib/dockfx-0.4-SNAPSHOT.jar"))
    api("com.google.inject:guice:4.2.3")
    api("com.jfoenix:jfoenix:9.0.9")
    implementation("org.jsoup:jsoup:1.11.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    api(group = "org.codehaus.plexus", name = "plexus-archiver", version = "4.2.2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    build {
        dependsOn(fatJar)
    }
}

javafx {
    version = "14"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.web")
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "AppKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}