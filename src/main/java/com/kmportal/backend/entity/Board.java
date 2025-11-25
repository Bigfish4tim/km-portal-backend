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
 * 작성일: 2025년 11월 16일 (24일차)
 * 작성자: 24일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-16
 */
@Entity
@Table(name = "boards",  // 데이터베이스 테이블명을 'boards'로 지정
        indexes = {
                // 인덱스 생성: 자주 조회되는 컬럼에 인덱스를 걸어 검색 성능 향상
                @Index(name = "idx_board_title", columnList = "title"),           // 제목 검색 최적화
                @Index(name = "idx_board_category", columnList = "category"),     // 카테고리 필터링 최적화
                @Index(name = "idx_board_author", columnList = "author_id"),      // 작성자별 조회 최적화
                @Index(name = "idx_board_created_at", columnList = "created_at"), // 날짜 정렬 최적화
                @Index(name = "idx_board_pinned", columnList = "is_pinned"),      // 상단 고정 조회 최적화
                @Index(name = "idx_board_deleted", columnList = "is_deleted")     // 삭제 여부 필터링 최적화
        })
@Data  // Lombok: Getter, Setter, toString, equals, hashCode 자동 생성
@EqualsAndHashCode(callSuper = false)  // BaseEntity 상속 시 경고 제거
@NoArgsConstructor  // Lombok: 파라미터 없는 기본 생성자 자동 생성 (JPA 필수)
@AllArgsConstructor // Lombok: 모든 필드를 파라미터로 받는 생성자 자동 생성
@Builder  // Lombok: 빌더 패턴 자동 생성 (객체 생성을 더 쉽고 읽기 좋게)
public class Board extends BaseEntity {

    /**
     * 게시글 ID (Primary Key)
     *
     * @GeneratedValue: 자동 증가 전략 사용
     * - GenerationType.IDENTITY: 데이터베이스의 AUTO_INCREMENT 기능 활용
     * - MySQL/H2의 경우 INSERT 시 자동으로 1, 2, 3... 순서대로 증가
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    /**
     * 게시글 제목 (필수)
     *
     * 제약 조건:
     * - NULL 불가 (nullable = false)
     * - 최대 200자 (length = 200)
     * - 최소 1자 이상 필수 (@NotBlank)
     *
     * @NotBlank: 빈 문자열(""), 공백(" "), null 모두 불허
     * - 사용자가 제목을 입력하지 않으면 에러 발생
     */
    @Column(name = "title", nullable = false, length = 200)
    @NotBlank(message = "게시글 제목은 필수 입력 항목입니다.")
    @Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하로 입력해주세요.")
    private String title;

    /**
     * 게시글 내용 (필수)
     *
     * @Lob: Large Object
     * - 대용량 텍스트 저장 가능 (수십만 자 가능)
     * - 데이터베이스에서 TEXT 또는 CLOB 타입으로 저장됨
     * - Quill Editor로 작성한 HTML 내용이 저장됨
     *
     * 사용 예시:
     * - 게시글 본문 (HTML 포함)
     * - 이미지 태그, 링크, 서식 등 모두 저장
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "게시글 내용은 필수 입력 항목입니다.")
    private String content;

    /**
     * 게시글 카테고리
     *
     * 게시글을 분류하기 위한 카테고리입니다.
     *
     * 예시 카테고리:
     * - "NOTICE" : 공지사항
     * - "FREE" : 자유 게시판
     * - "QNA" : 질문과 답변
     * - "TECH" : 기술 게시판
     * - "REVIEW" : 후기
     * - "ETC" : 기타
     *
     * NULL 허용: 카테고리를 지정하지 않을 수도 있음
     * 최대 50자
     */
    @Column(name = "category", length = 50)
    @Size(max = 50, message = "카테고리는 50자 이하로 입력해주세요.")
    private String category;

