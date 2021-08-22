package com.km.bottlecapcollector;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;


@SpringBootTest(args = "--app.test=one", webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "/test.properties")
class BottleCapCollectorApplicationTests {
	@Autowired
	private WebApplicationContext context;

	@LocalServerPort
	private int port;

	@Value("${local.management.port}")
	private int actuatorPort;

	@Value("${test-value}")
	String testValue$;

	@Value("#{'${test-value}'}")
	String testValueHash;

	private MockMvc mvc;

	@Before
	public void setup() {
		mvc = MockMvcBuilders
				.webAppContextSetup(context)
				.apply(springSecurity()) // enable security for the mock set up
				.build();
	}

	@Test
	void portDefinedTest() {
		assertEquals(8080, port);
	}

	@Test
	void actuatorPortDefinedTest() {
		assertEquals(8080, actuatorPort);
	}

	@Test
	void applicationArgumentsPopulatedTest(@Autowired ApplicationArguments args) {
		assertThat(args.getOptionNames()).containsOnly("app.test");
		assertThat(args.getOptionValues("app.test")).containsOnly("one");
	}

	@Test
	void valueFromProperties$Test(){
		assertThat(testValue$).isEqualTo("test");
	}

	@Test
	void valueFromPropertiesHashTest(){
		assertThat(testValueHash).isEqualTo("test");
	}

}
