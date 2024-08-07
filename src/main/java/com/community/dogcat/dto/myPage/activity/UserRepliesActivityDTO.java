package com.community.dogcat.dto.myPage.activity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRepliesActivityDTO {

	private Long replyNo;

	private String userId;

	private String nickname;

	private Long postNo;

	private String boardCode;

	private String replyContent;

	private Date regDate;
}