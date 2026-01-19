package com.goalmond.auth.config

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * gRPC 클라이언트 설정
 */
@Configuration
class GrpcConfig {
    
    @Bean
    fun userServiceChannel(
        @Value("\${grpc.user-service.host:localhost}") host: String,
        @Value("\${grpc.user-service.port:9090}") port: Int
    ): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()
    }
    
    @Bean
    fun auditServiceChannel(
        @Value("\${grpc.audit-service.host:localhost}") host: String,
        @Value("\${grpc.audit-service.port:9091}") port: Int
    ): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()
    }
}
