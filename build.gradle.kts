plugins {
    kotlin("jvm") version "1.3.71"
    id("org.openjfx.javafxplugin") version "0.0.8"
}

group = "org.example"
version = "1.0-SNAPSHOT"

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
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

javafx {
    version = "14"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.web")
}