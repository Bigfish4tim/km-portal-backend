package com.kmportal.backend.entity;

import com.kmportal.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
 * Self Referencing 관계란?
 * - 같은 테이블을 자기 자신이 참조하는 관계
 * - parent 필드: 부모 댓글을 참조 (대댓글일 경우)
 * - replies 필드: 자식 댓글 목록 (이 댓글에 달린 대댓글들)
 * - parent가 null이면 최상위 댓글 (일반 댓글)
 * - parent가 있으면 대댓글
 *
 * 예시 구조:
 * 댓글 1 (parent = null, 최상위 댓글)
 *  ├─ 대댓글 1-1 (parent = 댓글 1)
 *  └─ 대댓글 1-2 (parent = 댓글 1)
 * 댓글 2 (parent = null, 최상위 댓글)
 *
 * 작성일: 2025년 11월 21일 (30일차)
 * 작성자: 30일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-21
 */
@Entity
@Table(name = "comments",  // 데이터베이스 테이블명을 'comments'로 지정
        indexes = {
                // 인덱스 생성: 자주 조회되는 컬럼에 인덱스를 걸어 검색 성능 향상
                @Index(name = "idx_comment_board", columnList = "board_id"),      // 게시글별 댓글 조회 최적화
                @Index(name = "idx_comment_author", columnList = "author_id"),     // 작성자별 댓글 조회 최적화
                @Index(name = "idx_comment_parent", columnList = "parent_id"),     // 대댓글 조회 최적화
                @Index(name = "idx_comment_created_at", columnList = "created_at"), // 날짜 정렬 최적화
                @Index(name = "idx_comment_deleted", columnList = "is_deleted")    // 삭제 여부 필터링 최적화
        })
@Data  // Lombok: Getter, Setter, toString, equals, hashCode 자동 생성
@EqualsAndHashCode(callSuper = false)  // BaseEntity 상속 시 경고 제거
@NoArgsConstructor  // Lombok: 파라미터 없는 기본 생성자 자동 생성 (JPA 필수)
@AllArgsConstructor // Lombok: 모든 필드를 파라미터로 받는 생성자 자동 생성
@Builder  // Lombok: 빌더 패턴 자동 생성 (객체 생성을 더 쉽고 읽기 좋게)
public class Comment extends BaseEntity {

    // ====== 기본 필드 ======

    /**
     * 댓글 ID (Primary Key)
     *
     * @GeneratedValue: 자동 증가 전략 사용
     * - GenerationType.IDENTITY: 데이터베이스의 AUTO_INCREMENT 기능 활용
     * - MySQL/H2의 경우 INSERT 시 자동으로 1, 2, 3... 순서대로 증가
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    /**
     * 댓글 내용 (필수)
     *
     * @NotBlank: 빈 문자열(""), 공백(" "), null 모두 불허
     * - 사용자가 댓글 내용을 입력하지 않으면 에러 발생
     *
     * 최대 길이: 1000자
     * - 일반적인 댓글은 1000자면 충분함
     * - 게시글 본문과 달리 짧은 텍스트이므로 @Lob 사용하지 않음
     */
    @Column(name = "content", nullable = false, length = 1000)
    @NotBlank(message = "댓글 내용은 필수 입력 항목입니다.")
    @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하로 입력해주세요.")
    private String content;

    // ====== 관계 필드 ======

