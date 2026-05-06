val appName: String by extra
val gdxVersion: String by project
val ashleyVersion: String by project
val box2dlightsVersion: String by project
val aiVersion: String by project
val gdxVfxCoreVersion: String by project
val gdxVfxEffectsVersion: String by project
val utilsVersion: String by project
val utilsBox2dVersion: String by project
val graalHelperVersion: String by project
val enableGraalNative: String by project

eclipse.project.name = "$appName-core"

dependencies {
    "api"("com.badlogicgames.ashley:ashley:$ashleyVersion")
    "api"("com.badlogicgames.box2dlights:box2dlights:$box2dlightsVersion")
    "api"("com.badlogicgames.gdx:gdx-ai:$aiVersion")
    "api"("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    "api"("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    "api"("com.badlogicgames.gdx:gdx:$gdxVersion")
    "api"("com.crashinvaders.vfx:gdx-vfx-core:$gdxVfxCoreVersion")
    "api"("com.crashinvaders.vfx:gdx-vfx-effects:$gdxVfxEffectsVersion")
    "api"("com.github.tommyettinger:libgdx-utils-box2d:$utilsBox2dVersion")
    "api"("com.github.tommyettinger:libgdx-utils:$utilsVersion")

    if (enableGraalNative == "true") {
        "implementation"("io.github.berstanio:gdx-svmhelper-annotations:$graalHelperVersion")
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