    /**
     * 게시글 작성자 (User와 다대일 관계)
     *
     * @ManyToOne: 여러 게시글(Board)은 한 명의 사용자(User)에게 속함
     * - Many = Board (여러 개)
     * - One = User (한 명)
     * - 한 사용자는 여러 게시글을 작성할 수 있음
     *
     * fetch = FetchType.LAZY: 지연 로딩 (성능 최적화)
     * - 게시글 조회 시 작성자 정보는 실제로 필요할 때만 조회
     * - EAGER를 사용하면 항상 User 정보까지 함께 조회 (비권장)
     *
     * @JoinColumn: 외래키(Foreign Key) 설정
     * - name = "author_id": 데이터베이스 컬럼명
     * - nullable = false: 작성자는 필수 (게시글은 반드시 작성자가 있어야 함)
     *
     * 사용 예시:
     * - board.getAuthor().getUsername() : 작성자 아이디 조회
     * - board.getAuthor().getFullName() : 작성자 이름 조회
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull(message = "작성자 정보는 필수입니다.")
    private User author;

    /**
     * 조회수
     *
     * 게시글이 조회될 때마다 자동으로 증가합니다.
     * 기본값은 0입니다.
     *
     * @Builder.Default: Lombok Builder 패턴 사용 시 기본값 설정
     * - Board.builder()로 객체 생성 시 viewCount가 자동으로 0으로 초기화
     *
     * 조회수 증가 로직:
     * 1. 사용자가 게시글 상세 페이지 접속
     * 2. BoardService에서 increaseViewCount() 메서드 호출
     * 3. viewCount += 1
     * 4. 데이터베이스에 저장
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 상단 고정 여부
     *
     * true: 게시판 목록 최상단에 항상 표시 (공지사항 등)
     * false: 일반 게시글 (작성일시 순으로 정렬)
     *
     * 기본값은 false (일반 게시글)입니다.
     *
     * 사용 예시:
     * - 공지사항을 상단에 고정
     * - 중요 이벤트 게시글 고정
     * - 관리자만 상단 고정 가능 (권한 필요)
     */
    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    /**
     * 삭제 여부 (Soft Delete)
     *
     * true: 삭제된 게시글 (목록에 표시 안 됨)
     * false: 정상 게시글
     *
     * 기본값은 false (정상)입니다.
     *
     * Soft Delete란?
     * - 실제 데이터베이스에서 삭제하지 않고 플래그만 변경
     * - 장점: 데이터 복구 가능, 참조 무결성 유지, 통계 분석 가능
     * - 실제 삭제는 관리자가 주기적으로 일괄 삭제 (배치 작업)
     *
     * 주의: BaseEntity에도 isDeleted가 있지만, 명시적으로 Board에도 추가
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ====== 비즈니스 메서드들 ======

    /**
     * 조회수 증가 메서드
     *
     * 게시글 상세 조회 시 자동으로 호출됩니다.
     *
     * 사용 예시:
     * Board board = boardRepository.findById(boardId);
     * board.increaseViewCount();  // 조회수 +1
     * boardRepository.save(board); // 변경사항 저장
     */
    public void increaseViewCount() {
        this.viewCount += 1;
    }

    /**
     * 게시글 상단 고정 메서드
     *
     * 관리자가 중요한 게시글을 상단에 고정할 때 사용합니다.
     *
     * 사용 예시:
     * board.pin();  // 상단 고정
     * boardRepository.save(board);
     */
    public void pin() {
        this.isPinned = true;
    }

    /**
     * 게시글 상단 고정 해제 메서드
     *
     * 상단 고정을 해제할 때 사용합니다.
     *
     * 사용 예시:
     * board.unpin();  // 고정 해제
     * boardRepository.save(board);
     */
    public void unpin() {
        this.isPinned = false;
    }

