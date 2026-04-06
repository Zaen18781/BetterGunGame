plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta4"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = project.property("group") as String
version = project.property("version") as String

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:33.3.1-jre")
        force("com.google.code.gson:gson:2.11.0")
        force("it.unimi.dsi:fastutil:8.5.15")
        force("org.apache.logging.log4j:log4j-bom:2.24.1")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly(files("C:/Users/tcfke/OneDrive/Desktop/BetterCore/BetterCore_23.03.23uhr/build/libs/BetterCore.jar"))
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12") {
        exclude(group = "org.apache.logging.log4j")
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.10") {
        exclude(group = "org.apache.logging.log4j")
    }
    implementation("dev.triumphteam:triumph-gui:3.1.12")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("dev.triumphteam.gui", "dev.zaen.betterGunGame.libs.gui")
        minimize()
    }

    runServer {
        minecraftVersion("1.21.4")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation"))
    }
}
