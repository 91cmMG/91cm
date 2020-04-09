package com.nineone.nocm.controller.api;

import com.nineone.nocm.annotation.Socialuser;
import com.nineone.nocm.domain.ApiResponse;
import com.nineone.nocm.domain.Invite;
import com.nineone.nocm.domain.JoinInfo;
import com.nineone.nocm.domain.User;
import com.nineone.nocm.domain.enums.InviteState;
import com.nineone.nocm.service.ChannelService;
import com.nineone.nocm.service.InviteService;
import com.nineone.nocm.service.JoinInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invite")
public class InviteApiController {

    @Autowired
    private InviteService inviteService;
    @Autowired
    private JoinInfoService joinInfoService;
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private ChannelService channelService;

    // map에 필요한 정보 생성자 정보 (권한 확인용)
    // 채널 정보, 초대자 정보(유저 id)
    @PostMapping
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> inviteUser(@RequestBody Invite invite, Exception e) {
        try {
            if (joinInfoService.isExistUser(invite)) {
                return new ResponseEntity<>(ApiResponse.builder().error("405")
                        .message("해당 유저는 이미 채널에 가입되어 있습니다.")
                        .build(), HttpStatus.METHOD_NOT_ALLOWED);
            }
            if (joinInfoService.AuthorityCheck(invite)) {
                inviteService.saveInvite(invite);
                log.info(invite.getRecipient());
                messagingTemplate.convertAndSend("/sub/alarm/" + invite.getRecipient(), invite);
                return new ResponseEntity<>("{}", HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ApiResponse.builder().error("403")
                        .message("채널에 대한 권한이 없습니다.").build(), HttpStatus.FORBIDDEN);
            }
        } catch (Exception ex) {
            if (ex.getClass() == DataIntegrityViolationException.class) {
                return new ResponseEntity<>(ApiResponse.builder()
                        .error("500")
                        .message("해당 사용자가 없거나 입력이 잘못되었습니다.").build(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(ApiResponse.builder()
                    .error(ex.getClass().toString())
                    .message(ex.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptUser(@RequestBody Invite invite) throws RuntimeException {
        log.info("invite id :"+invite.getId()+"");
        if (joinInfoService.isExistUser(invite)){
            invite.setInvite_state(InviteState.ACCEPT);
            inviteService.updateInvite(invite);
            return new ResponseEntity<>("{}",HttpStatus.OK);
        }
        joinInfoService.insertJoinInfo(JoinInfo.builder()
                .channel_id(invite.getChannel_id())
                .member_email(invite.getRecipient())
                .build());
        invite.setInvite_state(InviteState.ACCEPT);
        log.info("invite accept");
        inviteService.updateInvite(invite);
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }
    @PostMapping("/refuse")
    public ResponseEntity<?> refuseUser(@RequestBody Invite invite) throws RuntimeException{
        // 거절 내용을 채널에 보내는 로직을 구현해야함
        log.info("refuse");
        invite.setInvite_state(InviteState.REFUSE);
        inviteService.updateInvite(invite);
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }
    @GetMapping("/list")
    public List<Invite> getInviteList(@Socialuser User user) throws RuntimeException{
        return inviteService.getInviteList(user.getEmail());
    }
}

