val appName: String by extra
val gdxVersion: String by project
val ashleyVersion: String by project
val box2dlightsVersion: String by project
val aiVersion: String by project
val daggerVersion: String by project
val junitVersion: String by project
val mockitoVersion: String by project
val snakeYamlVersion: String by project
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

    "api"("com.google.dagger:dagger:$daggerVersion")
    "annotationProcessor"("com.google.dagger:dagger-compiler:$daggerVersion")

    "api"("org.yaml:snakeyaml:$snakeYamlVersion")

    if (enableGraalNative == "true") {
        "implementation"("io.github.berstanio:gdx-svmhelper-annotations:$graalHelperVersion")
    }

    "testImplementation"("org.junit.jupiter:junit-jupiter:$junitVersion")
    "testImplementation"("org.mockito:mockito-core:$mockitoVersion")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

// Load Mockito as an explicit JVM agent so it doesn't self-attach (deprecated on JDK 21+).
val mockitoAgent: Configuration = configurations.create("mockitoAgent")
dependencies {
    mockitoAgent("org.mockito:mockito-core:$mockitoVersion") { isTransitive = false }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
