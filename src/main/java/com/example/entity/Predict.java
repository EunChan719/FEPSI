package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Setter;
import lombok.Getter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Predict {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	private double motion;
	private double saturation;
	private double audio;
	private double emotion;
	private double ai_recall;
	private double dopamine_index;
	
	public Predict(double motion,double saturation, double audio, double emotion, double dopamine_index) {
		this.motion = motion;
		this.audio=audio;
		this.saturation=saturation;
		this.emotion=emotion;
		this.dopamine_index=dopamine_index;
	}
	
	
}