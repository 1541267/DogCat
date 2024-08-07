package com.community.dogcat.controller.board.scrap;

import static org.springframework.http.HttpStatus.*;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.community.dogcat.controller.BaseController;
import com.community.dogcat.domain.PostLike;
import com.community.dogcat.domain.Scrap;
import com.community.dogcat.dto.board.scrap.ScrapDTO;
import com.community.dogcat.jwt.JWTUtil;
import com.community.dogcat.service.board.scrap.ScrapService;
import com.community.dogcat.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/scrap")
public class ScrapController extends BaseController {

	private final ScrapService scrapService;

	@Autowired
	public ScrapController(JWTUtil jwtUtil, UserService userService, ScrapService scrapService) {
		super(jwtUtil, userService);
		this.scrapService = scrapService;
	}

	@Operation(summary = "Scrap Register", description = "게시물 보관 등록")
	@PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Long>> register(@Valid @RequestBody ScrapDTO scrapDTO, Model model) {

		// 모델에서 사용자 정보를 가져옴
		String userId = (String)model.getAttribute("username");

		// 로그인 사용자 확인
		if (userId == null) {
			log.error("ScrapController register Error : 401 Unauthorized");
			return ResponseEntity.status(UNAUTHORIZED).build(); // 로그인되지 않은 경우 401 오류
		}

		Map<String, Long> response = new HashMap<>();

		Long scrapNo = scrapService.register(scrapDTO);

		response.put("scrapNo", scrapNo);

		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Scrap Delete", description = "게시물 보관 삭제")
	@DeleteMapping("/{scrapNo}")
	public ResponseEntity<Map<String, Long>> delete(@PathVariable("scrapNo") Long scrapNo, Model model) {

		// 모델에서 사용자 정보를 가져옴
		String userId = (String)model.getAttribute("username");

		// 로그인 사용자 확인
		if (userId == null) {
			log.error("ScrapController Delete Error : 401 Unauthorized");
			return ResponseEntity.status(UNAUTHORIZED).build(); // 로그인되지 않은 경우 401 오류
		}

		// 보관 정보 조회
		if (scrapNo == null) {
			log.error("ScrapController Delete Error : 404 Not Found");
			return ResponseEntity.status(NOT_FOUND).build(); // 좋아요가 존재하지 않는 경우 404 오류
		}

		scrapService.delete(scrapNo, userId);

		Map<String, Long> response = new HashMap<>();

		response.put("scrapNo", scrapNo);

		return ResponseEntity.ok(response);
	}
}
