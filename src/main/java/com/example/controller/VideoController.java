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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


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
	
	@GetMapping("/test-ai")
	@ResponseBody
	public String testAi() {
	    RestTemplate restTemplate = new RestTemplate();
	    String url = "http://127.0.0.1:8000/docs";
	    return "FastAPI 연결 시도 완료";
	}
	
	@GetMapping("/videos")
	public String uploadForm() {
	    return "video/show";
	}
	
	
	@GetMapping("/api/test/videos")
	@ResponseBody
	public List<Predict> getTestVideos() {
	    List<Predict> list = new ArrayList<>();
	    predictRepository.findAll().forEach(list::add);
	    return list;
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
	    
	    RestTemplate restTemplate = new RestTemplate();
	    String fastApiUrl = "http://127.0.0.1:8000/analyze";
	    
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	    body.add("file", new FileSystemResource(saveFile));

	    HttpEntity<MultiValueMap<String, Object>> requestEntity =
	            new HttpEntity<>(body, headers);

	    ResponseDto response = restTemplate.postForObject(
	            fastApiUrl,
	            requestEntity,
	            ResponseDto.class
	    );
	    
	    
	    Predict predict = new Predict();

	    predict.setMotion(response.getMotion());
	    predict.setSaturation(response.getSaturation());
	    predict.setAudio(response.getAudio());
	    predict.setEmotion(response.getEmotion());
	    predict.setAi_recall(response.getAi_recall());
	    predict.setDopamine_index(response.getDopamine_index());

	    Predict saved = predictRepository.save(predict);
	    
//	    return "redirect:/video/" + saved.getId();
//
//	    model.addAttribute("message", "업로드 성공");
//	    model.addAttribute("path", saveFile.getAbsolutePath());

		return "video/push";
	}
	
	
	
	@PostMapping("/api/videos")
	@ResponseBody
	public ResponseDto analyzeVideo(@RequestParam("video") MultipartFile video) throws IOException {
	    if (video.isEmpty()) {
	        throw new RuntimeException("파일을 선택해주세요.");
	    }

	    String uploadDir = "C:/upload/";
	    File dir = new File(uploadDir);

	    if (!dir.exists()) {
	        dir.mkdirs();
	    }

	    String fileName = "test.mp4";
	    File saveFile = new File(dir, fileName);

	    video.transferTo(saveFile);

	    RestTemplate restTemplate = new RestTemplate();
	    String fastApiUrl = "http://127.0.0.1:8000/analyze";

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	    body.add("file", new FileSystemResource(saveFile));

	    HttpEntity<MultiValueMap<String, Object>> requestEntity =
	            new HttpEntity<>(body, headers);

	    ResponseDto response = restTemplate.postForObject(
	            fastApiUrl,
	            requestEntity,
	            ResponseDto.class
	    );

	    Predict predict = new Predict();

	    predict.setMotion(response.getMotion());
	    predict.setSaturation(response.getSaturation());
	    predict.setAudio(response.getAudio());
	    predict.setEmotion(response.getEmotion());
	    predict.setAi_recall(response.getAi_recall());
	    predict.setDopamine_index(response.getDopamine_index());

	    Predict saved = predictRepository.save(predict);
	    response.setId(saved.getId());

	    return response;
	}
	
	
	
//	@PostMapping("/videos/create")
//	public String create(VideoDto dto) {
//		Video video = dto.toEntity();
//		Video created = videoRepository.save(video);
//		
//		return "redirect:/video/"+video.getId();
//	}
	
	
	@GetMapping("/video/{id}") 
	public String result(Model model,@PathVariable("id") Long id) {
//		Predict predict = predictService.result(id);
		Predict predict = predictRepository.findById(id).orElse(null);  
			
		Double motionAverage = predictRepository.findMotionAverage();
	    Double saturationAverage = predictRepository.findSaturationAverage();
	    Double audioAverage = predictRepository.findAudioAverage();
	    Double emotionAverage = predictRepository.findEmotionAverage();
	    Double dopamineAverage = predictRepository.findDopamineIndexAverage();
	    Double airecallAverage = predictRepository.findAiRecallAverage();

	    PredictResultDto dto = new PredictResultDto(predict.getId());

        dto.addMetric("motion", predict.getMotion(), motionAverage);
	    dto.addMetric("saturation", predict.getSaturation(), saturationAverage);
        dto.addMetric("audio", predict.getAudio(), audioAverage);
        dto.addMetric("emotion", predict.getEmotion(), emotionAverage);
        dto.addMetric("Ai Recall", predict.getAi_recall(), airecallAverage);
        dto.addMetric("Dopamine Index", predict.getDopamine_index(), dopamineAverage);


	    model.addAttribute("dto",dto);
		return "video/result";
	}
	
	@GetMapping("/api/videos/{id}")
	@ResponseBody
	public Predict resultApi(@PathVariable("id") Long id) {
	    return predictRepository.findById(id).orElse(null);
	}
	
	
	@GetMapping("/videos/list")
	public String create(Model model) {        
		Predict predict1 = predictRepository.findById(1L).orElse(null);
		Predict predict2 = predictRepository.findById(2L).orElse(null);
		
	    Double motionAverage = predictRepository.findMotionAverage();
	    Double saturationAverage = predictRepository.findSaturationAverage();
	    Double audioAverage = predictRepository.findAudioAverage();
	    Double emotionAverage = predictRepository.findEmotionAverage();
	    Double airecallAverage = predictRepository.findAiRecallAverage();
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
		        dto.addMetric("Ai Recall", predict.getAi_recall(), airecallAverage);
		        dto.addMetric("Dopamine Index", predict.getDopamine_index(), dopamineAverage);

		        resultList.add(dto);
		    }

		    model.addAttribute("predictList", resultList);
		
		return "video/list";
	}
	
	@GetMapping("/api/videos")
	@ResponseBody
	public List<Predict> listApi() {
	    return predictRepository.findAll();
	}
	
}