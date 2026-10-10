import java.util.Properties

plugins {
    // Gradle's own plugin for build logic: it brings the Gradle API and the Kotlin version Gradle embeds,
    // so it has no catalog entry.
    `kotlin-dsl`
}

// buildSrc is a build of its own and does not see the root gradle.properties, so `findProperty("jvm.toolchain")`
// would be null here. Read the root file instead, to keep the toolchain defined in one place.
val jvmToolchain = providers.fileContents(layout.projectDirectory.file("../gradle.properties")).asText.map { content ->
    Properties().apply { load(content.reader()) }.getProperty("jvm.toolchain").toInt()
}

kotlin {
    jvmToolchain(jvmToolchain.get())
}
