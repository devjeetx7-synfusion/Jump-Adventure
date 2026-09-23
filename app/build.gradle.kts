plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.jumpadventure.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.synfusion.jump"
        minSdk = 24
        targetSdk = 34
        versionCode = providers.gradleProperty("versionCode").map { it.toInt() }.orElse(1).get()
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("jump-adventure-test.jks")
            storePassword = "JumpAdventureTest2026!"
            keyAlias = "jump_adventure_test"
            keyPassword = "JumpAdventureTest2026!"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation("junit:junit:4.13.2")
}
