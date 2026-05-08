package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Predict;

public interface PredictRepository extends JpaRepository<Predict, Long> {
	
}