    /**
     * 게시글 논리적 삭제 메서드
     *
     * 실제 데이터베이스에서 삭제하지 않고 삭제 플래그만 설정합니다.
     *
     * 사용 예시:
     * board.softDelete();  // 논리적 삭제
     * boardRepository.save(board);
     *
     * 삭제된 게시글:
     * - 목록에 표시 안 됨
     * - 검색 결과에 포함 안 됨
     * - 관리자는 복구 가능
     */
    public void softDelete() {
        this.isDeleted = true;
        // BaseEntity의 softDelete()도 함께 호출 (deletedAt 설정)
        super.softDelete();
    }

    /**
     * 게시글 복구 메서드
     *
     * 삭제된 게시글을 복구합니다.
     *
     * 사용 예시:
     * board.restore();  // 복구
     * boardRepository.save(board);
     */
    public void restore() {
        this.isDeleted = false;
        // BaseEntity의 restore()도 함께 호출
        super.restore();
    }

    /**
     * 게시글이 삭제된 상태인지 확인하는 메서드
     *
     * @return true: 삭제됨, false: 정상
     *
     * 사용 예시:
     * if (board.isDeleted()) {
     *     throw new IllegalStateException("삭제된 게시글입니다.");
     * }
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }

    /**
     * 게시글이 상단 고정되었는지 확인하는 메서드
     *
     * @return true: 고정됨, false: 일반
     *
     * 사용 예시:
     * if (board.isPinned()) {
     *     // 상단 고정 게시글 스타일 적용
     * }
     */
    public boolean isPinned() {
        return Boolean.TRUE.equals(this.isPinned);
    }

    /**
     * 특정 사용자가 작성한 게시글인지 확인하는 메서드
     *
     * @param userId 확인할 사용자 ID
     * @return true: 해당 사용자가 작성함, false: 다른 사용자가 작성함
     *
     * 사용 예시:
     * if (!board.isAuthor(currentUserId)) {
     *     throw new UnauthorizedException("본인이 작성한 게시글만 수정/삭제할 수 있습니다.");
     * }
     */
    public boolean isAuthor(Long userId) {
        return this.author != null &&
                this.author.getUserId() != null &&
                this.author.getUserId().equals(userId);
    }

    /**
     * 게시글 정보 업데이트 메서드
     *
     * 게시글 수정 시 제목, 내용, 카테고리를 한 번에 업데이트합니다.
     *
     * @param title 새 제목
     * @param content 새 내용
     * @param category 새 카테고리
     *
     * 사용 예시:
     * board.update("수정된 제목", "수정된 내용", "FREE");
     * boardRepository.save(board);
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
     *
     * @return 작성자 이름 (작성자가 없으면 "알 수 없음")
     */
    public String getAuthorName() {
        if (this.author != null && this.author.getFullName() != null) {
            return this.author.getFullName();
        }
        return "알 수 없음";
    }

    /**
     * 댓글 수 조회 편의 메서드
     *
     * @return 댓글 수 (현재는 0 반환, 향후 실제 댓글 수로 개선 가능)
     */
    public Integer getCommentCount() {
        // 현재는 기본값 0 반환
        // 향후 Comment 엔티티와 연관관계가 맺어지면 실제 댓글 수 반환
        return 0;
    }
}

/*
 * ====== 향후 추가할 수 있는 기능들 ======
 *
 * 1. 댓글 관계:
 *    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
 *    private List<Comment> comments = new ArrayList<>();
 *
 * 2. 첨부파일 관계:
 *    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
 *    private List<File> attachments = new ArrayList<>();
 *
 * 3. 추천/좋아요:
 *    @Column(name = "like_count")
 *    private Integer likeCount = 0;
 *
 * 4. 태그:
 *    @ElementCollection
 *    private List<String> tags = new ArrayList<>();
 *
 * 5. 게시글 상태:
 *    @Enumerated(EnumType.STRING)
 *    private BoardStatus status; // DRAFT, PUBLISHED, ARCHIVED
 */