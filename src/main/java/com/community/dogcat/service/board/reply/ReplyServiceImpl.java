package com.community.dogcat.service.board.reply;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import com.community.dogcat.repository.report.ReportLogRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.community.dogcat.domain.Post;
import com.community.dogcat.domain.Reply;
import com.community.dogcat.domain.User;
import com.community.dogcat.dto.board.reply.ReplyDTO;
import com.community.dogcat.repository.board.BoardRepository;
import com.community.dogcat.repository.board.reply.ReplyRepository;
import com.community.dogcat.repository.user.UserRepository;
import com.community.dogcat.repository.user.UsersAuthRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReplyServiceImpl implements ReplyService {

	private final UserRepository userRepository;

	private final UsersAuthRepository usersAuthRepository;

	private final BoardRepository boardRepository;

	private final ReplyRepository replyRepository;

	private final ReportLogRepository reportLogRepository;

	private final ModelMapper modelMapper;

	@Override
	public Long register(ReplyDTO replyDTO) {

		// 로그인한 회원정보를 받아 userId 조회
		String userId = replyDTO.getUserId();
		User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("Reply Service Register Error : 401 Unauthorized"));
		// 조회한 회원정보 DTO에 추가
		replyDTO.setNickname(user.getNickname());

		// 댓글 작성을 위해 게시물 번호 조회
		Post post = boardRepository.findById(replyDTO.getPostNo()).orElseThrow(() -> new NoSuchElementException("Reply Service Register Error : 404 Not Found"));

		// 게시물이 비밀글인 경우
		boolean secret = post.isSecret();
		// 게시물 댓글 권한이 수의사만 가능한 경우
		boolean replyAuth = post.isReplyAuth();
		// 로그인한 유저가 관리자거나, 수의사인 경우
		String auth = usersAuthRepository.findByUserId(userId).getAuthorities();
		// 로그인한 유저가 게시글 작성자일때
		Optional<Post> postUser = boardRepository.findByPostNoAndUserId(post.getPostNo(), user);

		if (secret || replyAuth) {

			if (postUser.isPresent() || auth.equals("ROLE_ADMIN") ||  auth.equals("ROLE_VET")) {

				// 댓글 작성
				Reply reply = Reply.builder()
					.replyContent(replyDTO.getReplyContent())
					.regDate(replyDTO.getRegDate())
					.userId(user)
					.postNo(post)
					.build();

				replyRepository.save(reply);

			} else {
				log.error("Reply Service Register Error : 403 Forbidden");
				return null;
			}

		} else {

		// 비밀글이 아닐때 누구나 댓글 작성 가능
		Reply reply = Reply.builder()
			.replyContent(replyDTO.getReplyContent())
			.regDate(replyDTO.getRegDate())
			.userId(user)
			.postNo(post)
			.build();

		replyRepository.save(reply);
		}

		return replyDTO.getReplyNo();
	}

	@Override
	public void delete(Long replyNo, String userId) {

		// 로그인한 회원 정보 확인
		User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("Reply Service Delete Error : 401 Unauthorized"));

		// 댓글 번호와 회원 아이디가 일치하는 댓글인지 확인
		Optional<Reply> reply = replyRepository.findByReplyNoAndUserId(replyNo, user);

		// 로그인한 회원이 관리자인지 확인
		String auth = usersAuthRepository.findByUserId(userId).getAuthorities();

		// 회원 아이디로 작성한 댓글이거나 관리자이면 삭제
		if (reply.isPresent() || auth.equals("ROLE_ADMIN")) {

			// 해당 댓글 신고 내역있으면 삭제
			List<Long> reportLogIds = reportLogRepository.findByReplyNo(replyNo);

			for (Long reportLogId : reportLogIds) {

				reportLogRepository.deleteReportLog(reportLogId);
			}

			replyRepository.deleteById(replyNo);

		} else {
			log.error("Reply Service Delete Error : 403 Forbidden");
		}
	}

	@Override
	public List<ReplyDTO> getListOfReply(Long postNo) {

		Post post = boardRepository.findById(postNo).orElseThrow(() -> new NoSuchElementException("Post not found"));

		List<Reply> replies = replyRepository.findByPostNo(post.getPostNo());

		return replies.stream()
			.map(reply -> new ReplyDTO(reply)).collect(Collectors.toList());
	}

	@Override
	public Reply findReplyByReplyNo(Long replyNo) {

		Optional<Reply> optionalReply = replyRepository.findById(replyNo);

		Reply reply = optionalReply.get();

		return reply;
	}

}
