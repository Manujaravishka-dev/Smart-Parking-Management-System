package com.spms.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"eureka.client.enabled=false", "server.port=0"})
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
