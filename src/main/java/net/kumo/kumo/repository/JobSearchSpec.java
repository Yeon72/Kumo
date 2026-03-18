package net.kumo.kumo.repository;

import jakarta.persistence.criteria.Predicate;
import net.kumo.kumo.domain.entity.BaseEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification을 활용하여 구인 공고(Job) 조회 시 동적 검색 조건을 생성하는 유틸리티 클래스입니다.
 * 지역별, 좌표 유무별로 분리된 다수의 엔티티 테이블(Tokyo, Osaka 등)에 대해
 * 제네릭 타입(T extends BaseEntity)을 사용하여 일관된 검색 조건(WHERE 절)을 제공합니다.
 */
public class JobSearchSpec {

    /**
     * 사용자가 입력한 키워드와 지역 필터 조건을 기반으로 JPA 동적 쿼리 조건(Specification)을 조립하여 반환합니다.
     *
     * @param keyword      사용자가 입력한 검색 키워드. (제목 및 회사명 필드에서 다국어 LIKE 검색 수행)
     * @param subRegion    사용자가 선택한 세부 지역 명칭. (지정된 지역 컬럼들에서 LIKE 검색 수행)
     * @param regionFields 동적 검색을 수행할 엔티티의 대상 지역 컬럼명 가변 인자.
     * (예: 도쿄 엔티티 - "wardCityJp", 좌표 없는 엔티티 - "address" 등)
     * @param <T>          BaseEntity를 상속받는 구인 공고 엔티티 타입
     * @return             조립이 완료된 JPA Specification 객체
     */
    public static <T extends BaseEntity> Specification<T> searchConditions(String keyword, String subRegion, String... regionFields) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword + "%";

                predicates.add(cb.or(
                        cb.like(root.get("title"), pattern),
                        cb.like(root.get("titleJp"), pattern),
                        cb.like(root.get("companyName"), pattern),
                        cb.like(root.get("companyNameJp"), pattern)
                ));
            }

            if (StringUtils.hasText(subRegion)) {
                List<Predicate> regionPreds = new ArrayList<>();

                for (String field : regionFields) {
                    regionPreds.add(cb.like(root.get(field), "%" + subRegion + "%"));
                }

                predicates.add(cb.or(regionPreds.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}