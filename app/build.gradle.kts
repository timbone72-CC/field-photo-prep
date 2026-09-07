plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.timbone72cc.fieldphotoprep"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.timbone72cc.fieldphotoprep"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    implementation("com.google.code.gson:gson:2.14.0")

    testImplementation("junit:junit:4.13.2")
}
