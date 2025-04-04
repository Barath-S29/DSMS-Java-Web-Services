plugins {
    id("java")
}

group = "org.Share"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.sun.xml.ws:jaxws-rt:2.3.3")
//    implementation("javax.xml.ws:jaxws-api:2.3.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}