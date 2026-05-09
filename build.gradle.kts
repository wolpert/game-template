import java.util.Properties

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        google()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    }
    dependencies {
        classpath(libs.android.gradle.plugin)
    }
}

allprojects {
    apply(plugin = "eclipse")
    apply(plugin = "idea")

    // This allows you to "Build and run using IntelliJ IDEA", an option in IDEA's Settings.
    configure<org.gradle.plugins.ide.idea.model.IdeaModel> {
        module {
            outputDir = file("build/classes/java/main")
            testOutputDir = file("build/classes/java/test")
        }
    }
}

configure(subprojects - project(":android")) {
    apply(plugin = "java-library")
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
    }

    // From https://lyze.dev/2021/04/29/libGDX-Internal-Assets-List/
    tasks.register("generateAssetList") {
        inputs.dir("${project.rootDir}/assets/")
        val assetsFolder = file("${project.rootDir}/assets/")
        val assetsFile = File(assetsFolder, "assets.txt")
        assetsFile.delete()
        fileTree(assetsFolder)
            .map { assetsFolder.toPath().relativize(it.toPath()).toString() }
            .sorted()
            .forEach { assetsFile.appendText(it + "\n") }
    }
    tasks.named("processResources") { dependsOn("generateAssetList") }

    tasks.withType<JavaCompile>().configureEach {
        options.isIncremental = true
        options.encoding = "UTF-8"
    }
}

// `libs` accessor isn't available in cross-project blocks (subprojects/allprojects), so resolve
// the projectVersion via the runtime VersionCatalogsExtension API at root scope and reuse below.
val projectVersionFromCatalog: String =
    extensions.getByType<VersionCatalogsExtension>()
        .named("libs")
        .findVersion("projectVersion").orElseThrow { IllegalStateException("projectVersion missing in libs.versions.toml") }
        .requiredVersion

subprojects {
    version = projectVersionFromCatalog
    extra["appName"] = "game-template"
    repositories {
        mavenCentral()
        // You may want to remove the following line if you have errors downloading dependencies.
        mavenLocal()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        maven { url = uri("https://jitpack.io") }
    }
}

configure<org.gradle.plugins.ide.eclipse.model.EclipseModel> {
    project.name = "game-template-parent"
}

// --- Asset pipeline: Aseprite -> TexturePacker -----------------------------------------
//
// `./gradlew buildAtlases` exports each .aseprite in art/ to per-tag-frame PNGs under
// build/aseprite-frames/, then TexturePacker repacks them into assets/atlases/<appName>.atlas.
// The result is committed so CI doesn't need an Aseprite install — only contributors who
// touch source art do.

val asepriteBinPath: String? by lazy {
    val localProps = file("local.properties")
    if (!localProps.exists()) return@lazy null
    val raw = Properties().apply { localProps.inputStream().use { load(it) } }
        .getProperty("aseprite.bin") ?: return@lazy null
    if (raw.startsWith("~")) System.getProperty("user.home") + raw.drop(1) else raw
}

val asepriteSourceDir = file("art")
val asepriteFrameDir = layout.buildDirectory.dir("aseprite-frames").get().asFile
val atlasOutputDir = file("assets/atlases")
val atlasName = (extra.properties["appName"] as? String) ?: "game-template"

repositories {
    mavenCentral()
}

val texturePacker: Configuration = configurations.create("texturePacker")
dependencies {
    texturePacker(libs.gdx.tools)
    texturePacker(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
}

tasks.register("exportAseprite") {
    description = "Export each .aseprite file in art/ to one PNG per tag-frame in build/aseprite-frames/."
    group = "assets"
    inputs.dir(asepriteSourceDir).withPropertyName("source")
    outputs.dir(asepriteFrameDir).withPropertyName("frames")

    doLast {
        val bin = asepriteBinPath
            ?: throw GradleException("aseprite.bin is not set in local.properties — see README.")
        val binFile = file(bin)
        if (!binFile.canExecute()) {
            throw GradleException("aseprite.bin ($bin) is not an executable file.")
        }

        delete(asepriteFrameDir)
        asepriteFrameDir.mkdirs()

        val sources = asepriteSourceDir.listFiles { f -> f.isFile && f.extension == "aseprite" }
            ?.sortedBy { it.name }
            ?: emptyList()
        if (sources.isEmpty()) {
            logger.lifecycle("No .aseprite files found in $asepriteSourceDir; skipping.")
            return@doLast
        }

        sources.forEach { source ->
            val pattern = "${asepriteFrameDir.absolutePath}/${source.nameWithoutExtension}_{tag}_{tagframe}.png"
            val cmd = listOf(bin, "-b", "--split-tags", source.absolutePath, "--save-as", pattern)
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val rc = process.waitFor()
            if (rc != 0) {
                throw GradleException("aseprite failed (exit=$rc) on ${source.name}:\n$output")
            }
        }
    }
}

tasks.register<JavaExec>("packTextures") {
    description = "Repack frames in build/aseprite-frames/ into a libGDX TextureAtlas under assets/atlases/."
    group = "assets"
    dependsOn("exportAseprite")
    classpath = texturePacker
    mainClass.set("com.badlogic.gdx.tools.texturepacker.TexturePacker")
    // input dir, output dir, pack name
    args(asepriteFrameDir.absolutePath, atlasOutputDir.absolutePath, atlasName)
    inputs.dir(asepriteFrameDir).withPropertyName("frames")
    outputs.dir(atlasOutputDir).withPropertyName("atlas")
    doFirst { atlasOutputDir.mkdirs() }
}

tasks.register("buildAtlases") {
    description = "Run the full art pipeline (aseprite export -> TexturePacker)."
    group = "assets"
    dependsOn("packTextures")
}
