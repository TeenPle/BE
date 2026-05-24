package com.shu.backend.domain.board.enums;

import lombok.Getter;

@Getter
public enum BoardType {
    FREE("자유게시판", "자유롭게 이야기해요", 10),
    QUESTION("질문게시판", "궁금한 것을 물어봐요", 20),
    GRADE_1("1학년 게시판", "1학년 학생들끼리 이야기해요", 30),
    GRADE_2("2학년 게시판", "2학년 학생들끼리 이야기해요", 40),
    GRADE_3("3학년 게시판", "3학년 학생들끼리 이야기해요", 50),
    ACADEMIC("시험·수행", "시험과 수행평가 정보를 나눠요", 60),
    CONCERN("고민게시판", "말하기 어려운 고민을 나눠요", 70),
    PROMOTION("홍보게시판", "동아리와 모집 글을 올려요", 80),
    GRADUATE("졸업생 게시판", "졸업생과 이야기해요", 90);

    private final String title;
    private final String description;
    private final int sortOrder;

    BoardType(String title, String description, int sortOrder) {
        this.title = title;
        this.description = description;
        this.sortOrder = sortOrder;
    }
}
