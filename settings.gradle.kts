// Pixelfray Android glitch camera app using png-glitch and the wasm2kt Gradle plugin.
pluginManagement {
    val wasm2ktPath = providers.gradleProperty("wasm2ktDir").orNull ?: "third_party/wasm-component-kotlin"
    includeBuild(wasm2ktPath)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pixelfray"

// Substitutes io.github.wasm2kt:runtime-chicory with the wasm-component-kotlin submodule
// (or a local checkout passed via -Pwasm2ktDir=/path/to/wasm-component-kotlin).
val wasm2ktPath = providers.gradleProperty("wasm2ktDir").orNull ?: "third_party/wasm-component-kotlin"
includeBuild(wasm2ktPath)
include(":app")
include(":baselineprofile")
