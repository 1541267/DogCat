package com.community.dogcat.service.user;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.community.dogcat.domain.User;
import com.community.dogcat.domain.UsersAuth;
import com.community.dogcat.dto.user.JoinDTO;
import com.community.dogcat.dto.user.UserDetailDTO;
import com.community.dogcat.repository.user.RefreshRepository;
import com.community.dogcat.repository.user.UserRepository;
import com.community.dogcat.repository.user.UsersAuthRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final UsersAuthRepository usersAuthRepository;
	private final RefreshRepository refreshRepository;
	private final JavaMailSender javaMailSenderImpl;

	public Boolean isNicknameExists(String nickname) {

		return userRepository.existsByNickname(nickname);
	}

	public Boolean isUserIdExists(String userId) {

		return userRepository.existsByUserId(userId);
	}

	public User findUserId(String userId) {

		return userRepository.findByUserId(userId);
	}

	@Transactional
	public void deleteUserById(HttpServletResponse response, String userId) {
		usersAuthRepository.deleteById(userId);
		userRepository.deleteById(userId);
		refreshRepository.deleteAllByUsername(userId);

		//Refresh 토큰 Cookie 값 0
		Cookie refreshCookie = new Cookie("refresh", null);
		refreshCookie.setMaxAge(0);
		refreshCookie.setPath("/");

		//access 토큰 Cookie 값 0
		Cookie accessCookie = new Cookie("access", null);
		accessCookie.setMaxAge(0);
		accessCookie.setPath("/");

		//JSESSIONID 토큰 Cookie 값 0
		Cookie userIdCookie = new Cookie("userId", null);
		userIdCookie.setMaxAge(0);
		userIdCookie.setPath("/");

		//JSESSIONID 토큰 Cookie 값 0
		Cookie sessionCookie = new Cookie("JSESSIONID", null);
		sessionCookie.setMaxAge(0);
		sessionCookie.setPath("/");

		response.addCookie(refreshCookie);
		response.addCookie(accessCookie);
		response.addCookie(userIdCookie);
		response.addCookie(sessionCookie);
	}

	public UserDetailDTO findByUserId(String userId) {

		User user = userRepository.findByUserId(userId);

		UserDetailDTO userDetailDTO = UserDetailDTO.builder()
			.userId(user.getUserId())
			.userName(user.getUserName())
			.nickname(user.getNickname())
			.tel(user.getTel())
			.social(user.isSocial())
			.regDate(user.getRegDate())
			.build();

		return userDetailDTO;
	}

	public void userModify(UserDetailDTO userDetailDTO) {

		User existingUser = userRepository.findById(userDetailDTO.getUserId())
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		User updatedUser = User.builder()
			.userId(existingUser.getUserId())
			.userName(existingUser.getUserName())
			.nickname(userDetailDTO.getNickname())
			.tel(userDetailDTO.getTel())
			.userPw(userDetailDTO.getPassword() != null ? bCryptPasswordEncoder.encode(userDetailDTO.getPassword()) :
				existingUser.getUserPw()) // 비밀번호가 null 이 아닌 경우에만 업데이트
			.exp(existingUser.getExp())
			.build();

		userRepository.save(updatedUser);

	}

	public String getNickname(String userName) {

		User user = userRepository.findByUserId(userName);
		if (user != null) {
			return user.getNickname();
		}
		return null;
	}

	public void joinProcess(JoinDTO joinDTO) {

		Boolean isExist = userRepository.existsByUserId(joinDTO.getUserId());

		if (isExist) {
			return;
		}

		User user = User.builder()
			.userId(joinDTO.getUserId())
			.userPw(bCryptPasswordEncoder.encode(joinDTO.getUserPw()))
			.userName(joinDTO.getUserName())
			.nickname(joinDTO.getNickname())
			.tel(joinDTO.getTel())
			.userVet(joinDTO.isUserVet())
			.build();

		userRepository.save(user);

		if (user.isUserVet()) {
			UsersAuth usersAuth = UsersAuth.builder()
				.userId(user.getUserId())
				.authorities("ROLE_VET")
				.build();
			usersAuthRepository.save(usersAuth);
			return;
		}
		UsersAuth usersAuth = UsersAuth.builder()
			.userId(user.getUserId())
			.build();
		usersAuthRepository.save(usersAuth);
	}

	public User findUserByNameAndTel(String name, String tel) {
		return userRepository.findByUserNameAndTel(name, tel).orElse(null);
	}

	public User findPw(String name, String userId) {

		User user = userRepository.findByUserNameAndUserId(name, userId).orElse(null);

		if (user != null) {

			String newPassword = createNewPassword();

			user.setLoginLock(false);
			userRepository.save(user);

			userRepository.updatePassword(name, userId, bCryptPasswordEncoder.encode(newPassword), Instant.now());
			sendNewPasswordByMail(userId, newPassword);

			return user;
		}
		return null;
	}

	public String getRole(String userId) {

		UsersAuth usersAuth = usersAuthRepository.findByUserId(userId);

		if (usersAuth != null) {

			return usersAuth.getAuthorities();

		}

		return null;
	}

	public boolean checkPassword(String userId, String inputPassword) {
		String storedPasswordHash = userRepository.findPasswordHashByUsername(userId);
		return bCryptPasswordEncoder.matches(inputPassword, storedPasswordHash);
	}

	private String createNewPassword() {

		char[] chars = new char[] {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'x', 'y', 'z'
		};

		StringBuffer stringBuffer = new StringBuffer();
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.setSeed(new Date().getTime());

		int index = 0;
		int length = chars.length;
		for (int i = 0; i < 8; i++) {
			index = secureRandom.nextInt(length);

			if (index % 2 == 0)
				stringBuffer.append(String.valueOf(chars[index]).toUpperCase());
			else
				stringBuffer.append(String.valueOf(chars[index]).toLowerCase());

		}

		return stringBuffer.toString();

	}

	private void sendNewPasswordByMail(String toMailAddr, String newPassword) {

		final MimeMessagePreparator mimeMessagePreparator = new MimeMessagePreparator() {

			@Override
			public void prepare(MimeMessage mimeMessage) throws Exception {

				final MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
				mimeMessageHelper.setTo(toMailAddr);    // 수신 가능한 개인 메일 주소
				mimeMessageHelper.setSubject("[말랑발자국] 새 비밀번호 안내입니다.");
				mimeMessageHelper.setText("새 비밀번호 : " + newPassword, true);

			}

		};
		javaMailSenderImpl.send(mimeMessagePreparator);

	}

	// 유저 레벨 반환
	public Map<String, Object> checkUserLevel(long userExp) {
		long userLevel = 1;
		long expForNextLevel = 750;

		userLevel = userExp / expForNextLevel;
		userLevel++;
		if (userLevel > 5) {
			userLevel = 5;

		}

		long currentLevelExp = userExp % expForNextLevel;
		long percentage = (long)((currentLevelExp / (double)expForNextLevel) * 100);

		Map<String, Object> result = new HashMap<>();
		result.put("level", userLevel);
		result.put("percentage", percentage);

		log.info("exp: {}, Level:{}, percentage: {}%", userExp, userLevel, percentage);

		return result;
	}

	public long findUserExpByUsername(String username) {
		User user = userRepository.findByUserId(username);
		return user != null ? user.getExp() : 0;
	}
}
