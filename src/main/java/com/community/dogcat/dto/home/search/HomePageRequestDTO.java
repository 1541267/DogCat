package com.community.dogcat.dto.home.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomePageRequestDTO {

	@Builder.Default
	private int page = 1;

	@Builder.Default
	private int size = 10;

	// 검색 종류 t, c, u, tc
	private String type;

	// 검색어
	private String keyword;

	public String[] getTypes() {
		if (type == null || type.isEmpty()) {
			return null;
		}
		return type.split("");
	}

	public Pageable getPageable() {
		return PageRequest.of(this.page - 1, this.size);
	}

	// url 설정
	private String link;

	public String getLink() {

		if (link == null) {

			StringBuilder builder = new StringBuilder();

			builder.append("?page=" + this.page);

			if (type != null && type.length() > 0) {
				builder.append("?type=" + type);
			}

			if (keyword != null) {

				try {
					builder.append("?keyword=" + URLEncoder.encode(keyword, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					//예외 처리
				}
			}
			link = builder.toString();
		}
		return link;
	}

}
