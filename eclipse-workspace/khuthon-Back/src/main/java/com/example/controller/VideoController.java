package com.example.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.dto.PredictResultDto;
import com.example.dto.RequestDto;
import com.example.dto.ResponseDto;
import com.example.dto.VideoDto;
import com.example.entity.Predict;
import com.example.entity.Video;
import com.example.repository.PredictRepository;
import com.example.repository.VideoRepository;
import com.example.service.AiService;
import com.example.service.PredictService;

import org.springframework.ui.Model;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Controller
public class VideoController {
	@Autowired
	private VideoRepository videoRepository;
	@Autowired
	private PredictRepository predictRepository;
	@Autowired
	private PredictService predictService;
	@Autowired
	private AiService aiService;
	
	@GetMapping("/videos")
	public String uploadForm() {
	    return "video/show";
	}
	
	@PostMapping("/videos")
	public String show(@RequestParam("video") MultipartFile video, Model model) throws IOException  {
	    if (video.isEmpty()) {
	        model.addAttribute("message", "파일을 선택해주세요.");
	        return "video/upload";
	    }
		
	    String uploadDir = "C:/upload/";

	    File dir = new File(uploadDir);

	    if (!dir.exists()) {
	        dir.mkdirs();
	    }

	    String fileName = "test.mp4";
	    
	    File saveFile = new File(dir, fileName);

	    video.transferTo(saveFile);

	    model.addAttribute("message", "업로드 성공");
	    model.addAttribute("path", saveFile.getAbsolutePath());

		return "video/push";
	}
	
	@PostMapping("/videos/create")
	public String create(VideoDto dto) {
		Video video = dto.toEntity();
		Video created = videoRepository.save(video);
		
		return "redirect:/video/"+video.getId();
	}
	
	@GetMapping("/video/{id}") 
	public String result(Model model,@PathVariable("id") Long id) {
//		Predict predict = predictService.result(id);
		Predict predict = predictRepository.findById(id).orElse(null);  
			
		Double motionAverage = predictRepository.findMotionAverage();
	    Double saturationAverage = predictRepository.findSaturationAverage();
	    Double audioAverage = predictRepository.findAudioAverage();
	    Double emotionAverage = predictRepository.findEmotionAverage();
	    Double dopamineAverage = predictRepository.findDopamineIndexAverage();

	    PredictResultDto dto = new PredictResultDto(predict.getId());

        dto.addMetric("motion", predict.getMotion(), motionAverage);
	    dto.addMetric("saturation", predict.getSaturation(), saturationAverage);
        dto.addMetric("audio", predict.getAudio(), audioAverage);
        dto.addMetric("emotion", predict.getEmotion(), emotionAverage);
        dto.addMetric("dopamine_index", predict.getDopamine_index(), dopamineAverage);


	    model.addAttribute("dto",dto);
		return "video/result";
	}
	
	@GetMapping("/videos/list")
	public String create(Model model) {        
		Predict predict1 = predictRepository.findById(1L).orElse(null);
		Predict predict2 = predictRepository.findById(2L).orElse(null);
		
	    Double motionAverage = predictRepository.findMotionAverage();
	    Double saturationAverage = predictRepository.findSaturationAverage();
	    Double audioAverage = predictRepository.findAudioAverage();
	    Double emotionAverage = predictRepository.findEmotionAverage();
	    Double dopamineAverage = predictRepository.findDopamineIndexAverage();

		List<Predict> predictList = new ArrayList<>();
		
//		predictList.add(predict1);
		predictList.add(predict2);
		predictList.add(predict1);

		List<PredictResultDto> resultList = new ArrayList<>();

		  for (Predict predict : predictList) {
		       PredictResultDto dto = new PredictResultDto(predict.getId());

		        dto.addMetric("motion", predict.getMotion(), motionAverage);
		        dto.addMetric("saturation", predict.getSaturation(), saturationAverage);
		        dto.addMetric("audio", predict.getAudio(), audioAverage);
		        dto.addMetric("emotion", predict.getEmotion(), emotionAverage);
		        dto.addMetric("dopamine_index", predict.getDopamine_index(), dopamineAverage);

		        resultList.add(dto);
		    }

		    model.addAttribute("predictList", resultList);
		
		return "video/list";
	}
}