package com.community.dogcat.util.uploader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import com.community.dogcat.domain.ImgBoard;
import com.community.dogcat.domain.Post;
import com.community.dogcat.dto.uploadImage.UploadPostImageResultDTO;
import com.community.dogcat.repository.upload.UploadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@Transactional
@RequiredArgsConstructor
public class S3Uploader {

	private final AmazonS3 amazonS3;

	private final S3LocalUploader s3LocalUploader;

	private final UploadRepository uploadRepository;

	@Value("${cloud.aws.s3.bucket}")
	public String bucket; // S3 버킷 이름

	@Value("${s3UploadPath}")
	private String s3UploadPath;

	//S3로 파일 업로드 + DB 저장
	public List<String> upload(MultipartFile multipartFile, Post postNo, String uuid) throws
		RuntimeException,
		IOException {

		// 임시 저장 폴더가 없을경우 생성
		File directory = new File(s3UploadPath);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		String fileName = multipartFile.getOriginalFilename();
		String extension = fileName.substring(fileName.lastIndexOf("."));

		// 파일 여러개 업로드시 List로 받지 않아도 여러개의 uuid값이 한꺼번에 들어옴, 고쳐야함
		log.info("Uuid: {}", uuid);
		//S3에 업로드될 이름 저장
		String saveFileName = uuid + extension;

		log.info("------------ s3Upload fileName: {}", fileName);
		log.info("------------ saveFileName: {}", saveFileName);

		// S3업로드 전 LocalUpload
		UploadPostImageResultDTO results = s3LocalUploader.uploadLocal(multipartFile, extension, uuid);
		log.info("getThumbnailPath: {}", results.getThumbnailPath());

		// 썸네일 존재할시 multipartFile 로 변환 후 s3업로드
		MultipartFile thumbnailMultipartFile = results.getThumbnailPath() != null ?
			s3LocalUploader.convertFileToMultipartFile(new File(results.getThumbnailPath())) : null;

		// 썸네일 지정 후 업로드
		String thumbnailPath = "";
		if (thumbnailMultipartFile != null) {
			thumbnailPath = putS3(thumbnailMultipartFile, "t_" + saveFileName);
		}

		ImgBoard result = ImgBoard.builder()
			.fileUuid(results.getUuid())
			.postNo(postNo)
			.fileName(fileName)
			.extension(extension)
			.uploadPath(putS3(multipartFile, saveFileName))
			.thumbnailPath(thumbnailPath)
			.uploadTime(results.getUploadTime())
			.img(results.isImg())
			.build();

		// LocalUploader 에서 만들어진 정보로 db 저장
		uploadRepository.save(result);

		List<String> uploadResult = new ArrayList<>();
		uploadResult.add(String.valueOf(postNo.getPostNo()));
		uploadResult.add(uuid);
		uploadResult.add(fileName);

		log.info("uploadResult: {}", uploadResult);

		// // summernote 임시 파일 삭제
		// deleteTempFiles.deleteTempFile(saveFileName);
		// 업로드 후 로컬에 저장된 원본 파일 삭제
		removeOriginalFile(multipartFile, saveFileName);

		return uploadResult;
	}

	//S3로 업로드후 업로드 된 URL 반환
	public String putS3(MultipartFile uploadFile, String saveFileName) throws IOException {

		// 파일 업로드시 만들어졌던 uuid + 확장자 -> 버킷 링크 반환
		InputStream uploadedFile = uploadFile.getInputStream();

		// 스트림 데이터에 대한 내용 길이 명시
		// -> 명시해주지 않으면 메모리에서 버퍼링 하여 메모리 부족 가능의 경고가 뜸
		// 명시해주면 해당 데이터만큼 네트워크에서 처리
		ObjectMetadata objMeta = new ObjectMetadata();

		// inputStream 를 바이트 배열로, 이 과정에서 메모리 사용, 큰 파일일때 주의?
		byte[] bytes = IOUtils.toByteArray(uploadedFile);

		// MetaData 를 업로드 데이터의 길이로 설정
		// -> AWS SDK 가 적절한 방법으로 버퍼링 및 전송 관리
		objMeta.setContentLength(bytes.length);

		// bytes 를 업로드에 필요한 inputStream 으로 변환
		ByteArrayInputStream byteArrayIs = new ByteArrayInputStream(bytes);

		amazonS3.putObject(new PutObjectRequest(bucket, saveFileName, byteArrayIs, objMeta).withCannedAcl(
			CannedAccessControlList.PublicRead));
		uploadedFile.close();

		return amazonS3.getUrl(bucket, saveFileName).toString();
	}

	//S3 업로드 후 원본 파일 삭제
	private void removeOriginalFile(MultipartFile file, String saveFileName) {

		try {
			String fileName = file.getOriginalFilename();
			Path filePath = Paths.get(s3UploadPath, saveFileName);
			Path thumbfilePath = Paths.get(s3UploadPath, "t_" + saveFileName);

			Files.deleteIfExists(filePath);
			Files.deleteIfExists(thumbfilePath);

			if (!Files.exists(filePath) && !Files.exists(thumbfilePath)) {
				log.info("업로드 원본 파일 삭제 성공 파일 이름: {}", fileName);
			} else {
				log.info("업로드 원본 파일 삭제 실패, 파일 이름: {}", fileName);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void removeS3File(String fileName, String thumbFileName) {
		final DeleteObjectRequest imageFile = new DeleteObjectRequest(bucket, fileName);
		final DeleteObjectRequest thumbFile = new DeleteObjectRequest(bucket, thumbFileName);
		amazonS3.deleteObject(imageFile);
		amazonS3.deleteObject(thumbFile);
	}
}