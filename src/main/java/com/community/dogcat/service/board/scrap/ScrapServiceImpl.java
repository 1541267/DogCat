package com.community.dogcat.service.board.scrap;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.community.dogcat.domain.Post;
import com.community.dogcat.domain.Scrap;
import com.community.dogcat.domain.User;
import com.community.dogcat.dto.board.scrap.ScrapDTO;
import com.community.dogcat.repository.board.BoardRepository;
import com.community.dogcat.repository.board.scrap.ScrapRepository;
import com.community.dogcat.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class ScrapServiceImpl implements ScrapService {

	private final UserRepository userRepository;
	private final BoardRepository boardRepository;
	private final ScrapRepository scrapRepository;

	@Override
	public Long register(ScrapDTO scrapDTO) {
		// 게시물 번호 조회
		Post post = boardRepository.findById(scrapDTO.getPostNo()).orElseThrow();
		// 로그인한 회원정보를 받아 userId 조회
		String userId = scrapDTO.getUserId();
		User user = userRepository.findById(userId).orElseThrow();
		// 로그인한 회원의 스크랩 여부 확인
		Optional<Scrap> scrapState = scrapRepository.findByPostNoAndUserId(post, user);
		// 스크랩 정보가 없는 경우 스크랩 정보 추가
		if (scrapState.isEmpty()) {
			Scrap scrap = Scrap.builder()
				.postNo(post)
				.userId(user)
				.build();
			scrapRepository.save(scrap);
			return scrap.getScrapNo();
		} else {
			return null;
		}
	}

	@Override
	public void delete(Long scrapNo, String userId) {
		// 로그인한 회원정보를 받아 userId 조회
		User user = userRepository.findById(userId).orElseThrow();
		// 게시물 번호 조회
		Post post = scrapRepository.findById(scrapNo).orElseThrow().getPostNo();
		// 로그인한 회원의 스크랩 여부 확인
		Optional<Scrap> scrap = scrapRepository.findByPostNoAndUserId(post, user);
		// 스크랩 정보가 있는 경우 스크랩 삭제
		if (scrap.isPresent()) {
			scrapRepository.deleteById(scrapNo);
		}

	}

	// @Override
	// public ScrapDTO readOne(Long scrapNo) {
	// 	Scrap scrap = scrapRepository.findById(scrapNo).orElseThrow();
	// 	ScrapDTO scrapDTO = new ScrapDTO(scrap);
	// 	return scrapDTO;
	// }
	//
	// @Override
	// public List<ScrapDTO> list(Long scrapNo) {
	// 	Optional<Scrap> scraps = scrapRepository.findById(scrapNo);
	// 	return scraps.stream()
	// 		.map(scrap -> new ScrapDTO(scrap))
	// 		.collect(Collectors.toList());
	// }
}