-- Comment: 댓글 수 카운트 최적화
CREATE INDEX idx_comment_post_id
    ON comment (post_id);

-- 댓글 상태로 필터링
CREATE INDEX idx_comment_post_id_status
    ON comment (post_id, comment_status);

-- Post: 게시판별 게시글 목록 페이징 최적화
--CREATE INDEX idx_post_board_id_id
--    ON post (board_id, id);
