package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.SubscriptionDto;
import com.kb04.starroad.Service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "마이페이지 API")
@RestController
@RequiredArgsConstructor
public class MypageController {

    private final MemberService memberService;

    private static MemberDto getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession();
        MemberDto loginMember = (MemberDto) session.getAttribute("currentUser");
        return loginMember;
    }

    @Operation(summary = "자산", description = "자신의 자산을 확인할 수 있습니다")
    @GetMapping(value= {"/starroad/mypage", "/starroad/mypage/asset"})
    public ModelAndView asset(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        ModelAndView mav = new ModelAndView();
        MemberDto memberDto = getLoginMember(request);
        if (memberDto != null) {    // 로그인된 상태일 때
            mav.setViewName("mypage/asset");
            mav.addObject("memberAssets", memberService.getAssets(memberDto.getNo()));
        } else {
            redirectAttributes.addFlashAttribute("error", "마이페이지는 로그인이 필요한 서비스입니다");
            mav.setViewName("redirect:/starroad/login");
        }
        return mav;
    }

    @Operation(summary = "나의 게시판", description = "나의 게시물을 확인할 수 있습니다")
    @GetMapping("/starroad/mypage/board")
    public ModelAndView board(
            HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("mypage/board");
        MemberDto memberDto = getLoginMember(request);

        mav.addObject("writings", memberService.getWritings(memberDto.getNo()));
        return mav;
    }
    @Operation(summary = "나의 댓글", description = "나의 댓글을 확인할 수 있습니다")
    @GetMapping("/starroad/mypage/comment")
    public ModelAndView comment(
            HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("mypage/comment");
        MemberDto memberDto = getLoginMember(request);

        mav.addObject("comments", memberService.getComments(memberDto.getNo()));
        return mav;
    }

    @Operation(summary = "나의 챌린지", description = "나의 챌린지를 확인할 수 있습니다")
    @GetMapping("/starroad/mypage/challenge")
    public ModelAndView challenge(
            HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("mypage/challenge");
        MemberDto memberDto = getLoginMember(request);

        List<SubscriptionDto> subscriptions = memberService.getSubscriptions(memberDto);
        mav.addObject("subscriptions", subscriptions);
        List<String> paymentLogs = new ArrayList<>();
        for (SubscriptionDto sub : subscriptions) {
            paymentLogs.add(memberService.getPayLog(sub.getNo(), sub.getPeriod(), sub.getReceived()));
        }
        mav.addObject("paymentLogs", paymentLogs);
        return mav;
    }

    @Operation(summary = "가입상품정보", description = "나의 상품정보를 확인할 수 있습니다")
    @PostMapping("/starroad/mypage/reward")
    public ModelAndView reward(
            @Parameter(description = "가입상품번호") @RequestParam("sub_no") int subNo,
            @Parameter(description = "상품이름") @RequestParam("name") String name,
            @Parameter(description = "상품기간") @RequestParam("period") int period) {
        ModelAndView mav = new ModelAndView("mypage/reward");
        mav.addObject("reward", memberService.getReward(period));
        mav.addObject("sub_no", subNo);
        mav.addObject("name", name);
        mav.addObject("period", period);
        return mav;
    }

    @Operation(summary = "포인트리 확인", description = "나의 포인트리 받을 수 있습니다")
    @PostMapping("/starroad/mypage/save-reward")
    public ModelAndView getReward(
            @Parameter(description = "상품번호") @RequestParam("sub_no") int subNo,
            @Parameter(description = "포인트리") @RequestParam("reward") int reward) {
        ModelAndView mav = new ModelAndView("redirect:/starroad/mypage/asset");
        memberService.saveReward(1, subNo, reward);
        return mav;
    }
    @Operation(summary = "나의정보 확인 폼", description = "나의 정보를 확인할 수 있습니다")
    @GetMapping("/starroad/mypage/info")
    public ModelAndView info() {
        ModelAndView mav = new ModelAndView("mypage/info");
        return mav;
    }

    //회원정보 수정하는 부분
    @Operation(summary = "나의정보 수정", description = "나의 정보를 수정 할 수 있습니다")
    @PostMapping("/starroad/mypage/info")
    public ModelAndView info(
            HttpServletRequest request,
            @Parameter(description = "회원 정보") @RequestBody @ModelAttribute MemberDto changeDto) {
        ModelAndView mav = new ModelAndView("redirect:/starroad");

        MemberDto memberDto = getLoginMember(request);

        mav.addObject("member", memberDto);

        memberService.memberUpdate(memberDto, changeDto);

        return mav;
    }

    @Operation(summary = "나의 비밀번호 폼", description = "나의 비밀번호 폼으로 들어갈 수 있습니다")
    @GetMapping("/starroad/mypage/password")
    public ModelAndView password() {
        ModelAndView mav = new ModelAndView("mypage/password");
        return mav;
    }

    @Operation(summary = "나의정보 비밀번호 수정", description = "나의 비밀번호를 수정 할 수 있습니다")
    @PostMapping("/starroad/mypage/password")
    public ModelAndView password(
            @Parameter(description = "비밀번호") @RequestParam("password") String password,
            HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("redirect:/starroad");

        MemberDto memberDto = getLoginMember(request);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encPass = encoder.encode(password);

        memberService.memberPasswordUpdate(memberDto,encPass);

        return mav;
    }

    @Operation(summary = "나의 비밀번호 확인", description = "나의 비밀번호를 확인 할 수 있습니다")
    @PostMapping("/api/starroad/mypage/check-password")
    public String checkPassword(
                                @Parameter(description = "비밀번호") @RequestParam("inputPw") String inputPw,
                                HttpServletRequest request) {
        String msg = "";
        MemberDto memberDto = getLoginMember(request);


        if (!memberService.checkPassword(memberDto.getNo(), inputPw)) {
            msg = "비밀번호를 잘못 입력했습니다. 다시 입력해주세요.";
        }
        return msg;
    }
}