import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(androidx.plugins.application.get().pluginId)
    id(androidx.plugins.baselineprofile.get().pluginId)
    id(kotlinx.plugins.parcelize.get().pluginId)
    alias(libs.plugins.about.libraries)
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    compileSdk = AndroidConfig.compileSdkVersion
    namespace = "org.nekomanga"

    defaultConfig {
        minSdk = AndroidConfig.minSdkVersion
        targetSdk = AndroidConfig.targetSdkVersion
        applicationId = "com.kitty.manga"
        versionCode = 3800
        versionName = "3.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField(
            "String",
            "BUILD_TIME",
            if (providers.environmentVariable("CI").orNull == "true") "\"${getBuildTime()}\""
            else "\"1970-01-01T00:00:00\"",
        )

        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["mangadexAuthRedirectUri"] = "mangadex-auth-debug"
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            manifestPlaceholders["mangadexAuthRedirectUri"] = "mangadex-auth"
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        // Disable some unused things
        aidl = false
        shaders = false
        buildConfig = true
    }

    // Kitty ships English only, so drop library translations and license files from the APK.
    androidResources { localeFilters += "en" }

    packaging {
        resources {
            excludes +=
                listOf(
                    "META-INF/**/LICENSE*",
                    "META-INF/*.version",
                    "META-INF/*.kotlin_module",
                    "kotlin/**.kotlin_builtins",
                    "DebugProbesKt.bin",
                )
        }
    }

    flavorDimensions.add("default")

    productFlavors { create("standard") }
}

base { archivesName.set("Kitty") }

composeCompiler {
    val enableMetrics =
        project.providers.gradleProperty("enableComposeCompilerMetrics").orNull.toBoolean()
    val enableReports =
        project.providers.gradleProperty("enableComposeCompilerReports").orNull.toBoolean()

    val rootProjectDir = rootProject.layout.buildDirectory.asFile.get()
    val relativePath = projectDir.relativeTo(rootDir)
    if (enableMetrics) {
        val buildDirPath = rootProjectDir.resolve("compose-metrics").resolve(relativePath)
        metricsDestination.set(buildDirPath)
    }
    if (enableReports) {
        val buildDirPath = rootProjectDir.resolve("compose-reports").resolve(relativePath)
        reportsDestination.set(buildDirPath)
    }
}

dependencies {
    baselineProfile(projects.baselineprofile)

    implementation(projects.constants)
    implementation(projects.core)

    implementation(kotlinx.bundles.kotlin)

    coreLibraryDesugaring(libs.desugaring)

    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.serialization.json)
    implementation(kotlinx.serialization.okio)
    implementation(libs.bundles.ok)
    implementation(libs.tachi.unifile)

    implementation(androidx.bundles.androidx)
    implementation(androidx.profileinstaller)
    implementation(libs.bundles.google)

    implementation(libs.bundles.retrofit)

    // Disk
    implementation(libs.disklrue)

    // HTML parser
    implementation(libs.jsoup)

    // Database
    implementation(libs.sqlite)

    // Room
    implementation(androidx.bundles.room)
    ksp(androidx.room.compiler)

    // Dependency injection
    implementation(libs.injekt.core)

    // Image library
    implementation(libs.bundles.coil)
    implementation(libs.telephoto.zoomable.image.coil)

    // Logging
    implementation(libs.timber)

    // UI
    implementation(libs.cascade.compose)

    // Compose
    implementation(compose.bundles.compose)
    debugImplementation(compose.ui.tooling)
    implementation(compose.gap)
    implementation(compose.bundles.accompanist)

    implementation(compose.bundles.charting)

    implementation(compose.swipe)

    implementation(libs.pastelplaceholders)
    implementation(libs.tokenbucket)
    implementation(libs.bundles.sandwich)
    implementation(libs.aboutLibraries.compose)
    debugImplementation(libs.leakcanary)

    implementation(libs.bundles.results)

    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk) { exclude(group = "junit", module = "junit") }
    testImplementation(kotlinx.coroutines.test)
    testImplementation(androidx.room.testing)
}

tasks.withType<Test> { useJUnit() }

open class UnitTestForwarderTask : DefaultTask() {
    @set:Option(
        option = "tests",
        description = "Sets test class or method name to be included in the test run.",
    )
    @get:Internal
    var testFilters: List<String> = emptyList()
        set(value) {
            field = value
            project.tasks.named<Test>("testStandardDebugUnitTest").configure {
                value.forEach { filter.includeTestsMatching(it) }
            }
        }

    init {
        dependsOn("testStandardDebugUnitTest")
    }
}

tasks.register<UnitTestForwarderTask>("testDebugUnitTest") {
    description = "Run unit tests for the standardDebug build."
    group = "verification"
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
                "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-opt-in=coil3.annotation.ExperimentalCoilApi",
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            )
        )
    }
}
