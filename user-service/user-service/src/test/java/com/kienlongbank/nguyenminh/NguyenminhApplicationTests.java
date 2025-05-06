package com.kienlongbank.nguyenminh;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.kienlongbank.nguyenminh.config.TestTracerConfig;

@SpringBootTest(classes = {UserServiceApplication.class, TestTracerConfig.class})
public class NguyenminhApplicationTests {

	@Test
	void contextLoads() {
	}

}
