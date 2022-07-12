import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

group = "org.example"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.displee:rs-cache-library:6.8.1")
    implementation("org.bitbucket.akornilov.kotlin:binary-streams:0.33")
    implementation(files("lib/jogamp-fat.jar"))
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.slf4j:slf4j-api:1.7.36")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    build {
        dependsOn(fatJar)
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    manifest {
        attributes["Implementation-Version"] = archiveVersion.get()
        attributes["Main-Class"] = "AppKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
