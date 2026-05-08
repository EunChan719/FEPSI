package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.dto.RequestDto;
import com.example.dto.ResponseDto;


@Service
public class AiService {

    private final RestClient restClient;

    public AiService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://localhost:8000")
                .build();
    }

    public ResponseDto predict(RequestDto request) {
        return restClient.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(ResponseDto.class);
    }
}