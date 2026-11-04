plugins {
    `java-library`
}

dependencies {
    implementation(project(":publisher-api"))

    // TODO: Google Drive API i OAuth biblioteke, npr.
    // implementation("com.google.api-client:google-api-client:...")
    // implementation("com.google.oauth-client:google-oauth-client-jetty:...")
    // implementation("com.google.apis:google-api-services-drive:...")
}
