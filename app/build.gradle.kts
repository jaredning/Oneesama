plugins {
    alias(libs.plugins.android.application)
    id("realm-android")
}

android {
    namespace = "catgirl.oneesama"
    compileSdk = 35

    defaultConfig {
        applicationId = "catgirl.oneesama.v2"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.jvmArgs = (options.forkOptions.jvmArgs ?: mutableListOf()) + listOf(
        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)

    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.adapter.rxjava)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.commons.io)
    implementation(libs.appmetrica)
    implementation(libs.androidx.documentfile)
    implementation(libs.swiperefreshlayout)
    implementation(libs.commons.lang3)
    implementation(libs.materialish.progress)
    implementation(libs.butterknife)
    annotationProcessor(libs.butterknife.compiler)
    implementation(libs.picasso)
    implementation(libs.nineoldandroids)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
