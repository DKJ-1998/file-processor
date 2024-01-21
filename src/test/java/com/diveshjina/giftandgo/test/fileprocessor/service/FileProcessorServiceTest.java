package com.diveshjina.giftandgo.test.fileprocessor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import com.diveshjina.giftandgo.test.fileprocessor.client.IPAPIClient;
import com.diveshjina.giftandgo.test.fileprocessor.configuration.BlockedProperties;
import com.diveshjina.giftandgo.test.fileprocessor.dto.IpDetailsDto;
import com.diveshjina.giftandgo.test.fileprocessor.exceptions.InvalidFileException;
import com.diveshjina.giftandgo.test.fileprocessor.exceptions.IpBlockedException;
import com.diveshjina.giftandgo.test.fileprocessor.repository.Request;
import com.diveshjina.giftandgo.test.fileprocessor.repository.RequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class FileProcessorServiceTest {
    private static final String VALID_FILE_STRING = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35.0|95.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String PERSONS_JSON = "[" +
        "{\"name\":\"John Smith\",\"transport\":\"Rides A Bike\",\"topSpeed\":\"12.1\"}," +
        "{\"name\":\"Mike Smith\",\"transport\":\"Drives an SUV\",\"topSpeed\":\"95.5\"}," +
        "{\"name\":\"Jenny Walters\",\"transport\":\"Rides A Scooter\",\"topSpeed\":\"15.3\"}" +
        "]";
    private static final String INVALID_FILE_STRING_1 = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35.0|95.5something on the end
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String INVALID_FILE_STRING_2 = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        something at the start3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35.0|95.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String INVALID_FILE_STRING_3 = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35.0|05.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String INVALID_FILE_STRING_4 = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith|Likes Grape|Drives an SUV|05.0|95.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String INVALID_FILE_STRING_5 = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith 123|Likes Grape|Drives an SUV|35.0|95.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String INVALID_FILE_STRING_6 = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35|95.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String INVALID_FILE_STRING_7 = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222c|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35.0|95.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String INVALID_FILE_STRING_8 = """
        18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1
        3ce2d17b-e66a-4c1e-bca3-40eb1c9222cx|2X2D24|Mike Smith|Likes Grape|Drives an SUV|35.0|95.5
        1afb6f5d-a7c2-4311-a92d-974f3180ff5e|3X3D35|Jenny Walters|Likes Avocados|Rides A Scooter|8.5|15.3""";
    private static final String SOME_PERSONS_JSON = "[" +
        "{\"name\":\"John Smith\",\"transport\":\"Rides A Bike\",\"topSpeed\":\"12.1\"}," +
        "{\"name\":\"Jenny Walters\",\"transport\":\"Rides A Scooter\",\"topSpeed\":\"15.3\"}" +
        "]";
    private static final String INVALID_FILE_STRING_ALL_LINES_INVALID = """
        totally invalid line 1
        totally invalid line 2
        totally invalid line 3""";
    private static final String NO_PERSONS_JSON = "[]";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final IpDetailsDto VALID_IP_DETAILS = new IpDetailsDto("Italy", "Sky Italia", "IT");
    private static final IpDetailsDto BLOCKED_COUNTRY_IP_DETAILS = new IpDetailsDto("China", "Alibaba", "CH");
    private static final Set<String> BLOCKED_COUNTRIES = Set.of("China", "Spain", "USA");
    private static final IpDetailsDto BLOCKED_ISP_IP_DETAILS = new IpDetailsDto("United Kingdom", "AWS", "GB");
    private static final Set<String> BLOCKED_ISPS = Set.of("AWS", "GCP", "Azure");

    @Autowired
    FileProcessorService fileProcessorService;

    @MockBean
    IPAPIClient ipApiClient;

    @MockBean
    BlockedProperties blockedProperties;

    @MockBean
    RequestRepository requestRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Mock
    MultipartFile file;

    @Test
    void processFile_ValidFileAndValidate_PersonsReturned() throws InvalidFileException, IOException {
        var bytesInFile = VALID_FILE_STRING.getBytes(StandardCharsets.UTF_8);
        when(file.getBytes()).thenReturn(bytesInFile);

        var actualPersonsBytes = fileProcessorService.processFile(file, true);

        var expectedPersonsBytes = PERSONS_JSON.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expectedPersonsBytes, actualPersonsBytes);
    }

    @Test
    void processFile_ValidFileAndDoNotValidate_PersonsReturned() throws InvalidFileException, IOException {
        var bytesInFile = VALID_FILE_STRING.getBytes(StandardCharsets.UTF_8);
        when(file.getBytes()).thenReturn(bytesInFile);

        var actualPersonsBytes = fileProcessorService.processFile(file, false);

        var expectedPersonsBytes = PERSONS_JSON.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expectedPersonsBytes, actualPersonsBytes);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        INVALID_FILE_STRING_1, INVALID_FILE_STRING_2, INVALID_FILE_STRING_3, INVALID_FILE_STRING_4,
        INVALID_FILE_STRING_5, INVALID_FILE_STRING_6, INVALID_FILE_STRING_7, INVALID_FILE_STRING_8,
    })
    void processFile_someInvalidLinesInFileAndValidate_InvalidFileExceptionThrown(String invalidFileString) throws IOException {
        var bytesInFile = invalidFileString.getBytes(StandardCharsets.UTF_8);
        when(file.getBytes()).thenReturn(bytesInFile);

        assertThrows(InvalidFileException.class, () -> fileProcessorService.processFile(file, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        INVALID_FILE_STRING_1, INVALID_FILE_STRING_2, INVALID_FILE_STRING_3, INVALID_FILE_STRING_4,
        INVALID_FILE_STRING_5, INVALID_FILE_STRING_6, INVALID_FILE_STRING_7, INVALID_FILE_STRING_8,
    })
    void processFile_someInvalidLinesInFileAndDoNotValidate_ValidPersonsReturned(String invalidFileString) throws InvalidFileException, IOException {
        var bytesInFile = invalidFileString.getBytes(StandardCharsets.UTF_8);
        when(file.getBytes()).thenReturn(bytesInFile);

        var actualPersonsBytes = fileProcessorService.processFile(file, false);

        var expectedPersonsBytes = SOME_PERSONS_JSON.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expectedPersonsBytes, actualPersonsBytes);
    }

    @Test
    void processFile_allLinesInFileInvalidAndValidate_InvalidFileExceptionThrown() throws IOException {
        var bytesInFile = INVALID_FILE_STRING_ALL_LINES_INVALID.getBytes(StandardCharsets.UTF_8);
        when(file.getBytes()).thenReturn(bytesInFile);

        assertThrows(InvalidFileException.class, () -> fileProcessorService.processFile(file, true));
    }

    @Test
    void processFile_someInvalidLinesInFileAndDoNotValidate_NoPersonsReturned() throws InvalidFileException, IOException {
        var bytesInFile = INVALID_FILE_STRING_ALL_LINES_INVALID.getBytes(StandardCharsets.UTF_8);
        when(file.getBytes()).thenReturn(bytesInFile);

        var actualPersonsBytes = fileProcessorService.processFile(file, false);

        var expectedPersonsBytes = NO_PERSONS_JSON.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expectedPersonsBytes, actualPersonsBytes);
    }

    @Test
    void getIpDetails_IpAddressGiven_IpDetailsReturned() {
        when(ipApiClient.getIpDetails(IP_ADDRESS)).thenReturn(VALID_IP_DETAILS);

        var actualIpDetails = fileProcessorService.getIpDetails(IP_ADDRESS);

        assertEquals(VALID_IP_DETAILS, actualIpDetails);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validateIp_ValidIpAddress_NoExceptionThrown(boolean validate) {
         assertDoesNotThrow(() -> fileProcessorService.validateIp(VALID_IP_DETAILS, validate));
    }

    @Test
    void validateIp_CountryBlockedAndValidate_IpBlockedExceptionThrown() {
        when(blockedProperties.countries()).thenReturn(BLOCKED_COUNTRIES);

        assertThrows(IpBlockedException.class, () -> fileProcessorService.validateIp(BLOCKED_COUNTRY_IP_DETAILS, true));
    }

    @Test
    void validateIp_CountryBlockedAndDoNotValidate_NoExceptionThrown() {
        when(blockedProperties.countries()).thenReturn(BLOCKED_COUNTRIES);

        assertDoesNotThrow(() -> fileProcessorService.validateIp(BLOCKED_COUNTRY_IP_DETAILS, false));
        verify(blockedProperties, times(0)).countries();
        verify(blockedProperties, times(0)).isps();
    }

    @Test
    void validateIp_IspBlockedAndValidate_IpBlockedExceptionThrown() {
        when(blockedProperties.isps()).thenReturn(BLOCKED_ISPS);

        assertThrows(IpBlockedException.class, () -> fileProcessorService.validateIp(BLOCKED_ISP_IP_DETAILS, true));
    }

    @Test
    void validateIp_IspBlockedAndDoNotValidate_NoExceptionThrown() {
        when(blockedProperties.isps()).thenReturn(BLOCKED_ISPS);

        assertDoesNotThrow(() -> fileProcessorService.validateIp(BLOCKED_ISP_IP_DETAILS, false));
        verify(blockedProperties, times(0)).countries();
        verify(blockedProperties, times(0)).isps();
    }

    @Captor
    ArgumentCaptor<Request> requestCaptor;

    @Test
    void saveRequestDetails_DetailsToSave_DetailsSavedToRepository() {
        var request = mock(HttpServletRequest.class);
        var responseEntity = ResponseEntity.status(200).body("returned".getBytes(StandardCharsets.UTF_8));
        when(request.getRequestURI()).thenReturn("uri");
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        var startTime = LocalDateTime.of(2024, 1, 21, 21, 30, 0, 0);
        var endTime = LocalDateTime.of(2024, 1, 21, 21, 30, 0, 567000000);

        fileProcessorService.saveRequestDetails(request, startTime, VALID_IP_DETAILS, endTime, responseEntity);

        verify(requestRepository).save(requestCaptor.capture());
        var actualRequest = requestCaptor.getValue();
        assertEquals("uri", actualRequest.getRequestUri());
        assertEquals(Timestamp.valueOf( "2024-01-21 21:30:00.000000000"), actualRequest.getRequestTimestamp());
        assertEquals(200, actualRequest.getHttpResponseCode());
        assertEquals(IP_ADDRESS, actualRequest.getRequestIpAddress());
        assertEquals(VALID_IP_DETAILS.countryCode(), actualRequest.getRequestCountryCode());
        assertEquals(VALID_IP_DETAILS.isp(), actualRequest.getRequestIpProvider());
        assertEquals(567, actualRequest.getTimeLapsedInMillis());
    }
}