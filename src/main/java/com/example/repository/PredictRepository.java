package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.entity.Predict;

public interface PredictRepository extends JpaRepository<Predict, Long> {
	@Query("SELECT AVG(p.motion) FROM Predict p")
	Double findMotionAverage();
	
	@Query("SELECT AVG(p.saturation) FROM Predict p")
	Double findSaturationAverage();
	
	@Query("SELECT AVG(p.audio) FROM Predict p")
	Double findAudioAverage();
	
	@Query("SELECT AVG(p.emotion) FROM Predict p")
	Double findEmotionAverage();

	@Query("SELECT AVG(p.dopamine_index) FROM Predict p")
	Double findDopamineIndexAverage();
}