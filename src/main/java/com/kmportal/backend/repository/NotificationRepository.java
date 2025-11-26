package com.kmportal.backend.repository;

import com.kmportal.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * NotificationRepository - 알림 데이터 접근 레이어
 *
 * Spring Data JPA를 사용하여 알림 엔티티에 대한 CRUD 및
 * 다양한 조회 기능을 제공하는 Repository 인터페이스입니다.
 *
 * 주요 기능:
 * 1. 사용자별 알림 목록 조회 (페이징 지원)
 * 2. 읽지 않은 알림 개수 조회
 * 3. 최근 알림 조회 (드롭다운용)
 * 4. 알림 일괄 읽음 처리
 * 5. 오래된 알림 삭제
 *
 * 작성일: 2025년 11월 26일 (34일차)
 * 작성자: 34일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-26
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ====== 기본 조회 메서드 ======

    /**
     * 사용자 ID로 알림 목록 조회 (최신순 정렬, 페이징)
     *
     * 특정 사용자에게 전달된 모든 알림을 최신순으로 조회합니다.
     * 삭제되지 않은 알림만 조회합니다.
     *
     * @param recipientId 수신자(사용자) ID
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기)
     * @return 알림 목록 (페이징 적용)
     *
     * 사용 예시:
     * Pageable pageable = PageRequest.of(0, 10);  // 첫 페이지, 10개
     * Page<Notification> notifications = notificationRepository
     *     .findByRecipientUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
     */
    @Query("SELECT n FROM Notification n " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.isDeleted = false " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(
            @Param("recipientId") Long recipientId,
            Pageable pageable);

    /**
     * 사용자 ID로 알림 목록 조회 (간단한 버전)
     *
     * JPA 메서드명 규칙을 사용한 간단한 조회입니다.
     *
     * @param userId 수신자 ID
     * @param pageable 페이징 정보
     * @return 알림 목록 (페이징 적용)
     */
    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // ====== 읽지 않은 알림 관련 ======

    /**
     * 읽지 않은 알림 개수 조회
     *
     * 특정 사용자의 읽지 않은 알림 개수를 반환합니다.
     * 헤더의 알림 뱃지에 표시되는 숫자입니다.
     *
     * @param recipientId 수신자(사용자) ID
     * @return 읽지 않은 알림 개수
     *
     * 사용 예시:
     * long unreadCount = notificationRepository.countUnreadByRecipientId(userId);
     * // 결과: 5 (읽지 않은 알림이 5개)
     */
    @Query("SELECT COUNT(n) FROM Notification n " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.isRead = false " +
            "AND n.isDeleted = false")
    long countUnreadByRecipientId(@Param("recipientId") Long recipientId);

    /**
     * JPA 메서드명 규칙을 사용한 읽지 않은 알림 개수 조회
     *
     * User.java의 필드명이 'userId'이므로 메서드명도 그에 맞춰야 합니다.
     * JPA는 recipient.userId로 자동 매핑합니다.
     *
     * @param userId 수신자 ID
     * @return 읽지 않은 알림 개수
     */
    long countByRecipientUserIdAndIsReadFalse(Long userId);

    /**
     * 읽지 않은 알림 목록 조회 (최신순)
     *
     * 특정 사용자의 읽지 않은 알림만 최신순으로 조회합니다.
     *
     * @param recipientId 수신자 ID
     * @return 읽지 않은 알림 목록
     */
    @Query("SELECT n FROM Notification n " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.isRead = false " +
            "AND n.isDeleted = false " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipientId(@Param("recipientId") Long recipientId);

    // ====== 최근 알림 조회 (드롭다운용) ======

    /**
     * 최근 알림 N개 조회
     *
     * 헤더 드롭다운에 표시할 최근 알림을 조회합니다.
     *
     * @param recipientId 수신자 ID
     * @param pageable 페이징 정보 (예: PageRequest.of(0, 5))
     * @return 최근 알림 목록
     *
     * 사용 예시:
     * List<Notification> recent = notificationRepository
     *     .findRecentByRecipientId(userId, PageRequest.of(0, 5));
     * // 결과: 최신 알림 5개
     */
    @Query("SELECT n FROM Notification n " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.isDeleted = false " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findRecentByRecipientId(
            @Param("recipientId") Long recipientId,
            Pageable pageable);

    /**
     * 최근 5개 알림 조회 (메서드명 규칙)
     *
     * JPA의 findTop5By... 규칙을 사용한 조회입니다.
     *
     * @param recipientId 수신자 ID
     * @return 최근 알림 5개
     */
    List<Notification> findTop5ByRecipientUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long recipientId);

    // ====== 알림 유형별 조회 ======

    /**
     * 알림 유형별 목록 조회
     *
     * 특정 유형의 알림만 필터링하여 조회합니다.
     *
     * @param recipientId 수신자 ID
     * @param type 알림 유형 (예: "NEW_COMMENT", "SYSTEM")
     * @param pageable 페이징 정보
     * @return 해당 유형의 알림 목록
     *
     * 사용 예시:
     * Page<Notification> comments = notificationRepository
     *     .findByRecipientIdAndType(userId, "NEW_COMMENT", pageable);
     */
    @Query("SELECT n FROM Notification n " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.type = :type " +
            "AND n.isDeleted = false " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndType(
            @Param("recipientId") Long recipientId,
            @Param("type") String type,
            Pageable pageable);

    // ====== 읽음/안읽음 필터링 ======

    /**
     * 읽음 여부로 필터링하여 알림 조회
     *
     * @param recipientId 수신자 ID
     * @param isRead 읽음 여부 (true: 읽은 알림, false: 읽지 않은 알림)
     * @param pageable 페이징 정보
     * @return 필터링된 알림 목록
     */
    @Query("SELECT n FROM Notification n " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.isRead = :isRead " +
            "AND n.isDeleted = false " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndIsRead(
            @Param("recipientId") Long recipientId,
            @Param("isRead") Boolean isRead,
            Pageable pageable);

    // ====== 일괄 처리 메서드 ======

    /**
     * 특정 사용자의 모든 알림 읽음 처리
     *
     * UPDATE 쿼리로 읽지 않은 모든 알림을 읽음 상태로 변경합니다.
     *
     * @param recipientId 수신자 ID
     * @param readAt 읽은 시간
     * @return 업데이트된 알림 개수
     *
     * 주의: @Modifying 어노테이션과 함께 트랜잭션 내에서 실행해야 합니다.
     *
     * 사용 예시:
     * int count = notificationRepository.markAllAsReadByRecipientId(userId, LocalDateTime.now());
     * // 결과: 10 (10개의 알림이 읽음 처리됨)
     */
    @Modifying
    @Query("UPDATE Notification n " +
            "SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.isRead = false " +
            "AND n.isDeleted = false")
    int markAllAsReadByRecipientId(
            @Param("recipientId") Long recipientId,
            @Param("readAt") LocalDateTime readAt);

    /**
     * 특정 기간 이전의 알림 삭제 (Soft Delete)
     *
     * 지정된 날짜 이전에 생성된 알림을 논리적으로 삭제합니다.
     * 배치 작업에서 오래된 알림을 정리할 때 사용합니다.
     *
     * @param date 기준 날짜
     * @return 삭제된 알림 개수
     *
     * 사용 예시:
     * // 30일 이전의 알림 삭제
     * LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
     * int deleted = notificationRepository.softDeleteOlderThan(thirtyDaysAgo);
     */
    @Modifying
    @Query("UPDATE Notification n " +
            "SET n.isDeleted = true, n.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE n.createdAt < :date " +
            "AND n.isDeleted = false")
    int softDeleteOlderThan(@Param("date") LocalDateTime date);

    /**
     * 특정 기간 이전의 읽은 알림만 삭제 (Soft Delete)
     *
     * 읽은 알림 중 오래된 것만 삭제합니다.
     * 읽지 않은 중요 알림은 보존됩니다.
     *
     * @param date 기준 날짜
     * @return 삭제된 알림 개수
     */
    @Modifying
    @Query("UPDATE Notification n " +
            "SET n.isDeleted = true, n.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE n.createdAt < :date " +
            "AND n.isRead = true " +
            "AND n.isDeleted = false")
    int softDeleteReadNotificationsOlderThan(@Param("date") LocalDateTime date);

    // ====== 관련 엔티티 기반 조회 ======

    /**
     * 특정 게시글과 관련된 알림 조회
     *
     * 게시글이 삭제될 때 관련 알림을 함께 처리하기 위해 사용합니다.
     *
     * @param boardId 게시글 ID
     * @return 관련 알림 목록
     */
    @Query("SELECT n FROM Notification n " +
            "WHERE n.relatedBoardId = :boardId " +
            "AND n.isDeleted = false")
    List<Notification> findByRelatedBoardId(@Param("boardId") Long boardId);

    /**
     * 특정 댓글과 관련된 알림 조회
     *
     * 댓글이 삭제될 때 관련 알림을 함께 처리하기 위해 사용합니다.
     *
     * @param commentId 댓글 ID
     * @return 관련 알림 목록
     */
    @Query("SELECT n FROM Notification n " +
            "WHERE n.relatedCommentId = :commentId " +
            "AND n.isDeleted = false")
    List<Notification> findByRelatedCommentId(@Param("commentId") Long commentId);

    // ====== 통계 메서드 ======

    /**
     * 알림 유형별 개수 통계
     *
     * 특정 사용자의 알림 유형별 개수를 집계합니다.
     * 관리자 대시보드나 통계 페이지에서 사용합니다.
     *
     * @param recipientId 수신자 ID
     * @return 유형별 개수 (Object[]: [type, count])
     */
    @Query("SELECT n.type, COUNT(n) FROM Notification n " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.isDeleted = false " +
            "GROUP BY n.type")
    List<Object[]> countByTypeForRecipient(@Param("recipientId") Long recipientId);

    /**
     * 특정 기간 동안 생성된 알림 개수
     *
     * @param recipientId 수신자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 알림 개수
     */
    @Query("SELECT COUNT(n) FROM Notification n " +
            "WHERE n.recipient.userId = :recipientId " +
            "AND n.createdAt BETWEEN :startDate AND :endDate " +
            "AND n.isDeleted = false")
    long countByRecipientIdAndDateRange(
            @Param("recipientId") Long recipientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ====== 단건 조회 메서드 ======

    /**
     * 알림 ID와 수신자 ID로 알림 조회
     *
     * 본인의 알림만 조회할 수 있도록 수신자 검증을 포함합니다.
     *
     * @param id 알림 ID
     * @param recipientId 수신자 ID
     * @return 알림 (Optional)
     */
    @Query("SELECT n FROM Notification n " +
            "WHERE n.id = :id " +
            "AND n.recipient.userId = :recipientId " +
            "AND n.isDeleted = false")
    Optional<Notification> findByIdAndRecipientId(
            @Param("id") Long id,
            @Param("recipientId") Long recipientId);
}

/*
 * ====== 사용 예시 ======
 *
 * // 1. 알림 목록 조회 (페이징)
 * Pageable pageable = PageRequest.of(0, 10);
 * Page<Notification> notifications = notificationRepository
 *     .findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
 *
 * // 2. 읽지 않은 알림 개수
 * long unreadCount = notificationRepository.countUnreadByRecipientId(userId);
 *
 * // 3. 최근 알림 5개 (드롭다운용)
 * List<Notification> recent = notificationRepository
 *     .findTop5ByRecipientUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
 *
 * // 4. 전체 읽음 처리
 * int updated = notificationRepository.markAllAsReadByRecipientId(userId, LocalDateTime.now());
 *
 * // 5. 30일 이전 알림 정리
 * LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
 * int deleted = notificationRepository.softDeleteOlderThan(thirtyDaysAgo);
 */