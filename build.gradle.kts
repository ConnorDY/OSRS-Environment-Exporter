import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("application")
}

group = "link.cdy"
version = "2.5.0"

repositories {
    mavenCentral {
        content {
            excludeModule("org.lwjglx", "lwjgl3-awt")
        }
    }
    maven {
        url = uri("https://score.moe/m2/")
        metadataSources {
            artifact()
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.displee:rs-cache-library:6.8.1")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.pushing-pixels:radiance-theming:5.0.0")

    // lwjgl and dependencies thereof
    implementation(platform("org.lwjgl:lwjgl-bom:3.3.1"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-jawt")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjglx", "lwjgl3-awt", "0.1.9-SNAPSHOT")
    implementation("org.joml", "joml", "1.10.4")
    testImplementation(kotlin("test"))
    for (
        p in listOf(
            "natives-linux",
            "natives-linux-arm32",
            "natives-linux-arm64",
            "natives-macos",
            "natives-macos-arm64",
            "natives-windows",
            "natives-windows-arm64",
            "natives-windows-x86",
        )
    ) {
        runtimeOnly("org.lwjgl", "lwjgl", classifier = p)
        runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = p)
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
    withType<Jar> {
        manifest {
            attributes["Implementation-Version"] = archiveVersion.get()
            attributes["Main-Class"] = "AppKt"
        }
    }
    build {
        dependsOn(fatJar)
    }
}

tasks.test {
    useJUnitPlatform()
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

application {
    mainClass.set("AppKt")
}
