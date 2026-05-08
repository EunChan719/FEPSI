package com.example.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultDto {
    private String name;
    private double value;
    private double average;
    private double result;
    private String message;

    public ResultDto(String name, double value, double average) {
        this.name = name;
        this.value = Math.round((value) * 100) / 100.0;
        this.average = Math.round((average) * 100) / 100.0;
        this.result = (Math.round((value / average) * 10000) / 10000.0)*100;
        this.message = this.result <= 1
                ? name + "은 개선이 필요합니다."
                : name + "은 평균 이상입니다.";
    }

}
