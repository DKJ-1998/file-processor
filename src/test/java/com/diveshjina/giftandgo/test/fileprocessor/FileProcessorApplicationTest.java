package com.diveshjina.giftandgo.test.fileprocessor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = FileProcessorApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileProcessorApplicationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void healthCheckReturns200() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/actuator/health")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

}
