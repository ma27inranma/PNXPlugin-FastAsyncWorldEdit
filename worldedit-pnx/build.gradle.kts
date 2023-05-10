import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
}

project.description = "PNX"

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    mavenCentral()
    maven {
        url = uri("https://www.jitpack.io")
    }
    maven {
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
    maven {
        url = uri("https://repo.opencollab.dev/maven-releases/")
    }
    flatDir { dir(File("src/main/resources")) }
}

val localImplementation = configurations.create("localImplementation") {
    description = "Dependencies used locally, but provided by the runtime Bukkit implementation"
    isCanBeConsumed = false
    isCanBeResolved = false
}

dependencies {
    compileOnly(group = "cn.powernukkitx", name = "powernukkitx", version = "1.19.80-r2") {
        exclude("junit", "junit")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    // Modules
    api(projects.worldeditCore)
    api(projects.worldeditLibs.pnx)

    // Minecraft expectations
    implementation(libs.fastutil)
    // Platform expectations
//    compileOnly(files("D:\\idea\\project\\PowerNukkitX\\target\\powernukkitx-1.19.80-r1-shaded.jar"))
    // Logging
    localImplementation("org.apache.logging.log4j:log4j-api")
    localImplementation(libs.log4jBom) {
        because("Spigot provides Log4J (sort of, not in API, implicitly part of server)")
    }
    // Third party
    implementation("dev.notmyfault.serverlib:ServerLib")
    implementation("com.intellectualsites.paster:Paster") { isTransitive = false }
    api(libs.lz4Java) { isTransitive = false }
    api(libs.sparsebitset) { isTransitive = false }
    api(libs.parallelgzip) { isTransitive = false }
    compileOnly("net.kyori:adventure-api")
    compileOnlyApi("org.checkerframework:checker-qual")

    // Tests
    testImplementation(libs.mockito)
    testImplementation("net.kyori:adventure-api")
    testImplementation("org.checkerframework:checker-qual")
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
