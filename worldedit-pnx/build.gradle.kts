import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.userdev.attribute.Obfuscation

plugins {
    `java-library`
    id("com.modrinth.minotaur") version "2.+"
}

project.description = "PNX"

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.powernukkitx.cn/repository/maven-releases/")
    }
    flatDir { dir(File("src/main/resources")) }
}

val localImplementation = configurations.create("localImplementation") {
    description = "Dependencies used locally, but provided by the runtime Bukkit implementation"
    isCanBeConsumed = false
    isCanBeResolved = false
}

dependencies {
    // Modules
    api(projects.worldeditCore)
    compileOnly(projects.worldeditLibs.core.ap)
    compileOnly(group = "cn.powernukkitx", name = "powernukkitx", version = "1.19.50-r3") {
        exclude("junit", "junit")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    annotationProcessor(projects.worldeditLibs.core.ap)

    // Minecraft expectations
    annotationProcessor(libs.guava)
    implementation("com.google.guava:guava")
    implementation("com.google.code.gson:gson")

    // Logging
    implementation(libs.log4jBom) {
        because("We control Log4J on this platform")
    }
    implementation("org.apache.logging.log4j:log4j-api")
    implementation(libs.log4jCore)
    implementation("commons-cli:commons-cli:1.5.0")
    api(libs.parallelgzip) { isTransitive = false }
    api(libs.lz4Java)
}

tasks.named<Copy>("processResources") {
    val internalVersion = project.ext["internalVersion"]
    inputs.property("internalVersion", internalVersion)
    filesMatching("plugin.yml") {
        expand("internalVersion" to internalVersion)
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Class-Path" to CLASSPATH,
                "WorldEdit-Version" to project.version)
    }
}

addJarManifest(WorldEditKind.Plugin, includeClasspath = true)

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-PNX-${project.version}.${archiveExtension.getOrElse("jar")}")
    dependencies {
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
        include(dependency(":worldedit-core"))
        include(dependency("org.antlr:antlr4-runtime"))
        relocate("it.unimi.dsi.fastutil", "com.sk89q.worldedit.pnx.fastutil") {
            include(dependency("it.unimi.dsi:fastutil"))
        }
        relocate("org.lz4", "com.fastasyncworldedit.core.lz4") {
            include(dependency("org.lz4:lz4-java:1.8.0"))
        }
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

tasks {
    modrinth {
        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("fastasyncworldedit")
        versionName.set("${project.version}")
        versionNumber.set("${project.version}")
        versionType.set("release")
        uploadFile.set(file("build/libs/${rootProject.name}-Bukkit-${project.version}.jar"))
        gameVersions.addAll(listOf("1.19.3", "1.19.2", "1.19.1", "1.19", "1.18.2", "1.17.1", "1.16.5"))
        loaders.addAll(listOf("paper", "spigot"))
        changelog.set("The changelog is available on GitHub: https://github.com/IntellectualSites/" +
                "FastAsyncWorldEdit/releases/tag/${project.version}")
        syncBodyFrom.set(rootProject.file("README.md").readText())
    }
}
