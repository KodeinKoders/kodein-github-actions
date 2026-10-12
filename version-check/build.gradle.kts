import net.kodein.github.action.build.BundleAction
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(findProperty("jvm.toolchain")!!.toString().toInt())

    js {
        // The @actions/* npm packages are ESM-only, so they cannot be `require`d from the CommonJS (UMD) output.
        useEsModules()
        nodejs()
        binaries.library()
    }

    explicitApi()

    compilerOptions {
        // The action's entry point is exported, and the YAML wrapper hands back JsAny values.
        optIn.addAll("kotlin.js.ExperimentalJsExport", "kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        jsMain.dependencies {
            implementation(libs.kotlinWrappers.actions.core)
            implementation(libs.kotlinWrappers.actions.exec)
            implementation(libs.kotlinWrappers.actions.github)
            implementation(libs.kotlinWrappers.actions.io)
            implementation(libs.kotlinWrappers.yaml)

            implementation(libs.kotlinx.coroutines)

            // Bundles the action and its npm dependencies into a single file (see the "bundle" task below).
            implementation(devNpm("@vercel/ncc", libs.versions.ncc.get()))
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// `dist/` is committed, so this is run by hand (`./gradlew :version-check:bundle`) and is not part of `build`.
tasks.register<BundleAction>("bundle") {
    // The compiled library is synced next to the root node_modules, where `@actions/*` and `yaml` resolve from.
    dependsOn("jsProductionLibraryCompileSync", ":kotlinNpmInstall")
    compiled = rootProject.layout.buildDirectory.dir("js/packages/${rootProject.name}-${project.name}/kotlin")
    module = "${rootProject.name}-${project.name}.mjs"
    ncc = rootProject.layout.buildDirectory.file("js/node_modules/@vercel/ncc/dist/ncc/cli.js")
    node = rootProject.the<NodeJsEnvSpec>().executable
    dist = layout.projectDirectory.dir("dist")
}
