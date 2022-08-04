import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

group = "org.example"
version = "2.0.1"

repositories {
    mavenCentral {
        content {
            excludeGroup("org.jogamp.jogl")
        }
    }
    ivy {
        // url = uri("https://jogamp.org/deployment/")
        url = uri("https://score.moe/m/jogamp.org/deployment/")
        patternLayout {
            artifact("v[revision]/jar/[module].[ext]")
        }
        content {
            includeGroup("org.jogamp.jogl")
        }
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

    // Jogamp and dependencies thereof
    implementation("org.jogamp.jogl:gluegen:2.4.0-rc-20210111")
    implementation("org.jogamp.jogl:jogl-all:2.4.0-rc-20210111")
    for (
        p in listOf(
            "android-aarch64",
            "android-armv6",
            "android-x86",
            "ios-amd64",
            "ios-arm64",
            "linux-aarch64",
            "linux-amd64",
            "linux-armv6hf",
            "linux-i586",
            "macosx-universal",
            "windows-amd64",
            "windows-i586"
        )
    ) {
        implementation("org.jogamp.jogl:gluegen-rt-natives-$p:2.4.0-rc-20210111")
        implementation("org.jogamp.jogl:jogl-all-natives-$p:2.4.0-rc-20210111")
    }
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
