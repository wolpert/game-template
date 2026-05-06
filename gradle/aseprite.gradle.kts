import java.util.Properties

// Aseprite export pipeline.
//
// Two tasks:
//   exportAseprite  - runs the Aseprite CLI on every .aseprite file under art/,
//                     producing a PNG sheet + JSON frame manifest in build/aseprite/.
//   packAtlases     - runs libGDX TexturePacker over build/aseprite/, writing
//                     atlases into assets/atlases/.
//
// The Aseprite CLI path is resolved in this order:
//   1. aseprite.cli in local.properties
//   2. ASEPRITE_CLI env var
//   3. `aseprite` on PATH
//
// Add to local.properties (gitignored) when the binary isn't on PATH:
//   aseprite.cli=/opt/Aseprite/aseprite

fun asepriteCliPath(): String {
    val props = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
    return props.getProperty("aseprite.cli") ?: System.getenv("ASEPRITE_CLI") ?: "aseprite"
}

val artDir = rootProject.file("art")
val exportDir = rootProject.file("build/aseprite")
val atlasDir = rootProject.file("assets/atlases")

tasks.register("exportAseprite") {
    group = "assets"
    description = "Export every .aseprite file in art/ to a PNG sheet + JSON manifest."

    val sources = fileTree(artDir) { include("**/*.aseprite") }
    inputs.files(sources).withPropertyName("asepriteSources").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(exportDir).withPropertyName("exportDir")

    doLast {
        val cli = asepriteCliPath()
        exportDir.mkdirs()

        sources.forEach { src ->
            val rel = artDir.toPath().relativize(src.toPath()).toString()
            val base = rel.removeSuffix(".aseprite")
            val png = File(exportDir, "$base.png")
            val json = File(exportDir, "$base.json")
            png.parentFile.mkdirs()

            exec {
                commandLine(
                    cli,
                    "-b",
                    src.absolutePath,
                    "--sheet", png.absolutePath,
                    "--data", json.absolutePath,
                    "--format", "json-array",
                    "--list-tags",
                    "--sheet-pack"
                )
            }
        }
    }
}

tasks.register("packAtlases") {
    group = "assets"
    description = "Pack exported sheets into libGDX TextureAtlases under assets/atlases/."
    dependsOn("exportAseprite")

    inputs.dir(exportDir).withPropertyName("exportDir")
    outputs.dir(atlasDir).withPropertyName("atlasDir")

    doLast {
        atlasDir.mkdirs()
        // TexturePacker lives in gdx-tools; the buildscript should declare
        // com.badlogicgames.gdx:gdx-tools on its classpath before applying this script.
        val settings = com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings()
        settings.maxWidth = 2048
        settings.maxHeight = 2048
        settings.pot = false
        settings.duplicatePadding = true

        com.badlogic.gdx.tools.texturepacker.TexturePacker.process(
            settings,
            exportDir.absolutePath,
            atlasDir.absolutePath,
            "game"   // produces game.atlas + game.png(s)
        )
    }
}

// Umbrella task — extend dependsOn here as more asset pipelines are added.
tasks.register("processAssets") {
    group = "assets"
    description = "Run all asset build steps (Aseprite export, atlas packing, ...)."
    dependsOn("packAtlases")
}

// Hook into every subproject's build + dev-iteration tasks so assets are
// always fresh.
val assetTriggers = setOf("build", "run", "installDebug")
allprojects {
    tasks.matching { assetTriggers.contains(it.name) }.configureEach {
        dependsOn(rootProject.tasks.named("processAssets"))
    }
}
