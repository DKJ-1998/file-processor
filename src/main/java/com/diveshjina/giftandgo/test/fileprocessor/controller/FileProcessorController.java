package com.diveshjina.giftandgo.test.fileprocessor.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import com.diveshjina.giftandgo.test.fileprocessor.exceptions.InvalidFileException;
import com.diveshjina.giftandgo.test.fileprocessor.exceptions.IpBlockedException;
import com.diveshjina.giftandgo.test.fileprocessor.service.FileProcessorService;

@Controller
public class FileProcessorController {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessorController.class);

    private final FileProcessorService fileProcessorService;

    public FileProcessorController(FileProcessorService fileProcessorService) {
        this.fileProcessorService = fileProcessorService;
    }

    @PostMapping("/v0/process")
    public ResponseEntity<byte[]> process(@RequestParam MultipartFile file, @RequestParam boolean validate, HttpServletRequest request) {
        var startTime = LocalDateTime.now();
        var ipDetails = fileProcessorService.getIpDetails(request.getRemoteAddr());
        logger.info("Received file {}", file.getOriginalFilename());
        ResponseEntity<byte[]> responseEntity;
        try {
            fileProcessorService.validateIp(ipDetails, validate);
            var outcomeFile = fileProcessorService.processFile(file, validate);
            logger.info("Processed file {}", file.getOriginalFilename());
            responseEntity = ResponseEntity.ok(outcomeFile);
        } catch (IOException ex) {
            logger.error("Failed to process file {}", file.getOriginalFilename(), ex);
            responseEntity = ResponseEntity.internalServerError().build();
        } catch (InvalidFileException ex) {
            logger.error("File {} invalid", file.getOriginalFilename(), ex);
            responseEntity = ResponseEntity.badRequest().build();
        } catch (IpBlockedException ex) {
            logger.error("Ip {} blocked", request.getRemoteAddr(), ex);
            responseEntity = ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(("Error: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
        var endTime = LocalDateTime.now();
        fileProcessorService.saveRequestDetails(request, startTime, ipDetails, endTime, responseEntity);
        return responseEntity;
    }
}
