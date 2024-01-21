package com.diveshjina.giftandgo.test.fileprocessor.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import jakarta.servlet.http.HttpServletRequest;

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
public class FileProcessorControllerIT {
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

    @InjectWireMock("ip-api")
    private WireMockServer wireMockServer;

    @Autowired
    FileProcessorController fileProcessorController;

    @Mock
    HttpServletRequest request;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void processFile_ValidFileAndValidIp_HttpStatus200AndAllPersonsInFileReturned(boolean validate) throws JSONException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("uri");
        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", "Sky Italia");
        WireMock.stubFor(WireMock.get("/json/127.0.0.1")
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var actualResponse = fileProcessorController.process(mockFile, validate, request);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        var actualOutputFile = new String(actualResponse.getBody(), StandardCharsets.UTF_8);
        assertEquals(OUTPUT_FILE, actualOutputFile);
    }

    @Test
    void processFile_InvalidFileAndValidIpAndValidate_HttpStatus400Returned() throws JSONException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("uri");
        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INVALID_INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", "Sky Italia");
        WireMock.stubFor(WireMock.get("/json/127.0.0.1")
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var actualResponse = fileProcessorController.process(mockFile, true, request);

        assertEquals(HttpStatus.BAD_REQUEST, actualResponse.getStatusCode());
    }

    @Test
    void processFile_InvalidFileAndValidIpAndDoNotValidate_HttpStatus200AndSomePersonsInFileReturned() throws JSONException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("uri");
        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INVALID_INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", "Sky Italia");
        WireMock.stubFor(WireMock.get("/json/127.0.0.1")
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var actualResponse = fileProcessorController.process(mockFile, false, request);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        var actualOutputFile = new String(actualResponse.getBody(), StandardCharsets.UTF_8);
        assertEquals(PARTIAL_OUTPUT_FILE, actualOutputFile);
    }

    @ParameterizedTest
    @ValueSource(strings = {"China", "Spain", "USA"})
    void processFile_ValidFileAndIpCountryBlockedAndValidate_HttpStatus403AndErrorMessageReturned(String blockedCountry) throws JSONException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("uri");
        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", blockedCountry);
        jsonResponseFromIpApi.put("countryCode", "XX");
        jsonResponseFromIpApi.put("isp", "Oracle");
        WireMock.stubFor(WireMock.get("/json/127.0.0.1")
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var actualResponse = fileProcessorController.process(mockFile, true, request);

        assertEquals(HttpStatus.FORBIDDEN, actualResponse.getStatusCode());
        var actualOutputBody = new String(actualResponse.getBody(), StandardCharsets.UTF_8);
        assertFalse(actualOutputBody.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"China", "Spain", "USA"})
    void processFile_ValidFileAndIpCountryBlockedAndDoNotValidate_HttpStatus200AndAllPersonsInFileReturned(String blockedCountry) throws JSONException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("uri");
        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", blockedCountry);
        jsonResponseFromIpApi.put("countryCode", "XX");
        jsonResponseFromIpApi.put("isp", "Oracle");
        WireMock.stubFor(WireMock.get("/json/127.0.0.1")
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var actualResponse = fileProcessorController.process(mockFile, false, request);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        var actualOutputFile = new String(actualResponse.getBody(), StandardCharsets.UTF_8);
        assertEquals(OUTPUT_FILE, actualOutputFile);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWS", "GCP", "Azure"})
    void processFile_ValidFileAndIpIspBlockedAndValidate_HttpStatus403AndErrorMessageReturned(String blockedIsp) throws JSONException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("uri");
        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", blockedIsp);
        WireMock.stubFor(WireMock.get("/json/127.0.0.1")
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var actualResponse = fileProcessorController.process(mockFile, true, request);

        assertEquals(HttpStatus.FORBIDDEN, actualResponse.getStatusCode());
        var actualOutputBody = new String(actualResponse.getBody(), StandardCharsets.UTF_8);
        assertFalse(actualOutputBody.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWS", "GCP", "Azure"})
    void processFile_ValidFileAndIpIspBlockedAndDoNotValidate_HttpStatus200AndAllPersonsInFileReturned(String blockedIsp) throws JSONException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("uri");
        var mockFile = new MockMultipartFile("file", "EmptyFile.txt", "text/plain", INPUT_FILE.getBytes(StandardCharsets.UTF_8));
        var jsonResponseFromIpApi = new JSONObject();
        jsonResponseFromIpApi.put("country", "Italy");
        jsonResponseFromIpApi.put("countryCode", "IT");
        jsonResponseFromIpApi.put("isp", blockedIsp);
        WireMock.stubFor(WireMock.get("/json/127.0.0.1")
            .willReturn(
                WireMock.ok()
                    .withBody(jsonResponseFromIpApi.toString())
                    .withHeader("Content-Type", "application/json")));

        var actualResponse = fileProcessorController.process(mockFile, false, request);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        var actualOutputFile = new String(actualResponse.getBody(), StandardCharsets.UTF_8);
        assertEquals(OUTPUT_FILE, actualOutputFile);
    }
}
