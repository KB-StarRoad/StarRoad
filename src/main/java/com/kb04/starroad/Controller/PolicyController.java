package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.policy.PolicyRequestDto;
import com.kb04.starroad.Dto.policy.PolicyResponseDto;
import com.kb04.starroad.Service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@Tag(name = "청년정책 API")
@RequiredArgsConstructor
@RestController
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "청년정책 찜", description = "청년정책을 관심 정책으로 등록할 수 있다")
    @PostMapping("/starroad/policy")
    public ModelAndView likePolicy(@Parameter(description = "정책 번호", example = "1") @RequestParam("policyNo")int policyNo,
                                   HttpSession session, HttpServletRequest request){

        ModelAndView mav = new ModelAndView();
        MemberDto memberDto = (MemberDto) session.getAttribute("currentUser");
        String url = request.getHeader("referer").substring(21);

        if (!policyService.hasLiked(memberDto, policyNo)){  // 관심정책에서 삭제
            policyService.deletePolicyHeart(memberDto, policyNo);
        } else{  // 관심정책으로 등록
            policyService.addPolicyHeart(memberDto, policyNo);
        }
        mav.setViewName("redirect:" + url);
        return mav;
    }

    @Operation(summary = "청년정책 조회", description = "청년정책을 조회할 수 있다")
    @GetMapping("/starroad/policy")
    public ModelAndView policy(Model model,
                               @Parameter(description = "페이지 번호", example = "5") @RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
                               HttpSession session) {

        List<PolicyResponseDto> result = policyService.selectAllPolicies();
        if(session.getAttribute("currentUser") != null){
            result = policyService.mappingPolicyHeart(result, (MemberDto) session.getAttribute("currentUser"));
        }
        Map<String, Object> finalResult = policyService.returnPoliciesByPage(result, pageIndex);

        model.addAttribute("policyList", finalResult.get("policyList"));
        model.addAttribute("pageEndIndex", finalResult.get("pageEndIndex"));
        model.addAttribute("currentPage", pageIndex);

        ModelAndView mav = new ModelAndView("policy/policy");

        return mav;
    }

    @Operation(summary = "청년정책 검색", description = "청년정책을 조건을 이용하여 검색할 수 있다")
    @GetMapping("/starroad/policy/result")
    public ModelAndView getPolicyByForm(Model model,
                                        @Parameter(description = "페이지 번호", example = "5") @RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
                                        @Parameter(required = false, description = "지역") @RequestParam(required = false) String location,
                                        @Parameter(required = false, description = "정책명 키워드") @RequestParam(required = false) String keyword,
                                        @Parameter(required = false, description = "금융지원 TAG") @RequestParam(required = false) String tag1,
                                        @Parameter(required = false, description = "교육 TAG") @RequestParam(required = false) String tag2,
                                        @Parameter(required = false, description = "생활지원 TAG") @RequestParam(required = false) String tag3,
                                        @Parameter(required = false, description = "금융자산 형성 TAG") @RequestParam(required = false) String tag4,
                                        HttpSession session) {

        ModelAndView mav = new ModelAndView("policy/policy_result");
        PolicyRequestDto requestDto = PolicyRequestDto.builder()
                .location(((location == null || location.equals(""))? null : location))
                .tag1(((tag1 == null || tag1.equals(""))? null : tag1))
                .tag2(((tag2 == null || tag2.equals(""))? null : tag2))
                .tag3(((tag3 == null || tag3.equals(""))? null : tag3))
                .tag4(((tag4 == null || tag4.equals(""))? null : tag4 + " 형성"))
                .keyword(((keyword == null || keyword.equals(""))? null : keyword))
                .build();

        if(policyService.judgePolicies(requestDto)){ //아무것도 입력하지 않은 경우

            List<PolicyResponseDto> result = policyService.selectAllPolicies();
            if(session.getAttribute("currentUser") != null){
                result = policyService.mappingPolicyHeart(result, (MemberDto) session.getAttribute("currentUser"));
            }
            Map<String, Object> finalResult = policyService.returnPoliciesByPage(result, pageIndex);

            model.addAttribute("policyList", finalResult.get("policyList"));
            model.addAttribute("pageEndIndex", finalResult.get("pageEndIndex"));
            model.addAttribute("currentPage", pageIndex);

        }
        else {

            List<PolicyResponseDto> result = policyService.selectDetailPolicies(requestDto);
            if(session.getAttribute("currentUser") != null){
                result = policyService.mappingPolicyHeart(result, (MemberDto) session.getAttribute("currentUser"));
            }
            Map<String, Object> finalResult = policyService.returnPoliciesByPage(result, pageIndex);

            model.addAttribute("policyList", finalResult.get("policyList"));
            model.addAttribute("pageEndIndex", finalResult.get("pageEndIndex"));
            model.addAttribute("currentPage", pageIndex);

            Map<String, String> requests = policyService.mappingRequest(requestDto);
            for (String key: requests.keySet()) {
                if(key.equals("request_tag4") && requests.get(key) != null)
                    model.addAttribute(key, "금융자산");
                else
                    model.addAttribute(key, requests.get(key));
            }
        }
        return mav;
    }

}
