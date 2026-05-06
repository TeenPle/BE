package com.shu.backend.domain.poll.service;

import com.shu.backend.domain.poll.dto.PollOptionResponse;
import com.shu.backend.domain.poll.dto.PollResponse;
import com.shu.backend.domain.poll.entity.Poll;
import com.shu.backend.domain.poll.entity.PollOption;
import com.shu.backend.domain.poll.entity.PollVote;
import com.shu.backend.domain.poll.exception.PollException;
import com.shu.backend.domain.poll.exception.status.PollErrorStatus;
import com.shu.backend.domain.poll.repository.PollOptionRepository;
import com.shu.backend.domain.poll.repository.PollRepository;
import com.shu.backend.domain.poll.repository.PollVoteRepository;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollService {

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 5;
    private static final int MAX_OPTION_LENGTH = 100;

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createPollIfPresent(Post post, List<String> rawOptions) {
        if (rawOptions == null || rawOptions.isEmpty()) {
            return;
        }

        List<String> options = normalizeOptions(rawOptions);
        Poll poll = pollRepository.save(Poll.builder().post(post).build());
        saveOptions(poll, options);
    }

    @Transactional
    public void syncPoll(Post post, List<String> rawOptions) {
        if (rawOptions == null) {
            return;
        }

        Poll existing = pollRepository.findByPostId(post.getId()).orElse(null);
        if (rawOptions.isEmpty()) {
            if (existing != null) {
                pollVoteRepository.deleteByPollId(existing.getId());
                pollOptionRepository.deleteByPollId(existing.getId());
                pollRepository.delete(existing);
            }
            return;
        }

        List<String> options = normalizeOptions(rawOptions);
        if (existing == null) {
            Poll poll = pollRepository.save(Poll.builder().post(post).build());
            saveOptions(poll, options);
            return;
        }

        pollVoteRepository.deleteByPollId(existing.getId());
        pollOptionRepository.deleteByPollId(existing.getId());
        saveOptions(existing, options);
    }

    public PollResponse getPollResponse(Long postId, Long currentUserId) {
        Poll poll = pollRepository.findByPostId(postId)
                .orElse(null);
        if (poll == null) {
            return null;
        }

        return toResponse(poll, currentUserId);
    }

    public boolean hasPoll(Long postId) {
        return pollRepository.existsByPostId(postId);
    }

    @Transactional
    public PollResponse vote(Long postId, Long userId, Long optionId) {
        Poll poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new PollException(PollErrorStatus.POLL_NOT_FOUND));

        if (!poll.getPost().getId().equals(postId)) {
            throw new PollException(PollErrorStatus.POLL_NOT_FOUND);
        }

        if (pollVoteRepository.existsByPollIdAndUserId(poll.getId(), userId)) {
            throw new PollException(PollErrorStatus.POLL_ALREADY_VOTED);
        }

        PollOption option = pollOptionRepository.findById(optionId)
                .orElseThrow(() -> new PollException(PollErrorStatus.POLL_OPTION_NOT_FOUND));
        if (!option.getPoll().getId().equals(poll.getId())) {
            throw new PollException(PollErrorStatus.POLL_OPTION_MISMATCH);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        option.incrementVoteCount();
        pollVoteRepository.save(PollVote.builder()
                .poll(poll)
                .option(option)
                .user(user)
                .build());

        return toResponse(poll, userId);
    }

    private PollResponse toResponse(Poll poll, Long currentUserId) {
        Long selectedOptionId = pollVoteRepository.findByPollIdAndUserId(poll.getId(), currentUserId)
                .map(v -> v.getOption().getId())
                .orElse(null);
        long totalVotes = pollVoteRepository.countByPollId(poll.getId());
        List<PollOptionResponse> options = pollOptionRepository.findByPollIdOrderByDisplayOrderAsc(poll.getId())
                .stream()
                .map(option -> PollOptionResponse.of(option, totalVotes, selectedOptionId))
                .toList();

        return PollResponse.builder()
                .pollId(poll.getId())
                .totalParticipants((int) totalVotes)
                .hasVoted(selectedOptionId != null)
                .selectedOptionId(selectedOptionId)
                .options(options)
                .build();
    }

    private List<String> normalizeOptions(List<String> rawOptions) {
        List<String> options = rawOptions.stream()
                .map(option -> option == null ? "" : option.trim())
                .filter(option -> !option.isEmpty())
                .toList();

        if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
            throw new PollException(PollErrorStatus.INVALID_POLL_OPTIONS);
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>(options);
        if (unique.size() != options.size()) {
            throw new PollException(PollErrorStatus.DUPLICATE_POLL_OPTIONS);
        }

        if (options.stream().anyMatch(option -> option.length() > MAX_OPTION_LENGTH)) {
            throw new PollException(PollErrorStatus.POLL_OPTION_TOO_LONG);
        }

        return options;
    }

    private void saveOptions(Poll poll, List<String> options) {
        for (int i = 0; i < options.size(); i++) {
            pollOptionRepository.save(PollOption.builder()
                    .poll(poll)
                    .text(HtmlUtils.htmlEscape(options.get(i)))
                    .displayOrder(i)
                    .build());
        }
    }
}
