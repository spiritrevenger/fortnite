plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.perso.fortauto"
    compileSdk = 34
    defaultConfig { applicationId = "com.perso.fortauto"; minSdk = 30; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    // Modèle OCR latin embarqué : fonctionne hors-ligne, lit l'anglais
    implementation("com.google.mlkit:text-recognition:16.0.0")
}
