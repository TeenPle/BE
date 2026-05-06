package com.shu.backend.domain.poll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PollResponse {
    private Long pollId;
    private int totalParticipants;
    @JsonProperty("hasVoted")
    private boolean hasVoted;
    private Long selectedOptionId;
    private List<PollOptionResponse> options;
}
