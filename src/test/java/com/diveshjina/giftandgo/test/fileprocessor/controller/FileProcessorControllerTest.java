package com.diveshjina.giftandgo.test.fileprocessor.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import com.diveshjina.giftandgo.test.fileprocessor.dto.IpDetailsDto;
import com.diveshjina.giftandgo.test.fileprocessor.exceptions.InvalidFileException;
import com.diveshjina.giftandgo.test.fileprocessor.exceptions.IpBlockedException;
import com.diveshjina.giftandgo.test.fileprocessor.service.FileProcessorService;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class FileProcessorControllerTest {
    private static final String IP_ADDRESS = "127.0.0.1";

    @Autowired
    private FileProcessorController fileProcessorController;

    @MockBean
    private FileProcessorService fileProcessorService;

    @Mock
    private MultipartFile inputFile;

    @Mock
    private HttpServletRequest request;

    @Mock
    private IpDetailsDto ipDetailsDto;

    @BeforeEach
    void beforeEach() {
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
        when(fileProcessorService.getIpDetails(IP_ADDRESS)).thenReturn(ipDetailsDto);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void process_ValidFile_HttpStatus200AndProcessedFileReturned(boolean validate) throws InvalidFileException, IOException, IpBlockedException {
        var returnedBytes = "returned".getBytes(StandardCharsets.UTF_8);
        when(fileProcessorService.processFile(inputFile, validate)).thenReturn(returnedBytes);

        var actualResponse = fileProcessorController.process(inputFile, validate, request);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        assertEquals(returnedBytes, actualResponse.getBody());
        verify(fileProcessorService).validateIp(ipDetailsDto, validate);
        verify(fileProcessorService).saveRequestDetails(eq(request), any(LocalDateTime.class), eq(ipDetailsDto), any(LocalDateTime.class), eq(actualResponse));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void process_invalidFile_HttpStatus400Returned(boolean validate) throws InvalidFileException, IOException, IpBlockedException {
        doThrow(InvalidFileException.class).when(fileProcessorService).processFile(inputFile, validate);

        var actualResponse = fileProcessorController.process(inputFile, validate, request);

        assertEquals(HttpStatus.BAD_REQUEST, actualResponse.getStatusCode());
        verify(fileProcessorService).validateIp(ipDetailsDto, validate);
        verify(fileProcessorService).saveRequestDetails(eq(request), any(LocalDateTime.class), eq(ipDetailsDto), any(LocalDateTime.class), eq(actualResponse));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void process_errorReadingFile_HttpStatus500Returned(boolean validate) throws InvalidFileException, IOException, IpBlockedException {
        doThrow(IOException.class).when(fileProcessorService).processFile(inputFile, validate);

        var actualResponse = fileProcessorController.process(inputFile, validate, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, actualResponse.getStatusCode());
        verify(fileProcessorService).validateIp(ipDetailsDto, validate);
        verify(fileProcessorService).saveRequestDetails(eq(request), any(LocalDateTime.class), eq(ipDetailsDto), any(LocalDateTime.class), eq(actualResponse));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void process_ipBlocked_HttpStatus403Returned(boolean validate) throws InvalidFileException, IOException, IpBlockedException {
        doThrow(IpBlockedException.class).when(fileProcessorService).validateIp(ipDetailsDto, validate);

        var actualResponse = fileProcessorController.process(inputFile, validate, request);

        assertEquals(HttpStatus.FORBIDDEN, actualResponse.getStatusCode());
        verify(fileProcessorService).validateIp(ipDetailsDto, validate);
        verify(fileProcessorService, times(0)).processFile(inputFile, validate);
        verify(fileProcessorService).saveRequestDetails(eq(request), any(LocalDateTime.class), eq(ipDetailsDto), any(LocalDateTime.class), eq(actualResponse));
    }
}