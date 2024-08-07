package com.community.dogcat.service.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.community.dogcat.domain.User;
import com.community.dogcat.dto.user.CustomUserDetails;
import com.community.dogcat.jwt.JWTUtil;
import com.community.dogcat.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final JWTUtil jwtUtil;
	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUserId(username);

		if (user == null) {

			log.warn("User not found");
			throw new UsernameNotFoundException("User not found");

		}

		return new CustomUserDetails(user, jwtUtil);

	}

}
