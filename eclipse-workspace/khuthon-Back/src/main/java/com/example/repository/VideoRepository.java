package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Video;

public interface VideoRepository extends JpaRepository<Video,Long> {

}
