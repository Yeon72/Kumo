package net.kumo.kumo.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.kumo.kumo.domain.entity.CompanyEntity;
import net.kumo.kumo.domain.entity.UserEntity;
import net.kumo.kumo.repository.CompanyRepository;
import net.kumo.kumo.repository.OsakaGeocodedRepository;

/**
 * 구인자가 등록한 사업장(회사) 정보 관리에 대한 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final OsakaGeocodedRepository osakaGeocodedRepository;

    @Autowired
    private final MessageSource messageSource;

    /**
     * 특정 사용자(구인자) 계정에 등록된 전체 회사 목록을 조회합니다.
     *
     * @param user 조회를 요청한 사용자 엔티티
     * @return 해당 사용자가 등록한 회사 엔티티 리스트
     */
    public List<CompanyEntity> getCompanyList(UserEntity user) {
        return companyRepository.findAllByUser(user);
    }

    /**
     * 식별자를 기반으로 단일 회사 상세 정보를 조회합니다.
     *
     * @param id 조회할 회사의 고유 식별자
     * @return 조회된 회사 엔티티 (존재하지 않을 경우 null 반환)
     */
    public CompanyEntity getCompany(Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    /**
     * 신규 회사 정보를 등록하거나 기존 정보를 수정하여 데이터베이스에 저장합니다.
     *
     * @param company 저장할 회사 엔티티
     * @param user    해당 회사를 소유한 사용자 엔티티
     */
    @Transactional
    public void saveCompany(CompanyEntity company, UserEntity user) {
        company.setUser(user);
        companyRepository.save(company);
    }

    /**
     * 특정 회사 정보를 삭제합니다.
     * 단, 해당 회사를 참조하고 있는 구인 공고가 존재할 경우 삭제를 차단하고 다국어 에러 메시지를 반환합니다.
     *
     * @param companyId 삭제할 회사의 고유 식별자
     * @throws IllegalStateException 연관된 공고가 존재하여 삭제할 수 없을 때 발생
     */
    public void deleteCompany(Long companyId) {
        long count = osakaGeocodedRepository.countByCompany_CompanyId(companyId);

        if (count > 0) {
            Locale currentLocale = LocaleContextHolder.getLocale();
            String errorMessage = messageSource.getMessage(
                    "error.company.delete.inUse",
                    new Object[] { count },
                    currentLocale);

            throw new IllegalStateException(errorMessage);
        }

        companyRepository.deleteById(companyId);
    }
}