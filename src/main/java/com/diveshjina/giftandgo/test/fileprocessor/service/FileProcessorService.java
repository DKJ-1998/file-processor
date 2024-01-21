package com.diveshjina.giftandgo.test.fileprocessor.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;

import com.diveshjina.giftandgo.test.fileprocessor.client.IPAPIClient;
import com.diveshjina.giftandgo.test.fileprocessor.configuration.BlockedProperties;
import com.diveshjina.giftandgo.test.fileprocessor.dto.IpDetailsDto;
import com.diveshjina.giftandgo.test.fileprocessor.dto.PersonDto;
import com.diveshjina.giftandgo.test.fileprocessor.exceptions.InvalidFileException;
import com.diveshjina.giftandgo.test.fileprocessor.exceptions.IpBlockedException;
import com.diveshjina.giftandgo.test.fileprocessor.repository.Request;
import com.diveshjina.giftandgo.test.fileprocessor.repository.RequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FileProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessorService.class);

    private static final String LINE_MATCHING_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\|[0-9A-Z]{6}\\|([A-Za-z ]+)\\|Likes [A-Za-z ]+\\|([A-Za-z ]+)\\|(?:[0-9]|[1-9][0-9]+)\\.[0-9]\\|((?:[0-9]|[1-9][0-9]+)\\.[0-9])$";

    private final IPAPIClient ipApiClient;
    private final BlockedProperties blockedProperties;
    private final RequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    public FileProcessorService(IPAPIClient ipApiClient, BlockedProperties blockedProperties, RequestRepository requestRepository, ObjectMapper objectMapper) {
        this.ipApiClient = ipApiClient;
        this.blockedProperties = blockedProperties;
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
    }

    public byte[] processFile(MultipartFile file, boolean validate) throws IOException, InvalidFileException {
        var fileBytes = file.getBytes();
        var fileLines = Arrays.asList(
            new String(fileBytes, StandardCharsets.UTF_8)
                .split("\n"));
        var persons = getPersons(fileLines, validate);
        return objectMapper.writeValueAsBytes(persons);
    }

    public IpDetailsDto getIpDetails(String ip) {
        return ipApiClient.getIpDetails(ip);
    }

    public void validateIp(IpDetailsDto ipDetails, boolean validate) throws IpBlockedException {
        if (validate) {
            if (blockedProperties.countries().contains(ipDetails.country())) {
                throw new IpBlockedException(String.format("Request from %s not allowed", ipDetails.country()));
            }
            if (blockedProperties.isps().contains(ipDetails.isp())) {
                throw new IpBlockedException(String.format("Request from %s not allowed", ipDetails.isp()));
            }
        }
    }

    public void saveRequestDetails(HttpServletRequest request, LocalDateTime startTime, IpDetailsDto ipDetails, LocalDateTime endTime, ResponseEntity<byte[]> responseEntity) {
        var timeLapsed = Duration.between(startTime, endTime).toMillis();
        var requestEntity = new Request(
            UUID.randomUUID(),
            request.getRequestURI(),
            Timestamp.valueOf(startTime),
            responseEntity.getStatusCode().value(),
            request.getRemoteAddr(),
            ipDetails.countryCode(),
            ipDetails.isp(),
            timeLapsed
        );
        requestRepository.save(requestEntity);
    }

    private List<PersonDto> getPersons(List<String> fileLines, boolean validate) {
        if (validate) {
            logger.info("Getting persons with validation of file");
            return fileLines.stream()
                .map(this::getPersonFromLineWithValidation)
                .toList();
        }
        logger.info("Getting persons without validation of file");
        return fileLines.stream()
            .map(this::getPersonFromLine)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    @SneakyThrows(InvalidFileException.class)
    private PersonDto getPersonFromLineWithValidation(String line) {
        var person = getPersonFromLine(line);
        return person.orElseThrow(() -> new InvalidFileException(String.format("Line invalid: %s", line)));
    }

    private Optional<PersonDto> getPersonFromLine(String line) {
        var pattern = Pattern.compile(LINE_MATCHING_PATTERN);
        var matcher = pattern.matcher(line);
        if (matcher.find()) {
            var name = matcher.group(1);
            var transport = matcher.group(2);
            var topSpeed = matcher.group(3);
            return Optional.of(new PersonDto(name, transport, topSpeed));
        }
        return Optional.empty();
    }
}
