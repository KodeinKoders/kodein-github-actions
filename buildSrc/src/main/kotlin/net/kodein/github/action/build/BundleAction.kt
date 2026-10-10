package net.kodein.github.action.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject


// A GitHub Action runs straight from the repository, without `npm install`: it needs one self-contained file.
abstract class BundleAction : DefaultTask() {

    init {
        group = "distribution"
    }
    /** The directory the Kotlin/JS library is compiled to, next to the root `node_modules`. */
    @get:InputDirectory
    abstract val compiled: DirectoryProperty

    /** The compiled library's file name in [compiled]. It must export a `run` function. */
    @get:Input
    abstract val module: Property<String>

    @get:InputFile
    abstract val ncc: RegularFileProperty

    @get:Input
    abstract val node: Property<String>

    @get:OutputDirectory
    abstract val dist: DirectoryProperty

    @get:Inject
    protected abstract val exec: ExecOperations

    @TaskAction
    fun bundle() {
        // The compiled module is a library: nothing runs it. This entry file calls its exported `run`.
        val entry = temporaryDir.resolve("entry.mjs")
        entry.writeText("import { run } from \"${compiled.file(module).get().asFile}\"\nrun()\n")
        exec.exec {
            executable = node.get()
            args(ncc.get().asFile, "build", entry, "--out", dist.get().asFile, "--license", "licenses.txt")
        }
    }
}
