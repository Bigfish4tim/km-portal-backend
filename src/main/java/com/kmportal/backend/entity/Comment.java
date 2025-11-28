package com.kmportal.backend.entity;

import com.kmportal.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Comment 엔티티 (게시글 댓글)
 *
 * 게시판 시스템의 댓글 기능을 담당하는 엔티티입니다.
 * BaseEntity를 상속받아 생성일시, 수정일시 등의 공통 필드를 자동으로 관리합니다.
 *
 * 주요 기능:
 * 1. 댓글 작성, 수정, 삭제 (Soft Delete 방식)
 * 2. 대댓글(Reply) 지원 - Self Referencing 관계
 * 3. 게시글(Board)과 다대일 관계
 * 4. 작성자(User)와 다대일 관계
 *
 * ==== 37일차 업데이트: 쿼리 최적화 ====
 * 1. N+1 문제 해결을 위한 @BatchSize 추가
 * 2. 복합 인덱스 추가 (게시글별 댓글 조회 최적화)
 * 3. 대댓글 조회 최적화 인덱스
 *
 * 작성일: 2025년 11월 21일 (30일차)
 * 수정일: 2025년 11월 28일 (37일차 - 인덱스 최적화)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.1 (37일차 인덱스 최적화)
 * @since 2025-11-21
 */
@Entity
@Table(name = "comments",
        indexes = {
                // ======================================
                // 기존 단일 컬럼 인덱스
                // ======================================

                /**
                 * 게시글별 댓글 조회 인덱스
                 */
                @Index(name = "idx_comment_board", columnList = "board_id"),

                /**
                 * 작성자별 댓글 조회 인덱스
                 */
                @Index(name = "idx_comment_author", columnList = "author_id"),

                /**
                 * 대댓글 조회 인덱스 (부모 댓글 ID)
                 */
                @Index(name = "idx_comment_parent", columnList = "parent_id"),

                // ======================================
                // 37일차 추가: 복합 인덱스 (쿼리 패턴 최적화)
                // ======================================

                /**
                 * 복합 인덱스 1: 게시글별 삭제되지 않은 댓글 + 작성일시
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM comments
                 * WHERE board_id = ? AND is_deleted = false
                 * ORDER BY created_at ASC
                 *
                 * 사용 빈도: 매우 높음 (게시글 상세에서 댓글 목록)
                 */
                @Index(name = "idx_comment_board_active_created",
                        columnList = "board_id, is_deleted, created_at ASC"),

                /**
                 * 복합 인덱스 2: 게시글별 최상위 댓글 (parent_id가 NULL)
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM comments
                 * WHERE board_id = ? AND parent_id IS NULL AND is_deleted = false
                 * ORDER BY created_at ASC
                 *
                 * 사용 빈도: 높음 (댓글 목록 - 대댓글 제외)
                 */
                @Index(name = "idx_comment_board_toplevel",
                        columnList = "board_id, parent_id, is_deleted, created_at ASC"),

                /**
                 * 복합 인덱스 3: 부모 댓글별 대댓글
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM comments
                 * WHERE parent_id = ? AND is_deleted = false
                 * ORDER BY created_at ASC
                 *
                 * 사용 빈도: 높음 (대댓글 목록)
                 */
                @Index(name = "idx_comment_parent_active",
                        columnList = "parent_id, is_deleted, created_at ASC"),

                /**
                 * 복합 인덱스 4: 작성자별 삭제되지 않은 댓글
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM comments
                 * WHERE author_id = ? AND is_deleted = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 중간 (마이페이지 - 내 댓글)
                 */
                @Index(name = "idx_comment_author_active",
                        columnList = "author_id, is_deleted, created_at DESC")
        })
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    // ====== 기본 필드 ======

    /**
     * 댓글 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    /**
     * 댓글 내용 (필수)
     * 최대 1000자
     */
    @Column(name = "content", nullable = false, length = 1000)
    @NotBlank(message = "댓글 내용은 필수 입력 항목입니다.")
    @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하로 입력해주세요.")
    private String content;

    // ====== 관계 필드 ======

    /**
     * 게시글 (Board와 다대일 관계)
     *
     * 37일차 최적화:
     * - LAZY 로딩 유지
     * - 필요 시 JOIN FETCH 사용
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @NotNull(message = "게시글 정보는 필수입니다.")
    private Board board;

    /**
     * 댓글 작성자 (User와 다대일 관계)
     *
     * 37일차 최적화:
     * - LAZY 로딩 유지
     * - 댓글 목록 조회 시 @BatchSize로 N+1 문제 해결
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull(message = "작성자 정보는 필수입니다.")
    private User author;

    /**
     * 부모 댓글 (Self Referencing - 대댓글 지원)
     * NULL이면 최상위 댓글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    /**
     * 대댓글 목록 (Self Referencing의 반대 방향)
     *
     * 37일차 최적화:
     * - @BatchSize(size = 20) 추가
     * - 한 번에 최대 20개의 대댓글을 배치로 로드
     * - N+1 문제 방지
     *
     * @BatchSize 설명:
     * - 예: 10개의 댓글이 있고 각각 대댓글이 있을 때
     * - @BatchSize 없으면: 10번의 추가 쿼리 발생 (N+1)
     * - @BatchSize(20) 있으면: 1번의 IN 쿼리로 모두 조회
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)  // 37일차 추가: N+1 문제 방지
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    // ====== 상태 필드 ======

    /**
     * 삭제 여부 (Soft Delete)
     * 기본값 false
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ====== 비즈니스 메서드들 ======

    /**
     * 댓글 논리적 삭제 메서드
     */
    public void softDelete() {
        this.isDeleted = true;
        super.softDelete();
    }

    /**
     * 댓글 복구 메서드
     */
    public void restore() {
        this.isDeleted = false;
        super.restore();
    }

    /**
     * 댓글이 삭제된 상태인지 확인
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }

    /**
     * 대댓글인지 확인
     */
    public boolean isReply() {
        return this.parent != null;
    }

    /**
     * 최상위 댓글인지 확인
     */
    public boolean isTopLevel() {
        return this.parent == null;
    }

    /**
     * 특정 사용자가 작성한 댓글인지 확인
     */
    public boolean isAuthor(Long userId) {
        return this.author != null &&
                this.author.getUserId() != null &&
                this.author.getUserId().equals(userId);
    }

    /**
     * 댓글 내용 업데이트 메서드
     */
    public void updateContent(String content) {
        if (content != null && !content.trim().isEmpty()) {
            this.content = content.trim();
        }
    }

    /**
     * 대댓글 추가 메서드
     */
    public void addReply(Comment reply) {
        this.replies.add(reply);
        reply.setParent(this);
        reply.setBoard(this.board);
    }

    /**
     * 대댓글 제거 메서드
     */
    public void removeReply(Comment reply) {
        this.replies.remove(reply);
        reply.setParent(null);
    }

    /**
     * 대댓글 개수 조회 메서드
     */
    public int getReplyCount() {
        if (this.replies == null) {
            return 0;
        }
        return (int) this.replies.stream()
                .filter(reply -> !reply.isDeleted())
                .count();
    }

    /**
     * 대댓글이 있는지 확인
     */
    public boolean hasReplies() {
        return getReplyCount() > 0;
    }

    /**
     * 게시글 ID 조회 편의 메서드
     */
    public Long getBoardId() {
        return this.board != null ? this.board.getId() : null;
    }

    /**
     * 작성자 ID 조회 편의 메서드
     */
    public Long getAuthorId() {
        return this.author != null ? this.author.getUserId() : null;
    }

    /**
     * 작성자 이름 조회 편의 메서드
     */
    public String getAuthorName() {
        return this.author != null ? this.author.getFullName() : "알 수 없음";
    }

    /**
     * 부모 댓글 ID 조회 편의 메서드
     */
    public Long getParentId() {
        return this.parent != null ? this.parent.getId() : null;
    }
}

