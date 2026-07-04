package com.shu.backend.domain.user.support;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserStatus;

import java.util.List;

public final class UserDisplay {

    public static final String DELETED_USER_NAME = "탈퇴한 사용자";
    private static final List<String> TEENPLER_ADJECTIVES = List.of(
            "다정한", "활기찬", "차분한", "솔직한", "유쾌한",
            "섬세한", "성실한", "따뜻한", "명랑한", "느긋한",
            "꼼꼼한", "용감한", "상냥한", "재치있는", "든든한",
            "반짝이는", "신중한", "긍정적인", "친절한", "담백한",
            "부지런한", "사려깊은", "쾌활한", "침착한", "집중하는",
            "배려하는", "자유로운", "영리한", "싱그러운", "믿음직한",
            "부드러운", "산뜻한", "진지한", "밝은", "고요한",
            "튼튼한", "즐거운", "멋진", "꾸준한", "예리한",
            "정직한", "슬기로운", "희망찬", "깔끔한", "의젓한",
            "창의적인", "건강한", "당당한", "넉넉한", "온화한",
            "순수한", "해맑은", "기분좋은", "품격있는", "멋스러운",
            "사근사근한", "적극적인", "반가운", "고마운", "의욕적인",
            "센스있는", "알찬", "빛나는", "조화로운", "평온한",
            "단단한", "마음넓은", "깊이있는", "깨끗한", "새로운",
            "근사한", "훌륭한", "귀여운", "세련된", "자랑스러운",
            "기특한", "반듯한", "정겨운", "탁월한", "향기로운"
    );

    private UserDisplay() {
    }

    public static boolean isDeleted(User user) {
        return user == null || user.getStatus() == UserStatus.DELETED;
    }

    public static String nicknameOrDeleted(User user) {
        return isDeleted(user) ? DELETED_USER_NAME : user.getNickname();
    }

    public static String usernameOrDeleted(User user) {
        return isDeleted(user) ? DELETED_USER_NAME : user.getUsername();
    }

    public static String anonymousOrDeleted(User user, String anonymousName) {
        return isDeleted(user) ? DELETED_USER_NAME : anonymousName;
    }

    public static String teenplerAlias(User user) {
        return isDeleted(user) ? DELETED_USER_NAME : teenplerAlias(user.getId());
    }

    public static String teenplerAlias(Long seed) {
        long value = seed == null ? 0L : seed;
        int adjectiveIndex = Math.floorMod((value * 31L) + 17L, TEENPLER_ADJECTIVES.size());
        int aliasNumber = Math.floorMod((value * 73L) + 41L, 1000);
        return TEENPLER_ADJECTIVES.get(adjectiveIndex) + " 틴플러#" + String.format("%03d", aliasNumber);
    }
}
