rootProject.name = "FastAsyncWorldEdit"

include("worldedit-libs")

listOf("pnx", "core").forEach {
    include("worldedit-libs:$it")
    include("worldedit-$it")
}

include("worldedit-libs:core:ap")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "EngineHub"
            url = uri("https://maven.enginehub.org/repo/")
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include("worldedit-pnx")
