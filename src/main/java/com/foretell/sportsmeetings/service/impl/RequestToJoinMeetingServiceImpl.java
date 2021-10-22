package com.foretell.sportsmeetings.service.impl;

import com.foretell.sportsmeetings.dto.req.RequestToJoinMeetingReqDto;
import com.foretell.sportsmeetings.dto.req.RequestToJoinMeetingStatusReqDto;
import com.foretell.sportsmeetings.dto.res.RequestToJoinMeetingResDto;
import com.foretell.sportsmeetings.exception.RequestToJoinMeetingException;
import com.foretell.sportsmeetings.exception.UserHaveNotPermissionException;
import com.foretell.sportsmeetings.exception.notfound.RequestToJoinMeetingNotFoundException;
import com.foretell.sportsmeetings.model.Meeting;
import com.foretell.sportsmeetings.model.RequestToJoinMeeting;
import com.foretell.sportsmeetings.model.RequestToJoinMeetingStatus;
import com.foretell.sportsmeetings.model.User;
import com.foretell.sportsmeetings.repo.RequestToJoinMeetingRepo;
import com.foretell.sportsmeetings.service.MeetingService;
import com.foretell.sportsmeetings.service.RequestToJoinMeetingService;
import com.foretell.sportsmeetings.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequestToJoinMeetingServiceImpl implements RequestToJoinMeetingService {
    private final RequestToJoinMeetingRepo requestToJoinMeetingRepo;
    private final UserService userService;
    private final MeetingService meetingService;

    public RequestToJoinMeetingServiceImpl(RequestToJoinMeetingRepo requestToJoinMeetingRepo, UserService userService, MeetingService meetingService) {
        this.requestToJoinMeetingRepo = requestToJoinMeetingRepo;
        this.userService = userService;
        this.meetingService = meetingService;
    }


    @Override
    public boolean create(RequestToJoinMeetingReqDto requestToJoinMeetingReqDto, String username) {
        User user = userService.findByUsername(username);
        Long userId = user.getId();
        Long meetingId = requestToJoinMeetingReqDto.getMeetingId();
        if (requestToJoinMeetingRepo.findByMeetingIdAndCreatorId(meetingId, userId).isEmpty()) {
            Meeting meeting = meetingService.findById(meetingId);

            if (meeting.getCreator().getId().equals(userId)) {
                throw new RequestToJoinMeetingException("You cannot create request to your meeting");
            }

            RequestToJoinMeeting requestToJoinMeeting = new RequestToJoinMeeting(
                    requestToJoinMeetingReqDto.getDescription(),
                    user,
                    meeting,
                    RequestToJoinMeetingStatus.CREATED
            );
            requestToJoinMeetingRepo.save(requestToJoinMeeting);
            return true;
        } else {
            throw new RequestToJoinMeetingException("You already created request to this meeting");
        }
    }

    @Override
    public List<RequestToJoinMeetingResDto> getByMeetingId(Long meetingId, String username) {
        User user = userService.findByUsername(username);
        Meeting meeting = meetingService.findById(meetingId);
        if (meeting.getCreator().getId().equals(user.getId())) {
            return requestToJoinMeetingRepo.findAllByMeetingId(meeting.getId())
                    .stream()
                    .filter(requestToJoinMeeting -> requestToJoinMeeting.getStatus() == RequestToJoinMeetingStatus.CREATED)
                    .map(this::convertRequestToJoinMeetingToRequestToJoinMeetingResDto)
                    .collect(Collectors.toList());
        } else {
            throw new UserHaveNotPermissionException("You can get requests only for your meeting");
        }
    }

    @Override
    public RequestToJoinMeetingResDto updateStatus(Long id,
                                                   RequestToJoinMeetingStatusReqDto requestToJoinMeetingStatusReqDto,
                                                   String meetingCreatorUsername) {
        User creator = userService.findByUsername(meetingCreatorUsername);
        RequestToJoinMeeting requestToJoinMeeting = findById(id);

        if (requestToJoinMeeting.getMeeting().getCreator().getId().equals(creator.getId())) {
            requestToJoinMeeting.setStatus(requestToJoinMeetingStatusReqDto.getRequestToJoinMeetingStatus());
            return convertRequestToJoinMeetingToRequestToJoinMeetingResDto(
                    requestToJoinMeetingRepo.save(requestToJoinMeeting));
        } else {
            throw new UserHaveNotPermissionException("You can update status of request only if you are creator of meeting");
        }
    }

    @Override
    public RequestToJoinMeeting findById(Long id) {
        return requestToJoinMeetingRepo.findById(id).orElseThrow(() -> new RequestToJoinMeetingNotFoundException(
                "Request with id " + (id) + " not found"));
    }

    private RequestToJoinMeetingResDto convertRequestToJoinMeetingToRequestToJoinMeetingResDto(
            RequestToJoinMeeting requestToJoinMeeting) {
        return new RequestToJoinMeetingResDto(
                requestToJoinMeeting.getId(),
                requestToJoinMeeting.getMeeting().getId(),
                requestToJoinMeeting.getCreator().getId(),
                requestToJoinMeeting.getDescription());
    }

}
