package com.example.dto;

import com.example.entity.Video;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VideoDto {
	private Long id;
	private String videopath;
	private byte[] videoData;
	public Video toEntity() {
		// TODO Auto-generated method stub
	return new Video(id, videopath, videoData);
	}

}
