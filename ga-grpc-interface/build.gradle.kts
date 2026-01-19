plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    id("io.spring.dependency-management")
}

dependencies {
    // gRPC
    implementation("io.grpc:grpc-stub:1.60.1")
    implementation("io.grpc:grpc-protobuf:1.60.1")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("com.google.protobuf:protobuf-java:3.25.1")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.60.1"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
                create("grpckt")
            }
            // Java는 기본적으로 생성되므로 builtins 설정 불필요
            // Kotlin은 grpc-kotlin-stub을 통해 사용
        }
    }
}

// Proto files are automatically detected from src/main/proto directory
// No need to explicitly configure sourceSets

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
