package com.example.service;



import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.Predict;
import com.example.repository.PredictRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PredictService {
	@Autowired
	private PredictRepository predictRepository;

	
	
}