/*
 * ====== 37일차 N+1 문제 해결 가이드 ======
 *
 * 1. N+1 문제란?
 *    - 1개의 쿼리로 N개의 엔티티를 조회 후
 *    - 각 엔티티의 연관 엔티티를 조회하기 위해 N번의 추가 쿼리 발생
 *    - 예: 10개 댓글 조회 후 각 댓글의 작성자를 조회하면 11번의 쿼리
 *
 * 2. 해결 방법:
 *
 *    a) @BatchSize (이 파일에서 사용)
 *       - 연관 엔티티를 IN 쿼리로 배치 조회
 *       - 설정이 간단하고 기존 코드 변경 불필요
 *       - 단점: 배치 크기 튜닝 필요
 *
 *    b) JOIN FETCH (Repository에서 사용)
 *       - JPQL에서 연관 엔티티를 함께 조회
 *       - 예: SELECT c FROM Comment c JOIN FETCH c.author
 *       - 장점: 한 번의 쿼리로 모든 데이터 조회
 *       - 단점: 페이징과 함께 사용 시 주의 필요
 *
 *    c) EntityGraph
 *       - @EntityGraph 어노테이션으로 페치 전략 지정
 *       - 장점: 선언적, 재사용 가능
 *       - 단점: 복잡한 그래프에서 관리 어려움
 *
 * 3. 모니터링:
 *    - application.yml에서 hibernate.generate_statistics: true 설정
 *    - Actuator /actuator/metrics/hibernate.statements 확인
 *    - 로그에서 쿼리 횟수 확인
 *
 * 4. @BatchSize 적정 크기:
 *    - 너무 작으면: 배치 쿼리 횟수 증가
 *    - 너무 크면: IN 절이 길어져 성능 저하
 *    - 권장: 10~50 사이 (테스트 후 조정)
 */