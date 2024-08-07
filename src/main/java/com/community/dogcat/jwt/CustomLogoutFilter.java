package com.community.dogcat.jwt;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.GenericFilterBean;
import com.community.dogcat.repository.user.RefreshRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomLogoutFilter extends GenericFilterBean {

	private final JWTUtil jwtUtil;
	private final RefreshRepository refreshRepository;

	public CustomLogoutFilter(JWTUtil jwtUtil, RefreshRepository refreshRepository) {

		this.jwtUtil = jwtUtil;
		this.refreshRepository = refreshRepository;

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
	}

	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

		String requestUri = request.getRequestURI();

		if (!requestUri.matches("^/logout$")) {
;
			filterChain.doFilter(request, response);
			return;

		}

		String requestMethod = request.getMethod();

		if (!requestMethod.equals("POST")) {

			log.warn("This is not a POST method");
			filterChain.doFilter(request, response);
			return;

		}

		String refresh = null;
		Cookie[] cookies = request.getCookies();

		for (Cookie cookie : cookies) {

			if (cookie.getName().equals("refresh")) {
				refresh = cookie.getValue();
			}

		}

		if (refresh == null) {

			log.warn("No refresh token found, proceeding with logout");
			clearCookies(response);
			redirectToHome(response, request);
			return;

		}

		try {

			jwtUtil.isExpired(refresh);

		} catch (ExpiredJwtException e) {

			log.warn("Expired refresh token: {}", refresh);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;

		}

		String category = jwtUtil.getCategory(refresh);

		if (!category.equals("refresh")) {

			log.warn("Invalid token category: {}", refresh);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;

		}

		Boolean isExist = refreshRepository.existsByRefresh(refresh);

		if (!isExist) {

			log.warn("Refresh token not found in DB: {}", refresh);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;

		}

		refreshRepository.deleteByRefresh(refresh);
		clearCookies(response);
		response.setStatus(HttpServletResponse.SC_OK);
		redirectToHome(response, request);
	}

	private void clearCookies(HttpServletResponse response) {

		Cookie refreshCookie = new Cookie("refresh", null);
		refreshCookie.setMaxAge(0);
		refreshCookie.setPath("/");

		Cookie accessCookie = new Cookie("access", null);
		accessCookie.setMaxAge(0);
		accessCookie.setPath("/");

		Cookie sessionCookie = new Cookie("JSESSIONID", null);
		sessionCookie.setMaxAge(0);
		sessionCookie.setPath("/");

		response.addCookie(refreshCookie);
		response.addCookie(accessCookie);
		response.addCookie(sessionCookie);

	}

	private void redirectToHome(HttpServletResponse response, HttpServletRequest request) {

		try {
			response.sendRedirect(request.getContextPath() + "/");
		} catch (IOException e) {
			log.error("IOException occurred while redirecting", e);
		}

	}
}
