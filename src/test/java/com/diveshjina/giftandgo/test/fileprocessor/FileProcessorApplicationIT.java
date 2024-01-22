package com.diveshjina.giftandgo.test.fileprocessor;

import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test", "wiremock-test"})
@EnableWireMock(@ConfigureWireMock(name = "ip-api"))
@WireMockTest(httpPort = 8081)
@AutoConfigureMockMvc
public class FileProcessorApplicationIT {
    private static final String INPUT_FILE = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35.0|95.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String INVALID_INPUT_FILE = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35.0|95.5something on the end
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String OUTPUT_FILE = "[{\"name\":\"John Smith\",\"transport\":\"Rides A Bike\",\"topSpeed\":\"12.1\"},{\"name\":\"Mike Smith\",\"transport\":\"Drives an SUV\",\"topSpeed\":\"95.5\"},{\"name\":\"Jenny Walters\",\"transport\":\"Rides A Scooter\",\"topSpeed\":\"15.3\"}]";
    private static final String PARTIAL_OUTPUT_FILE = "[{\"name\":\"John Smith\",\"transport\":\"Rides A Bike\",\"topSpeed\":\"12.1\"},{\"name\":\"Jenny Walters\",\"transport\":\"Rides A Scooter\",\"topSpeed\":\"15.3\"}]";
    private static final String IP_ADDRESS = "31.63.127.255";

    @Autowired
    MockMvc mockMvc;

    @InjectWireMock("ip-api")
    private WireMockServer wireMockServer;

    @Test
    void actuatorhealth_HealthCheck_HttpStatus200AndUpReturned() throws Exception {
        var mockRequest = MockMvcRequestBuilders.get("/actuator/health")
            .accept(MediaType.APPLICATION_JSON);
        mockMvc.perform(mockRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void v0process_ValidFileAndValidIp_HttpStatus200AndAllPersonsInFileReturned(boolean validate) throws Exception {
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", "Sky Italia");
        WireMock.stubFor(WireMock.get(String.format("/json/%s", IP_ADDRESS))
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var mockRequest = MockMvcRequestBuilders.multipart("/v0/process")
            .file(mockFile)
            .part(new MockPart("validate", String.valueOf(validate).getBytes(StandardCharsets.UTF_8)))
            .with(request -> {request.setRemoteAddr(IP_ADDRESS); return request;});

        mockMvc.perform(mockRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().bytes(OUTPUT_FILE.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void v0process_InvalidFileAndValidIpAndValidate_HttpStatus400Returned() throws Exception {
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", "Sky Italia");
        WireMock.stubFor(WireMock.get(String.format("/json/%s", IP_ADDRESS))
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INVALID_INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var mockRequest = MockMvcRequestBuilders.multipart("/v0/process")
            .file(mockFile)
            .part(new MockPart("validate", "true".getBytes(StandardCharsets.UTF_8)))
            .with(request -> {request.setRemoteAddr(IP_ADDRESS); return request;});

        mockMvc.perform(mockRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void v0process_InvalidFileAndValidIpAndDoNotValidate_HttpStatus200AndSomePersonsInFileReturned() throws Exception {
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", "Sky Italia");
        WireMock.stubFor(WireMock.get(String.format("/json/%s", IP_ADDRESS))
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INVALID_INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var mockRequest = MockMvcRequestBuilders.multipart("/v0/process")
            .file(mockFile)
            .part(new MockPart("validate", "false".getBytes(StandardCharsets.UTF_8)))
            .with(request -> {request.setRemoteAddr(IP_ADDRESS); return request;});

        mockMvc.perform(mockRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().bytes(PARTIAL_OUTPUT_FILE.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"China", "Spain", "USA"})
    void v0process_ValidFileAndIpCountryBlockedAndValidate_HttpStatus403AndErrorMessageReturned(String blockedCountry) throws Exception {
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", blockedCountry);
        jsonResponseFromIpApi.put("countryCode", "XX");
        jsonResponseFromIpApi.put("isp", "Oracle");
        WireMock.stubFor(WireMock.get(String.format("/json/%s", IP_ADDRESS))
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var mockRequest = MockMvcRequestBuilders.multipart("/v0/process")
            .file(mockFile)
            .part(new MockPart("validate", "true".getBytes(StandardCharsets.UTF_8)))
            .with(request -> {request.setRemoteAddr(IP_ADDRESS); return request;});

        mockMvc.perform(mockRequest)
            .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"China", "Spain", "USA"})
    void v0process_ValidFileAndIpCountryBlockedAndDoNotValidate_HttpStatus200AndAllPersonsInFileReturned(String blockedCountry) throws Exception {
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", blockedCountry);
        jsonResponseFromIpApi.put("countryCode", "XX");
        jsonResponseFromIpApi.put("isp", "Oracle");
        WireMock.stubFor(WireMock.get(String.format("/json/%s", IP_ADDRESS))
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var mockRequest = MockMvcRequestBuilders.multipart("/v0/process")
            .file(mockFile)
            .part(new MockPart("validate", "false".getBytes(StandardCharsets.UTF_8)))
            .with(request -> {request.setRemoteAddr(IP_ADDRESS); return request;});

        mockMvc.perform(mockRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().bytes(OUTPUT_FILE.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWS", "GCP", "Azure"})
    void v0process_ValidFileAndIpIspBlockedAndValidate_HttpStatus403AndErrorMessageReturned(String blockedIsp) throws Exception {
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", blockedIsp);
        WireMock.stubFor(WireMock.get(String.format("/json/%s", IP_ADDRESS))
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var mockRequest = MockMvcRequestBuilders.multipart("/v0/process")
            .file(mockFile)
            .part(new MockPart("validate", "true".getBytes(StandardCharsets.UTF_8)))
            .with(request -> {request.setRemoteAddr(IP_ADDRESS); return request;});

        mockMvc.perform(mockRequest)
            .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWS", "GCP", "Azure"})
    void v0process_ValidFileAndIpIspBlockedAndDoNotValidate_HttpStatus200AndAllPersonsInFileReturned(String blockedIsp) throws Exception {
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", blockedIsp);
        WireMock.stubFor(WireMock.get(String.format("/json/%s", IP_ADDRESS))
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var mockRequest = MockMvcRequestBuilders.multipart("/v0/process")
            .file(mockFile)
            .part(new MockPart("validate", "false".getBytes(StandardCharsets.UTF_8)))
            .with(request -> {request.setRemoteAddr(IP_ADDRESS); return request;});

        mockMvc.perform(mockRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().bytes(OUTPUT_FILE.getBytes(StandardCharsets.UTF_8)));
    }
}
