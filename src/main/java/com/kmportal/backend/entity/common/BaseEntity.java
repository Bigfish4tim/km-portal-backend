package com.kmportal.backend.entity.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티에서 상속받아 사용할 기본 엔티티 클래스
 *
 * 이 클래스가 제공하는 기능들:
 * 1. 자동으로 생성일/수정일 관리 (JPA Auditing 기능 사용)
 * 2. 생성자/수정자 정보 추적 (향후 사용자 인증 연동시 활용)
 * 3. 논리적 삭제 기능 (실제 DB에서 삭제하지 않고 플래그로 관리)
 * 4. 모든 테이블에 공통으로 필요한 메타데이터 필드들 제공
 *
 * 사용법:
 * - 다른 엔티티 클래스에서 이 클래스를 상속받으면 됨
 * - 예: public class User extends BaseEntity { ... }
 *
 * 주의사항:
 * - JPA Auditing을 활성화하기 위해 @EnableJpaAuditing을 main 클래스에 추가해야 함
 * - AuditorAware를 구현하여 생성자/수정자 정보를 자동으로 채우도록 설정 가능
 */
@Getter
@MappedSuperclass  // 이 어노테이션으로 상속받는 엔티티들이 이 클래스의 필드를 포함하게 됨
@EntityListeners(AuditingEntityListener.class)  // JPA Auditing 기능을 활성화
public abstract class BaseEntity {

    /**
     * 엔티티 생성일시
     *
     * @CreatedDate: Spring Data JPA가 엔티티가 처음 저장될 때 자동으로 현재 시간을 설정
     * updatable = false: 한번 설정된 후에는 수정되지 않도록 함
     *
     * 사용 예시:
     * - 게시글이 언제 작성되었는지 확인
     * - 사용자가 언제 가입했는지 확인
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티 최종 수정일시
     *
     * @LastModifiedDate: Spring Data JPA가 엔티티가 수정될 때마다 자동으로 현재 시간으로 업데이트
     *
     * 사용 예시:
     * - 게시글이 마지막으로 수정된 시간 확인
     * - 사용자 정보가 언제 업데이트되었는지 확인
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 엔티티 생성자 정보
     *
     * @CreatedBy: Spring Data JPA가 엔티티 생성시 현재 인증된 사용자 정보를 자동으로 설정
     * 현재는 String으로 설정했지만, 향후 사용자 인증 시스템 구축 후 User 객체로 변경 가능
     *
     * 사용 예시:
     * - 게시글을 누가 작성했는지 추적
     * - 파일을 누가 업로드했는지 확인
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /**
     * 엔티티 최종 수정자 정보
     *
     * @LastModifiedBy: Spring Data JPA가 엔티티 수정시 현재 인증된 사용자 정보를 자동으로 설정
     *
     * 사용 예시:
     * - 게시글을 마지막으로 누가 수정했는지 확인
     * - 사용자 정보를 누가 변경했는지 추적
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * 논리적 삭제 여부
     *
     * false: 활성 상태 (기본값)
     * true: 삭제된 상태
     *
     * 논리적 삭제의 장점:
     * 1. 데이터 복구 가능
     * 2. 참조 무결성 유지
     * 3. 감사 목적으로 데이터 보존
     * 4. 삭제된 데이터에 대한 통계 분석 가능
     *
     * 사용 예시:
     * - 사용자가 탈퇴했지만 작성한 게시글은 보존
     * - 실수로 삭제한 데이터 복구
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 실제 삭제일시
     *
     * 논리적 삭제가 수행된 시점을 기록
     * isDeleted가 true로 변경된 시점의 시간이 저장됨
     *
     * 사용 예시:
     * - 30일 후 물리적 삭제를 위한 배치 작업에서 활용
     * - 삭제 데이터에 대한 보존 기간 계산
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ====== 비즈니스 메서드들 ======

    /**
     * 논리적 삭제를 수행하는 메서드
     *
     * 실제 데이터베이스에서 삭제하지 않고 삭제 플래그만 설정
     * 삭제 시점도 함께 기록
     *
     * 사용 예시:
     * user.softDelete();  // 사용자를 논리적으로 삭제
     * boardRepository.save(user);  // 변경 사항을 DB에 반영
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 논리적 삭제를 취소하는 메서드
     *
     * 삭제 플래그를 false로 되돌리고 삭제 시점 정보를 제거
     *
     * 사용 예시:
     * user.restore();  // 삭제된 사용자를 복구
     * boardRepository.save(user);  // 변경 사항을 DB에 반영
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    /**
     * 엔티티가 삭제된 상태인지 확인하는 메서드
     *
     * @return true: 삭제된 상태, false: 활성 상태
     *
     * 사용 예시:
     * if (user.isDeleted()) {
     *     throw new IllegalStateException("삭제된 사용자입니다.");
     * }
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }

    /**
     * 엔티티가 새로 생성된 것인지 확인하는 메서드
     *
     * @return true: 새로운 엔티티 (DB에 저장된 적 없음), false: 기존 엔티티
     *
     * 사용 예시:
     * if (user.isNew()) {
     *     // 신규 사용자 처리 로직
     * }
     */
    public boolean isNew() {
        return this.createdAt == null;
    }

    /**
     * 엔티티가 수정된 적이 있는지 확인하는 메서드
     *
     * @return true: 수정됨, false: 수정되지 않음
     *
     * 사용 예시:
     * if (user.isModified()) {
     *     // 수정된 사용자에 대한 추가 처리
     * }
     */
    public boolean isModified() {
        return this.updatedAt != null &&
                this.createdAt != null &&
                !this.updatedAt.equals(this.createdAt);
    }
}

/*
 * ====== 향후 추가할 수 있는 기능들 ======
 *
 * 1. 버전 관리 (Optimistic Locking):
 *    @Version
 *    private Long version;
 *
 * 2. 테넌트 지원 (Multi-tenancy):
 *    @Column(name = "tenant_id")
 *    private String tenantId;
 *
 * 3. 정렬 순서:
 *    @Column(name = "sort_order")
 *    private Integer sortOrder;
 *
 * 4. 활성/비활성 상태:
 *    @Column(name = "is_active")
 *    private Boolean isActive = true;
 */