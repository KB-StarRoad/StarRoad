package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.SubscriptionDto;
import com.kb04.starroad.Dto.product.ProductDto;
import com.kb04.starroad.Dto.product.ProductResponseDto;
import com.kb04.starroad.Entity.Member;
import com.kb04.starroad.Service.ProductService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
public class ProductController {
    private final ProductService productService;

    private static final int ITEMS_PER_PAGE = 3;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/starroad/product")
    public ModelAndView product(
            Model model,
            @RequestParam(defaultValue = "1") int page,
            HttpServletRequest request) {

        Member loginMember = getLoginMember(request);
        List<ProductResponseDto> productList = null;
        if(loginMember == null) {
            productList = productService.getProductList();
            model.addAttribute("user", null);
        } else {
            Double monthlyAvailablePrice = getMonthlyAvailablePricePerMember(loginMember);
            productList = productService.getProductList(monthlyAvailablePrice);
            model.addAttribute("user", loginMember.getName());
            model.addAttribute("monthlyAvaiablePrice", monthlyAvailablePrice);
        }

        // 최대 기본 이율 = max_rate - max_condition_rate
        // 최대 이율 기간 = max_rate_period
        // 회원 우대 이율 들어가야됨
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, productList.size());

        model.addAttribute("productItems", productList.subList(startIndex, endIndex));
        model.addAttribute("pageEndIndex", Math.ceil(productList.size() / Double.valueOf(ITEMS_PER_PAGE)));
        model.addAttribute("currentPage", page);

//        model.addAttribute("price", 10000);

        ModelAndView mav = new ModelAndView("product/product");
        return mav;
    }

    private Double getMonthlyAvailablePricePerMember(Member loginMember) {
        int memberSalary = loginMember.getSalary();
        int memberGoal = loginMember.getGoal();
        Double monthGoal = (1.0 * memberSalary) * (1.0 * memberGoal) / 100;

        List<SubscriptionDto> subscriptionList = productService.getSubscriptions(loginMember.toMemberDto());
        int sum = 0; // 매달 이미 나가고 있는 적금의 양
        for (SubscriptionDto dto : subscriptionList) {
            if (dto.getProd().getType() == 'S') // 적금인 상품에 대해서 매달 나가는 비용 계산
                sum += dto.getPrice();
        }
        Double result = monthGoal - sum;

        return result;
    }

    // 검색했을 때 기간 적용 -> 최대 기본 이율
    private Double getRate(List<ProductResponseDto> productList, int period) {
        // base rate

        // condition rate


        return 0.0;
    }

    private static Member getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Member loginMember = (Member) session.getAttribute("currentUser");
        return loginMember;
    }

    @GetMapping("/starroad/product/result")
    public ModelAndView product_search_result(
            Model model,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String rate,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            HttpServletRequest request) {

        Member loginMember = getLoginMember(request);
        Double monthlyAvailablePrice = getMonthlyAvailablePricePerMember(loginMember);

        List<ProductResponseDto> productList = null;
        if (type != null || period != null || query != null)
            productList = productService.findByForm(type.charAt(0), Integer.parseInt(period), query);
        if (productList == null) {
            productList = productService.getProductList(monthlyAvailablePrice);
        }

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, productList.size());

        model.addAttribute("productItems", productList.subList(startIndex, endIndex));
        model.addAttribute("pageEndIndex", Math.ceil(productList.size() / Double.valueOf(ITEMS_PER_PAGE)));
        model.addAttribute("currentPage", page);
        model.addAttribute("user", loginMember.getName());
        model.addAttribute("monthlyAvaiablePrice", monthlyAvailablePrice);

        if (type != null)
            model.addAttribute("type", type);
        if (period != null)
            model.addAttribute("period", period);
        if (rate != null)
            model.addAttribute("rate", rate);
        if (query != null)
            model.addAttribute("query", query);

        model.addAttribute("user", "장서우");
        model.addAttribute("price", 10000);

        ModelAndView mav = new ModelAndView("product/product_result");
        return mav;
    }
}
