package com.example.dto;

import com.example.entity.Predict;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestDto {
	private double motion;
	private double saturation;
	private double audio;
	private double emotion;
	private double dopamine_index;
	public Predict toEntity(double dopamine_index) {
		// TODO Auto-generated method stub
		return new Predict(motion, saturation, audio, emotion, dopamine_index);
	}
	
	
}