    /**
     * 게시글 (Board와 다대일 관계)
     *
     * @ManyToOne: 여러 댓글(Comment)은 하나의 게시글(Board)에 속함
     * - Many = Comment (여러 개)
     * - One = Board (한 개)
     * - 한 게시글에 여러 댓글이 달릴 수 있음
     *
     * fetch = FetchType.LAZY: 지연 로딩 (성능 최적화)
     * - 댓글 조회 시 게시글 정보는 실제로 필요할 때만 조회
     *
     * @JoinColumn: 외래키(Foreign Key) 설정
     * - name = "board_id": 데이터베이스 컬럼명
     * - nullable = false: 댓글은 반드시 게시글에 속해야 함
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @NotNull(message = "게시글 정보는 필수입니다.")
    private Board board;

    /**
     * 댓글 작성자 (User와 다대일 관계)
     *
     * @ManyToOne: 여러 댓글(Comment)은 한 명의 사용자(User)에게 속함
     * - 한 사용자는 여러 댓글을 작성할 수 있음
     *
     * fetch = FetchType.LAZY: 지연 로딩 (성능 최적화)
     * - 댓글 조회 시 작성자 정보는 실제로 필요할 때만 조회
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull(message = "작성자 정보는 필수입니다.")
    private User author;

    /**
     * 부모 댓글 (Self Referencing - 대댓글 지원)
     *
     * @ManyToOne: 여러 대댓글은 하나의 부모 댓글을 가질 수 있음
     *
     * nullable = true: 최상위 댓글은 부모가 없음
     * - parent == null: 일반 댓글 (최상위)
     * - parent != null: 대댓글
     *
     * 예시:
     * - "좋은 글이네요!" (parent = null) → 일반 댓글
     * - "저도 그렇게 생각해요!" (parent = 위 댓글) → 대댓글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")  // nullable = true가 기본값
    private Comment parent;

    /**
     * 대댓글 목록 (Self Referencing의 반대 방향)
     *
     * @OneToMany: 하나의 댓글에 여러 대댓글이 달릴 수 있음
     * - mappedBy = "parent": Comment 엔티티의 parent 필드와 매핑
     *
     * cascade = CascadeType.ALL: 연관 엔티티 작업 전파
     * - 부모 댓글 삭제 시 대댓글도 함께 처리 (단, Soft Delete 사용 권장)
     *
     * orphanRemoval = true: 고아 객체 자동 삭제
     * - replies에서 제거된 대댓글은 자동 삭제
     *
     * @Builder.Default: Builder 패턴 사용 시 기본값 설정
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    // ====== 상태 필드 ======

    /**
     * 삭제 여부 (Soft Delete)
     *
     * true: 삭제된 댓글 (목록에 표시 안 됨)
     * false: 정상 댓글
     *
     * 기본값은 false (정상)입니다.
     *
     * Soft Delete 장점:
     * - 삭제된 댓글 복구 가능
     * - 대댓글이 있어도 안전하게 삭제
     * - "삭제된 댓글입니다" 표시 가능
     * - 통계 데이터 유지
     *
     * 삭제된 댓글 표시 방식:
     * - 대댓글이 없는 경우: 완전히 숨김
     * - 대댓글이 있는 경우: "삭제된 댓글입니다" 표시
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ====== 비즈니스 메서드들 ======

    /**
     * 댓글 논리적 삭제 메서드
     *
     * 실제 데이터베이스에서 삭제하지 않고 삭제 플래그만 설정합니다.
     *
     * 사용 예시:
     * comment.softDelete();
     * commentRepository.save(comment);
     */
    public void softDelete() {
        this.isDeleted = true;
        // BaseEntity의 softDelete()도 함께 호출 (deletedAt 설정)
        super.softDelete();
    }

    /**
     * 댓글 복구 메서드
     *
     * 삭제된 댓글을 복구합니다.
     *
     * 사용 예시:
     * comment.restore();
     * commentRepository.save(comment);
     */
    public void restore() {
        this.isDeleted = false;
        // BaseEntity의 restore()도 함께 호출
        super.restore();
    }

    /**
     * 댓글이 삭제된 상태인지 확인하는 메서드
     *
     * @return true: 삭제됨, false: 정상
     *
     * 사용 예시:
     * if (comment.isDeleted()) {
     *     return "삭제된 댓글입니다.";
     * }
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }

    /**
     * 대댓글인지 확인하는 메서드
     *
     * @return true: 대댓글, false: 최상위 댓글
     *
     * 사용 예시:
     * if (comment.isReply()) {
     *     // 대댓글 스타일 적용 (들여쓰기 등)
     * }
     */
    public boolean isReply() {
        return this.parent != null;
    }

    /**
     * 최상위 댓글인지 확인하는 메서드
     *
     * @return true: 최상위 댓글, false: 대댓글
     */
    public boolean isTopLevel() {
        return this.parent == null;
    }

    /**
     * 특정 사용자가 작성한 댓글인지 확인하는 메서드
     *
     * @param userId 확인할 사용자 ID
     * @return true: 해당 사용자가 작성함, false: 다른 사용자가 작성함
     *
     * 사용 예시:
     * if (!comment.isAuthor(currentUserId)) {
     *     throw new UnauthorizedException("본인이 작성한 댓글만 수정/삭제할 수 있습니다.");
     * }
     */
    public boolean isAuthor(Long userId) {
        return this.author != null &&
                this.author.getUserId() != null &&
                this.author.getUserId().equals(userId);
    }

    /**
     * 댓글 내용 업데이트 메서드
     *
     * 댓글 수정 시 내용을 업데이트합니다.
     *
     * @param content 새로운 댓글 내용
     *
     * 사용 예시:
     * comment.updateContent("수정된 댓글 내용");
     * commentRepository.save(comment);
     */
    public void updateContent(String content) {
        if (content != null && !content.trim().isEmpty()) {
            this.content = content.trim();
        }
    }

