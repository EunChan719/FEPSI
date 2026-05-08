package com.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResponseDto {
	private Long id;
    private double motion;
    private double saturation;
    private double audio;
    private double emotion;
    private double ai_recall;
    private double dopamine_index;

}