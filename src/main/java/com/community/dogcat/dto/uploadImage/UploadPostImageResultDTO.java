package com.community.dogcat.dto.uploadImage;

import java.time.Instant;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@Component
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPostImageResultDTO {

	private String uploadPath;

	private String uuid;

	private String fileName;

	private boolean img;

	private String extension;

	private Instant uploadTime;
	// S3에 업로드 하기 위한 섬네일 경로
	private String thumbnailPath;
}