    /**
     * 대댓글 추가 메서드
     *
     * 이 댓글에 대댓글을 추가합니다.
     * 양방향 관계를 유지하기 위해 parent도 함께 설정합니다.
     *
     * @param reply 추가할 대댓글
     *
     * 사용 예시:
     * Comment reply = Comment.builder().content("대댓글입니다").build();
     * parentComment.addReply(reply);
     */
    public void addReply(Comment reply) {
        this.replies.add(reply);
        reply.setParent(this);
        reply.setBoard(this.board);  // 대댓글도 같은 게시글에 속함
    }

    /**
     * 대댓글 제거 메서드
     *
     * 이 댓글에서 대댓글을 제거합니다.
     *
     * @param reply 제거할 대댓글
     */
    public void removeReply(Comment reply) {
        this.replies.remove(reply);
        reply.setParent(null);
    }

    /**
     * 대댓글 개수 조회 메서드
     *
     * 삭제되지 않은 대댓글의 개수를 반환합니다.
     *
     * @return 대댓글 개수
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
     * 대댓글이 있는지 확인하는 메서드
     *
     * 삭제되지 않은 대댓글이 있는지 확인합니다.
     *
     * @return true: 대댓글 있음, false: 대댓글 없음
     */
    public boolean hasReplies() {
        return getReplyCount() > 0;
    }

    /**
     * 게시글 ID 조회 편의 메서드
     *
     * 댓글이 속한 게시글의 ID를 반환합니다.
     *
     * @return 게시글 ID (board가 null이면 null 반환)
     */
    public Long getBoardId() {
        return this.board != null ? this.board.getId() : null;
    }

    /**
     * 작성자 ID 조회 편의 메서드
     *
     * 댓글 작성자의 ID를 반환합니다.
     *
     * @return 작성자 ID (author가 null이면 null 반환)
     */
    public Long getAuthorId() {
        return this.author != null ? this.author.getUserId() : null;
    }

    /**
     * 작성자 이름 조회 편의 메서드
     *
     * 댓글 작성자의 이름을 반환합니다.
     *
     * @return 작성자 이름 (author가 null이면 "알 수 없음" 반환)
     */
    public String getAuthorName() {
        return this.author != null ? this.author.getFullName() : "알 수 없음";
    }

    /**
     * 부모 댓글 ID 조회 편의 메서드
     *
     * 대댓글인 경우 부모 댓글의 ID를 반환합니다.
     *
     * @return 부모 댓글 ID (parent가 null이면 null 반환)
     */
    public Long getParentId() {
        return this.parent != null ? this.parent.getId() : null;
    }
}

/*
 * ====== Self Referencing 관계 상세 설명 ======
 *
 * Self Referencing이란?
 * - 같은 테이블(엔티티)이 자기 자신을 참조하는 관계
 * - 계층 구조를 표현할 때 사용 (댓글-대댓글, 카테고리-하위카테고리 등)
 *
 * 데이터베이스 구조:
 * +----------+------------+-----------+-----------+---------+
 * |comment_id| content    | board_id  | author_id | parent_id|
 * +----------+------------+-----------+-----------+---------+
 * |    1     | "댓글1"    |     1     |     1     |   NULL   | ← 최상위
 * |    2     | "대댓글1"  |     1     |     2     |     1    | ← 1번의 대댓글
 * |    3     | "대댓글2"  |     1     |     3     |     1    | ← 1번의 대댓글
 * |    4     | "댓글2"    |     1     |     1     |   NULL   | ← 최상위
 * +----------+------------+-----------+-----------+---------+
 *
 * 조회 방법:
 * 1. 최상위 댓글만 조회: WHERE parent_id IS NULL
 * 2. 특정 댓글의 대댓글 조회: WHERE parent_id = {댓글ID}
 *
 * 주의사항:
 * 1. 무한 루프 방지: 대댓글의 대댓글은 1단계만 지원 권장
 * 2. N+1 문제: 대댓글 조회 시 별도 쿼리 필요
 * 3. 삭제 처리: 대댓글이 있는 댓글 삭제 시 Soft Delete 권장
 */

/*
 * ====== 향후 추가할 수 있는 기능들 ======
 *
 * 1. 좋아요 수:
 *    @Column(name = "like_count")
 *    private Integer likeCount = 0;
 *
 * 2. 신고 횟수:
 *    @Column(name = "report_count")
 *    private Integer reportCount = 0;
 *
 * 3. 수정 여부:
 *    @Column(name = "is_edited")
 *    private Boolean isEdited = false;
 *
 * 4. 댓글 깊이 (다단계 대댓글 지원):
 *    @Column(name = "depth")
 *    private Integer depth = 0;  // 0: 최상위, 1: 대댓글, 2: 대대댓글...
 *
 * 5. 비밀 댓글:
 *    @Column(name = "is_secret")
 *    private Boolean isSecret = false;
 */