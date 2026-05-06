import java.util.Properties

val appName: String by extra
val gdxVersion: String by project

plugins {
    id("com.android.application")
}

android {
    namespace = "com.codeheadsystems.game"
    compileSdk = 35
    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.setSrcDirs(listOf("src/main/java"))
            aidl.setSrcDirs(listOf("src/main/java"))
            renderscript.setSrcDirs(listOf("src/main/java"))
            res.setSrcDirs(listOf("res"))
            assets.setSrcDirs(listOf("../assets"))
            jniLibs.setSrcDirs(listOf("libs"))
        }
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/robovm/ios/robovm.xml", "META-INF/DEPENDENCIES.txt", "META-INF/DEPENDENCIES",
                "META-INF/dependencies.txt", "**/*.gwt.xml"
            )
            pickFirsts += setOf(
                "META-INF/LICENSE.txt", "META-INF/LICENSE", "META-INF/license.txt", "META-INF/LGPL2.1",
                "META-INF/NOTICE.txt", "META-INF/NOTICE", "META-INF/notice.txt"
            )
        }
    }
    defaultConfig {
        applicationId = "com.codeheadsystems.game"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

repositories {
    // needed for AAPT2, may be needed for other tools
    google()
}

val natives: Configuration by configurations.creating

dependencies {
    "coreLibraryDesugaring"("com.android.tools:desugar_jdk_libs:2.1.5")
    "implementation"("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    "implementation"(project(":core"))

    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

// Called every time gradle gets executed; takes the native dependencies of
// the natives configuration and extracts them into the proper libs/ folders
// so they get packed with the APK.
tasks.register("copyAndroidNatives") {
    doFirst {
        file("libs/armeabi-v7a/").mkdirs()
        file("libs/arm64-v8a/").mkdirs()
        file("libs/x86_64/").mkdirs()
        file("libs/x86/").mkdirs()

        natives.copy().files.forEach { jar ->
            val outputDir: File? = when {
                jar.name.endsWith("natives-armeabi-v7a.jar") -> file("libs/armeabi-v7a")
                jar.name.endsWith("natives-arm64-v8a.jar") -> file("libs/arm64-v8a")
                jar.name.endsWith("natives-x86_64.jar") -> file("libs/x86_64")
                jar.name.endsWith("natives-x86.jar") -> file("libs/x86")
                else -> null
            }
            if (outputDir != null) {
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("copyAndroidNatives")
}

tasks.register<Exec>("run") {
    val localProperties = project.file("../local.properties")
    val path: String = if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_SDK_ROOT")
    } else {
        System.getenv("ANDROID_SDK_ROOT")
    }
    val adb = "$path/platform-tools/adb"
    commandLine(
        adb, "shell", "am", "start", "-n",
        "com.codeheadsystems.game/com.codeheadsystems.game.android.AndroidLauncher"
    )
}

eclipse.project.name = "$appName-android"
