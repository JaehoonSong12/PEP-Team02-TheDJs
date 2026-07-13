plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.netflix.dgs.codegen") version "8.3.0"
	id("org.graalvm.buildtools.native") version "1.1.1"
}

group = "com.revature"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

extra["springModulithVersion"] = "2.1.0-RC1"

dependencies {
	// Put the REST Assured dependency above JUnit
	testImplementation("io.rest-assured:rest-assured:6.0.0")
	implementation("org.springframework.modulith:spring-modulith-starter-core")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa") // [Added dependency] Added as instructed to support Spring Data JPA
	implementation("org.springframework.boot:spring-boot-starter-webmvc") // [Added dependency] Added as instructed to support Spring Web MVC
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test") // [Added dependency] Added as instructed to support JPA testing
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test") // [Added dependency] Added as instructed to support Web MVC testing
	testImplementation("org.springframework.modulith:spring-modulith-starter-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
	implementation("org.xerial:sqlite-jdbc:3.53.2.0") // [Added dependency] Added as instructed for SQLite database driver
	implementation("org.hibernate.orm:hibernate-community-dialects:8.0.0.Alpha1") // [Added dependency] Added as instructed for Hibernate SQLite dialect
	implementation("io.jsonwebtoken:jjwt-api:0.13.0") // [Added dependency] Added as instructed for JSON Web Token API
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0") // [Added dependency] Added as instructed for JSON Web Token implementation
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0") // [Added dependency] Added as instructed for JSON Web Token Jackson mapping
	testImplementation("com.h2database:h2:2.4.240") // [Added dependency] Added as instructed for in-memory H2 database testing
	testImplementation("net.jqwik:jqwik:1.9.3") // [Added dependency] Property-based testing for PasswordValidator
	// the core cucumber code https://mvnrepository.com/artifact/io.cucumber/cucumber-java
    testImplementation("io.cucumber:cucumber-java:7.33.0")
    // the integration code for cucumber & junit https://mvnrepository.com/artifact/io.cucumber/cucumber-junit-platform-engine
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.33.0")
    // the sub module that gives us access to the junit test suite feature https://mvnrepository.com/artifact/org.junit.platform/junit-platform-suite
    testImplementation("org.junit.platform:junit-platform-suite:1.14.1")
	// Source: https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
	implementation("org.seleniumhq.selenium:selenium-java:4.45.0")
	// Source: https://mvnrepository.com/artifact/io.cucumber/cucumber-spring
	implementation("io.cucumber:cucumber-spring:7.34.4")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
	}
}

tasks.generateJava {
	schemaPaths.add("${projectDir}/src/main/resources/graphql-client")
	packageName = "com.revature.todomanagement.codegen"
	generateClient = true
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// tasks.test {
// 	useJUnitPlatform()
// 	systemProperty("cucumber.junit-platform.naming-strategy", "long")
// 	//Pass Cucumber tag filter from command line: ./gradlew test -Dtags="@driver"
// 	systemProperty("cucumber.filter.tags", System.getProperty("tags") ?: "")

// 	// change report output directory: ./gradlew test -DreportDir="my-reports"
// 	val reportDir = System.getProperty("reportDir")
// 	if(reportDir != null) {
// 		reports.html.outputLocation.set(file(reportDir))
// 	}
// }