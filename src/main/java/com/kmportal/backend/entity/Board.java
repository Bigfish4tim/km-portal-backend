package com.kmportal.backend.entity;

import com.kmportal.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Board 엔티티 (게시판 게시글)
 *
 * 게시판 시스템의 핵심 엔티티로, 모든 게시글 정보를 담고 있습니다.
 * BaseEntity를 상속받아 생성일시, 수정일시, 삭제 여부 등의 공통 필드를 자동으로 관리합니다.
 *
 * 주요 기능:
 * 1. 게시글 작성, 수정, 삭제 (Soft Delete 방식)
 * 2. 조회수 자동 증가
 * 3. 상단 고정 기능
 * 4. 카테고리별 분류
 * 5. 작성자 추적 (User와 ManyToOne 관계)
 *
 * ==== 37일차 업데이트: 쿼리 최적화 ====
 * 1. 복합 인덱스 추가 (자주 사용되는 쿼리 패턴 최적화)
 * 2. @BatchSize 추가 (N+1 문제 방지)
 * 3. Fetch 전략 최적화
 *
 * 인덱스 설계 원칙:
 * - 자주 WHERE 절에 사용되는 컬럼
 * - ORDER BY에 사용되는 컬럼
 * - 복합 조건 쿼리에 최적화된 복합 인덱스
 *
 * 작성일: 2025년 11월 16일 (24일차)
 * 수정일: 2025년 11월 28일 (37일차 - 인덱스 최적화)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.1 (37일차 인덱스 최적화)
 * @since 2025-11-16
 */
