package com.shu.backend.domain.report.service;

import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.penalty.repository.PenaltyRepository;
import com.shu.backend.domain.penalty.service.PenaltyService;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.report.dto.ReportDTO;
import com.shu.backend.domain.report.dto.ReportSummaryResponse;
import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportStatus;
import com.shu.backend.domain.report.enums.TargetType;
import com.shu.backend.domain.report.exception.ReportException;
import com.shu.backend.domain.report.exception.status.ReportErrorStatus;
import com.shu.backend.domain.report.repository.ReportRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PenaltyRepository penaltyRepository;
    private final PenaltyService penaltyService;
    private final BoardAccessPolicy boardAccessPolicy;

    @PreAuthorize("@penaltyChecker.notPenalized(#reporterId)")
    @Transactional
    public Long create(Long reporterId, ReportDTO.CreateRequest req) {

        //중복 신고 검사
        if(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, req.getTargetType(), req.getTargetId())){
            throw new ReportException(ReportErrorStatus.DUPLICATE_REPORT);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        validateReporterCanAccessTarget(reporterId, req.getTargetType(), req.getTargetId());

        User reportedUser = resolveReportedUser(req.getTargetType(), req.getTargetId());

        //신고자와 피신고자가 같을 경우 X
        if (reportedUser.getId().equals(reporterId)) {
            throw new ReportException(ReportErrorStatus.SELF_REPORT_FORBIDDEN);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .targetType(req.getTargetType())
                .targetId(req.getTargetId())
                .reportReason(req.getReportReason())
                .build();

        return reportRepository.save(report).getId();
    }

    @Transactional
    public Long approve(Long adminId, Long reportId, int penaltyDays){

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING){
            throw new ReportException(ReportErrorStatus.REPORT_NOT_PENDING);
        }

        User handler = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        report.resolve(handler);

        return penaltyService.create(reportId, penaltyDays);
    }


    @Transactional
    public Long reject(Long adminId, Long reportId){

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING){
            throw new ReportException(ReportErrorStatus.REPORT_NOT_PENDING);
        }

        User handler = userRepository.findById(adminId)
                        .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        report.reject(handler);
        return reportId;

    }

    public Page<Report> getReports(ReportStatus status, Pageable pageable){
        return reportRepository.findAllByStatus(status, pageable);
    }

    public ReportSummaryResponse.DetailResponse getReportDetail(Long reportId) {
        Report r = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        String targetContent = resolveTargetContent(r.getTargetType(), r.getTargetId());
        PostContext ctx = resolvePostContext(r.getTargetType(), r.getTargetId());

        return ReportSummaryResponse.DetailResponse.builder()
                .reportId(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterNickname(r.getReporter().getNickname())
                .reportedUserId(r.getReportedUser().getId())
                .reportedUserNickname(r.getReportedUser().getNickname())
                .targetType(r.getTargetType().name())
                .targetId(r.getTargetId())
                .targetContent(targetContent)
                .schoolName(ctx.schoolName())
                .boardTitle(ctx.boardTitle())
                .reportReason(r.getReportReason().name())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(ISO_FMT) : null)
                .processedAt(r.getProcessedAt() != null ? r.getProcessedAt().format(ISO_FMT) : null)
                .build();
    }

    private String resolveTargetContent(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(p -> "[제목] " + p.getTitle() + "\n" + p.getContent())
                    .orElse("(삭제된 게시글)");
            case COMMENT -> commentRepository.findById(targetId)
                    .map(Comment::getContent)
                    .orElse("(삭제된 댓글)");
            default -> "";
        };
    }

    private record PostContext(String schoolName, String boardTitle) {}

    private PostContext resolvePostContext(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(p -> new PostContext(
                            p.getBoard().getSchool() != null ? p.getBoard().getSchool().getName() : null,
                            p.getBoard().getTitle()
                    ))
                    .orElse(new PostContext(null, null));
            case COMMENT -> commentRepository.findById(targetId)
                    .map(c -> new PostContext(
                            c.getPost().getBoard().getSchool() != null ? c.getPost().getBoard().getSchool().getName() : null,
                            c.getPost().getBoard().getTitle()
                    ))
                    .orElse(new PostContext(null, null));
            default -> new PostContext(null, null);
        };
    }

    //타입을 보고 타겟 객체와 id 조회
    private User resolveReportedUser(TargetType targetType, Long targetId) {

        return switch (targetType){
            case POST -> postRepository.findById(targetId)
                    .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND))
                    .getUser();

            case COMMENT -> commentRepository.findById(targetId)
                    .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND))
                    .getUser();

            case USER -> userRepository.findById(targetId)
                    .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

            default -> throw new ReportException(ReportErrorStatus.UNSUPPORTED_TARGET_TYPE);
        };
    }

    private void validateReporterCanAccessTarget(Long reporterId, TargetType targetType, Long targetId) {
        switch (targetType) {
            case POST -> {
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
                boardAccessPolicy.assertCanAccessPost(reporterId, post);
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));
                boardAccessPolicy.assertCanAccessComment(reporterId, comment);
            }
            case USER -> boardAccessPolicy.requireActiveUserWithSchool(reporterId);
            default -> throw new ReportException(ReportErrorStatus.UNSUPPORTED_TARGET_TYPE);
        }
    }
}
