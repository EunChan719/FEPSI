package com.example.dto;


import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredictResultDto {
    private Long id;
    private List<ResultDto> metrics = new ArrayList<>();

    public PredictResultDto(Long id) {
        this.id = id;
    }

    public void addMetric(String name, double value, double average) {
        metrics.add(new ResultDto(name, value, average));
    }

    
}



