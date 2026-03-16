package net.kumo.kumo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.entity.CompanyEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.security.AuthenticatedUser;
import net.kumo.kumo.service.CompanyService;
import net.kumo.kumo.service.RecruiterService;

/**
 * 구인자의 회사 정보 등록, 조회, 수정, 삭제 요청을 처리하는 Controller 클래스입니다.
 */
@Controller
@RequestMapping("/Recruiter")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final RecruiterService recruiterService;

    @Value("${kumo.google.maps.keys}")
    private String googleMapsKey;

    /**
     * 회사 정보 관리 메인 페이지를 렌더링하며, 회사 목록 조회 및 신규 등록 폼 상태를 제어합니다.
     *
     * @param id                조회할 특정 회사 식별자 (선택)
     * @param model             뷰에 전달할 데이터를 담는 Model 객체
     * @param authenticatedUser 현재 인증된 사용자(구인자) 정보
     * @return 회사 정보 관리 뷰 파일명
     */
    @GetMapping("/CompanyInfo")
    public String companyInfo(@RequestParam(value = "id", required = false) Long id,
                              Model model, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        if (authenticatedUser == null) {
            return "redirect:/login";
        }

        UserEntity loginUser = recruiterService.getCurrentUser(authenticatedUser.getUsername());

        List<CompanyEntity> companyList = companyService.getCompanyList(loginUser);
        CompanyEntity currentCompany;

        model.addAttribute("currentMenu", "companyInfo");

        if (id == null) {
            currentCompany = new CompanyEntity();
            model.addAttribute("isNew", true);
        } else {
            currentCompany = companyService.getCompany(id);
            model.addAttribute("isNew", false);
        }

        model.addAttribute("companyList", companyList);
        model.addAttribute("currentCompany", currentCompany);
        model.addAttribute("googleMapsKey", googleMapsKey);

        return "recruiterView/companyInfo";
    }

    /**
     * 신규 회사 정보를 등록하거나 기존 회사 정보를 수정(Update) 처리합니다.
     *
     * @param company           수정할 회사 정보를 담은 엔티티 객체
     * @param authenticatedUser 현재 인증된 사용자 정보
     * @return 처리 완료 후 회사 정보 상세 페이지로 리다이렉트
     */
    @PostMapping("/CompanyUpdate")
    public String updateCompany(@ModelAttribute CompanyEntity company,
                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        if (authenticatedUser == null) {
            return "redirect:/login";
        }

        UserEntity loginUser = recruiterService.getCurrentUser(authenticatedUser.getUsername());
        companyService.saveCompany(company, loginUser);

        return "redirect:/Recruiter/CompanyInfo?id=" + company.getCompanyId();
    }

    /**
     * 특정 회사 정보를 시스템에서 삭제합니다.
     * 연관된 데이터가 있을 경우 삭제가 제한될 수 있습니다.
     *
     * @param id 삭제할 회사 식별자
     * @return 처리 성공 여부를 포함한 ResponseEntity
     */
    @DeleteMapping("/api/company/delete/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        try {
            companyService.deleteCompany(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    /**
     * 신규 회사 등록을 위한 폼 페이지 진입점입니다.
     * 단일 뷰 아키텍처 구성을 위해 통합 관리 페이지로 리다이렉트 처리합니다.
     *
     * @return 회사 정보 관리 메인 페이지 리다이렉트 URL
     */
    @GetMapping("/CompanyAdd")
    public String companyAddForm() {
        return "redirect:/Recruiter/CompanyInfo";
    }
}