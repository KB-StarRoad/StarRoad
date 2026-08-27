package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.auth.LoginRequestDto;
import com.kb04.starroad.Service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Tag(name = "로그인 API")
@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "로그인 폼 조회", description = "로그인 폼을 조회할 수 있다")
    @GetMapping("/starroad/login")
    public ModelAndView showLoginForm() {
        ModelAndView mav = new ModelAndView("member/login");
        return mav;
    }
    @Operation(summary = "로그인 기능", description = "로그인을 할 수 있다")
    @PostMapping("/starroad/login")
    public ModelAndView login(
            @Parameter(description = "게시글 수정하기 위한 정보") @RequestBody @ModelAttribute LoginRequestDto requestDto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        ModelAndView mav = new ModelAndView();

        if (requestDto.getId().trim().isEmpty()) {
            mav.setViewName("redirect:/starroad/login");
            redirectAttributes.addFlashAttribute("error", "아이디를 입력해주세요");
        } else if (requestDto.getPassword().trim().isEmpty()) {
            mav.setViewName("redirect:/starroad/login");
            redirectAttributes.addFlashAttribute("written_id", requestDto.getId());
            redirectAttributes.addFlashAttribute("error", "비밀번호를 입력해주세요");
        } else {
            MemberDto memberDto = authService.authenticate(requestDto);
            if (memberDto != null) {
                session.setAttribute("currentUser", memberDto);
                mav.setViewName("redirect:/starroad"); // 로그인 성공
            } else {
                mav.setViewName("redirect:/starroad/login"); // 로그인 실패
                redirectAttributes.addFlashAttribute("error", "아이디와 비밀번호가 일치하지 않습니다");
            }
        }
        return mav;
    }

    @Operation(summary = "로그아웃 기능", description = "로그아웃을 할 수 있다")
    @GetMapping("/starroad/logout")
    public ModelAndView logout(
            HttpSession session) {
        session.invalidate();  // 세션 정보를 모두 삭제
        ModelAndView mav = new ModelAndView("redirect:/starroad");
        return mav;
    }
}