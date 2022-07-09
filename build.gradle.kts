import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.3.71"
    id("org.openjfx.javafxplugin") version "0.0.8"
}

group = "org.example"
version = "0.1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.displee:rs-cache-library:6.8.1")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("org.bitbucket.akornilov.kotlin:binary-streams:0.33")
    implementation(files("lib/jogamp-fat.jar"))
    implementation(files("lib/dockfx-0.4-SNAPSHOT.jar"))
    implementation("com.google.inject:guice:5.0.1")
    implementation("com.jfoenix:jfoenix:9.0.10")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    implementation("org.apache.commons:commons-compress:1.21")
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