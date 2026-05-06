buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        google()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.3")
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

subprojects {
    version = property("projectVersion") as String
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
