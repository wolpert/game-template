val appName: String by extra
val gdxVersion: String by project
val ashleyVersion: String by project
val daggerVersion: String by project
val junitVersion: String by project
val mockitoVersion: String by project
val snakeYamlVersion: String by project
val graalHelperVersion: String by project
val enableGraalNative: String by project

eclipse.project.name = "$appName-core"

dependencies {
    "api"("com.badlogicgames.ashley:ashley:$ashleyVersion")
    "api"("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    "api"("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    "api"("com.badlogicgames.gdx:gdx:$gdxVersion")

    "api"("com.google.dagger:dagger:$daggerVersion")
    "annotationProcessor"("com.google.dagger:dagger-compiler:$daggerVersion")

    "api"("org.yaml:snakeyaml:$snakeYamlVersion")

    if (enableGraalNative == "true") {
        "implementation"("io.github.berstanio:gdx-svmhelper-annotations:$graalHelperVersion")
    }

    "testImplementation"("org.junit.jupiter:junit-jupiter:$junitVersion")
    "testImplementation"("org.mockito:mockito-core:$mockitoVersion")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    // Native libs so tests can construct real Box2D Worlds/Bodies — World's class init touches JNI.
    "testRuntimeOnly"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    "testRuntimeOnly"("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")
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