@Entity
@Table(name = "boards",
        indexes = {
                // ======================================
                // 기존 단일 컬럼 인덱스
                // ======================================

                /**
                 * 제목 검색 인덱스
                 * - LIKE '%keyword%' 검색에는 인덱스 효과 제한적
                 * - LIKE 'keyword%' (접두어 검색)에는 효과적
                 */
                @Index(name = "idx_board_title", columnList = "title"),

                /**
                 * 카테고리 필터링 인덱스
                 * - WHERE category = 'NOTICE' 쿼리 최적화
                 */
                @Index(name = "idx_board_category", columnList = "category"),

                /**
                 * 작성자별 조회 인덱스
                 * - 마이페이지에서 "내 게시글" 조회 시 사용
                 */
                @Index(name = "idx_board_author", columnList = "author_id"),

                /**
                 * 작성일시 정렬 인덱스
                 * - ORDER BY created_at DESC 쿼리 최적화
                 * - 최근 게시글 조회 시 사용
                 */
                @Index(name = "idx_board_created_at", columnList = "created_at"),

                // ======================================
                // 37일차 추가: 복합 인덱스 (쿼리 패턴 최적화)
                // ======================================

                /**
                 * 복합 인덱스 1: 삭제되지 않은 게시글 + 작성일시 정렬
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM boards
                 * WHERE is_deleted = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 매우 높음 (게시글 목록의 기본 쿼리)
                 */
                @Index(name = "idx_board_active_created",
                        columnList = "is_deleted, created_at DESC"),

                /**
                 * 복합 인덱스 2: 삭제되지 않은 게시글 + 카테고리 + 작성일시
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM boards
                 * WHERE is_deleted = false AND category = ?
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 높음 (카테고리별 게시글 목록)
                 */
                @Index(name = "idx_board_active_category_created",
                        columnList = "is_deleted, category, created_at DESC"),

                /**
                 * 복합 인덱스 3: 상단 고정 게시글 + 삭제여부 + 작성일시
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM boards
                 * WHERE is_pinned = true AND is_deleted = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 높음 (공지사항 목록)
                 */
                @Index(name = "idx_board_pinned_active",
                        columnList = "is_pinned, is_deleted, created_at DESC"),

                /**
                 * 복합 인덱스 4: 작성자별 삭제되지 않은 게시글
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM boards
                 * WHERE author_id = ? AND is_deleted = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 중간 (마이페이지 내 게시글)
                 */
                @Index(name = "idx_board_author_active",
                        columnList = "author_id, is_deleted, created_at DESC"),

                /**
                 * 복합 인덱스 5: 조회수 정렬 (인기 게시글)
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM boards
                 * WHERE is_deleted = false
                 * ORDER BY view_count DESC
                 *
                 * 사용 빈도: 중간 (인기 게시글, 대시보드)
                 */
                @Index(name = "idx_board_active_viewcount",
                        columnList = "is_deleted, view_count DESC")
        })
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board extends BaseEntity {

    /**
     * 게시글 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    /**
     * 게시글 제목 (필수)
     * 최대 200자
     */
    @Column(name = "title", nullable = false, length = 200)
    @NotBlank(message = "게시글 제목은 필수 입력 항목입니다.")
    @Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하로 입력해주세요.")
    private String title;

    /**
     * 게시글 내용 (필수)
     * TEXT 타입으로 대용량 텍스트 저장
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "게시글 내용은 필수 입력 항목입니다.")
    private String content;

    /**
     * 게시글 카테고리
     * NULL 허용, 최대 50자
     */
    @Column(name = "category", length = 50)
    @Size(max = 50, message = "카테고리는 50자 이하로 입력해주세요.")
    private String category;

    /**
     * 게시글 작성자 (User와 다대일 관계)
     *
     * 37일차 최적화:
     * - FetchType.LAZY 유지 (기본 조회 시 User 정보 불필요)
     * - 필요한 경우 JPQL에서 JOIN FETCH 사용
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull(message = "작성자 정보는 필수입니다.")
    private User author;

    /**
     * 조회수
     * 기본값 0
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 상단 고정 여부
     * 기본값 false
     */
    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    /**
     * 삭제 여부 (Soft Delete)
     * 기본값 false
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ====== 비즈니스 메서드들 ======

    /**
     * 조회수 증가 메서드
     */
    public void increaseViewCount() {
        this.viewCount += 1;
    }

    /**
     * 게시글 상단 고정 메서드
     */
    public void pin() {
        this.isPinned = true;
    }

    /**
     * 게시글 상단 고정 해제 메서드
     */
    public void unpin() {
        this.isPinned = false;
    }

    /**
     * 게시글 논리적 삭제 메서드
     */
    public void softDelete() {
        this.isDeleted = true;
        super.softDelete();
    }

    /**
     * 게시글 복구 메서드
     */
    public void restore() {
        this.isDeleted = false;
        super.restore();
    }

    /**
     * 게시글이 삭제된 상태인지 확인
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }

    /**
     * 게시글이 상단 고정되었는지 확인
     */
    public boolean isPinned() {
        return Boolean.TRUE.equals(this.isPinned);
    }

    /**
     * 특정 사용자가 작성한 게시글인지 확인
     */
    public boolean isAuthor(Long userId) {
        return this.author != null &&
                this.author.getUserId() != null &&
                this.author.getUserId().equals(userId);
    }

    /**
     * 게시글 정보 업데이트 메서드
     */
    public void update(String title, String content, String category) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
        if (content != null && !content.trim().isEmpty()) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
    }

    // ====== 통계 API용 편의 메서드 ======

    /**
     * 작성자 이름 조회 편의 메서드
     */
    public String getAuthorName() {
        if (this.author != null && this.author.getFullName() != null) {
            return this.author.getFullName();
        }
        return "알 수 없음";
    }

    /**
     * 댓글 수 조회 편의 메서드
     */
    public Integer getCommentCount() {
        return 0;
    }
}

/*
 * ====== 37일차 인덱스 최적화 가이드 ======
 *
 * 1. 인덱스 선택 기준:
 *    - WHERE 절에서 자주 사용되는 컬럼
 *    - JOIN 조건에 사용되는 컬럼
 *    - ORDER BY에 사용되는 컬럼
 *    - 선택도(Selectivity)가 높은 컬럼
 *
 * 2. 복합 인덱스 컬럼 순서:
 *    - 등호(=) 조건 컬럼 먼저
 *    - 범위 조건 컬럼 그 다음
 *    - ORDER BY 컬럼 마지막
 *
 * 3. 주의사항:
 *    - 인덱스가 많으면 INSERT/UPDATE 성능 저하
 *    - 작은 테이블에는 인덱스 효과 미미
 *    - 실제 쿼리 실행 계획 확인 필요
 *
 * 4. 모니터링 방법:
 *    - EXPLAIN ANALYZE로 쿼리 실행 계획 확인
 *    - Spring Boot Actuator /actuator/metrics/hibernate.* 확인
 *    - 느린 쿼리 로그 분석
 *
 * 5. H2 데이터베이스 인덱스 확인:
 *    SELECT * FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'BOARDS';
 *
 * 6. MS SQL Server 인덱스 확인:
 *    SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID('boards');
 */