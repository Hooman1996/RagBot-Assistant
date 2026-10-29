plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val sourceRevision =
    providers
        .exec {
            workingDir(rootDir.parentFile)
            commandLine("git", "rev-parse", "--short=12", "HEAD")
        }
        .standardOutput
        .asText
        .get()
        .trim()
val privateResources = providers.gradleProperty("replicaPrivateResources").orNull

android {
    namespace = "org.ragbot.replica"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "org.ragbot.replica"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SOURCE_REVISION", "\"$sourceRevision\"")
        buildConfigField("boolean", "PRIVATE_RESOURCES", (privateResources != null).toString())
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs.getByName("debug") {
        storeFile = rootDir.resolve("../local/toolchain/replica-debug.keystore")
    }
    if (privateResources != null) {
        require(file(privateResources).isDirectory) {
            "Private resource input directory is missing"
        }
        sourceSets.getByName("main").res.directories.add(privateResources)
    }
    lint { abortOnError = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("junit:junit:4.13.2")
}
