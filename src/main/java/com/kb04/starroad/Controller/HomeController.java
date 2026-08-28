package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.policy.PolicyResponseDto;
import com.kb04.starroad.Service.PolicyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;

@Tag(name = "홈 API")
@RequiredArgsConstructor
@RestController
public class HomeController {

    private final PolicyService policyService;

    @Operation(summary = "home", description = "홈")
    @GetMapping("/starroad")
    public ModelAndView home(HttpSession session) {

        ModelAndView mav;
        if (session.getAttribute("currentUser") == null) {
            session.removeAttribute("modal");
            mav = new ModelAndView("home");
        } else {
            mav = new ModelAndView("loginHome");
            MemberDto dto = (MemberDto) session.getAttribute("currentUser");

            PolicyResponseDto result = policyService.modalPolicy(dto);
            if (result == null){
                mav.addObject("message", "관심정책을 등록하고 알림을 받아보세요🤗");
            } else {
                mav.addObject("message", "Y");
                mav.addObject("currentUser", dto.getName());
                mav.addObject("policy", result);
            }
        }
        return mav;
    }

}
