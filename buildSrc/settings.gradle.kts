dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    // Downloads the JDK the toolchain asks for. The version is pinned in the root settings.gradle.kts, whose
    // plugin classpath buildSrc inherits - declaring it again here fails with "already on the classpath
    // with an unknown version".
    id("org.gradle.toolchains.foojay-resolver-convention")
}
