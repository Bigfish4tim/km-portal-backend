package com.kmportal.backend.service;

import com.kmportal.backend.entity.Board;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.BoardRepository;
import com.kmportal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BoardService
 *
 * 게시판 시스템의 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * Controller와 Repository 사이에서 실제 업무 처리를 담당합니다.
 *
 * 서비스 레이어의 역할:
 * 1. 비즈니스 로직 처리 (권한 확인, 유효성 검사 등)
 * 2. 트랜잭션 관리 (@Transactional)
 * 3. 여러 Repository 조합하여 복잡한 기능 구현
 * 4. 엔티티와 DTO 간 변환
 * 5. 예외 처리 및 에러 메시지 생성
 *
 * 주요 기능:
 * 1. 게시글 CRUD (Create, Read, Update, Delete)
 * 2. 게시글 검색 및 필터링
 * 3. 상단 고정 관리
 * 4. 조회수 증가
 * 5. 통계 정보 제공
 * 6. 권한 기반 접근 제어
 *
 * 작성일: 2025년 11월 16일 (24일차)
 * 작성자: 24일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-16
 */
@Service  // Spring이 이 클래스를 서비스 빈으로 관리
@RequiredArgsConstructor  // Lombok: final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Slf4j  // Lombok: 로그 객체 자동 생성 (log.info(), log.error() 등 사용 가능)
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 트랜잭션 (성능 최적화)
public class BoardService {

    // ====== 의존성 주입 ======

    /**
     * BoardRepository: 게시글 데이터 액세스
     *
     * final 키워드:
     * - 한번 주입되면 변경 불가
     * - @RequiredArgsConstructor가 생성자를 통해 주입
     * - 스프링이 자동으로 BoardRepository 빈을 찾아서 주입
     */
    private final BoardRepository boardRepository;

    /**
     * UserRepository: 사용자 데이터 액세스
     *
     * 게시글 작성 시 작성자 정보를 조회하기 위해 필요합니다.
     */
    private final UserRepository userRepository;

    // ================================
    // 게시글 생성 (Create)
    // ================================

    /**
     * 게시글 생성 메서드
     *
     * 새로운 게시글을 작성합니다.
     * 현재 로그인한 사용자가 작성자로 설정됩니다.
     *
     * @Transactional: 쓰기 트랜잭션 활성화
     * - readOnly = false가 기본값이므로 별도 지정 안 함
     * - 메서드 실행 중 예외 발생 시 자동 롤백
     * - 메서드 종료 시 자동 커밋
     *
     * @param title 게시글 제목
     * @param content 게시글 내용 (HTML 포함 가능)
     * @param category 카테고리
     * @return 생성된 게시글
     * @throws IllegalArgumentException 제목이나 내용이 비어있을 때
     * @throws RuntimeException 현재 사용자를 찾을 수 없을 때
     *
     * 사용 예시 (Controller에서):
     * Board board = boardService.createBoard("제목", "내용", "FREE");
     */
    @Transactional  // 쓰기 작업이므로 readOnly = false (기본값)
    public Board createBoard(String title, String content, String category) {
        // 로그 기록: 누가 어떤 게시글을 작성하려고 하는지
        log.info("게시글 생성 시작 - 제목: {}, 카테고리: {}", title, category);

        // 1. 입력값 검증
        if (title == null || title.trim().isEmpty()) {
            log.error("게시글 생성 실패 - 제목이 비어있음");
            throw new IllegalArgumentException("게시글 제목은 필수 입력 항목입니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            log.error("게시글 생성 실패 - 내용이 비어있음");
            throw new IllegalArgumentException("게시글 내용은 필수 입력 항목입니다.");
        }

        // 2. 현재 로그인한 사용자 조회
        User currentUser = getCurrentUser();
        log.info("현재 사용자: {} (ID: {})", currentUser.getUsername(), currentUser.getUserId());

        // 3. Board 엔티티 생성 (Builder 패턴 사용)
        Board board = Board.builder()
                .title(title.trim())      // 앞뒤 공백 제거
                .content(content.trim())  // 앞뒤 공백 제거
                .category(category)
                .author(currentUser)
                .viewCount(0)             // 초기 조회수 0
                .isPinned(false)          // 기본값: 상단 고정 안 함
                .isDeleted(false)         // 기본값: 삭제 안 됨
                .build();

        // 4. 데이터베이스에 저장
        Board savedBoard = boardRepository.save(board);

        log.info("게시글 생성 완료 - ID: {}, 제목: {}", savedBoard.getId(), savedBoard.getTitle());

        return savedBoard;
    }

    // ================================
    // 게시글 조회 (Read)
    // ================================

    /**
     * 게시글 상세 조회 메서드
     *
     * 게시글 ID로 상세 정보를 조회하고, 조회수를 1 증가시킵니다.
     *
     * 참고: 조회수 중복 증가 방지 로직은 향후 추가 가능
     * - 쿠키나 세션에 조회한 게시글 ID 저장
     * - 일정 시간 내 재조회 시 조회수 증가 안 함
     *
     * @param id 게시글 ID
     * @return 게시글 정보
     * @throws RuntimeException 게시글이 없거나 삭제되었을 때
     */
    @Transactional  // 조회수 증가(쓰기)가 포함되므로 쓰기 트랜잭션 필요
    public Board getBoardById(Long id) {
        log.info("게시글 조회 시작 - ID: {}", id);

        // 1. 게시글 조회 (삭제되지 않은 게시글만)
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("게시글 조회 실패 - 존재하지 않는 ID: {}", id);
                    return new RuntimeException("게시글을 찾을 수 없습니다.");
                });

        // 2. 조회수 증가
        boardRepository.increaseViewCount(id);
        log.info("게시글 조회 완료 - ID: {}, 제목: {}, 조회수: {} -> {}",
                id, board.getTitle(), board.getViewCount(), board.getViewCount() + 1);

        // 3. 조회수가 증가된 게시글 반환 (영속성 컨텍스트에서 자동 반영됨)
        board.increaseViewCount();  // 엔티티 객체의 조회수도 동기화
        return board;
    }

    /**
     * 게시글 목록 조회 메서드 (페이징)
     *
     * 삭제되지 않은 모든 게시글을 페이징하여 조회합니다.
     *
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 게시글 목록 (페이징)
     *
     * 사용 예시 (Controller에서):
     * Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
     * Page<Board> boards = boardService.getBoardList(pageable);
     */
    public Page<Board> getBoardList(Pageable pageable) {
        log.info("게시글 목록 조회 - 페이지: {}, 크기: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Board> boards = boardRepository.findByIsDeletedFalse(pageable);

        log.info("게시글 목록 조회 완료 - 총 {}개, 현재 페이지 {}개",
                boards.getTotalElements(), boards.getNumberOfElements());

        return boards;
    }

    /**
     * 카테고리별 게시글 목록 조회
     *
     * @param category 카테고리
     * @param pageable 페이징 정보
     * @return 해당 카테고리의 게시글 목록
     */
    public Page<Board> getBoardsByCategory(String category, Pageable pageable) {
        log.info("카테고리별 게시글 조회 - 카테고리: {}, 페이지: {}",
                category, pageable.getPageNumber());

        Page<Board> boards = boardRepository.findByCategoryAndIsDeletedFalse(category, pageable);

        log.info("카테고리별 게시글 조회 완료 - 카테고리: {}, 총 {}개",
                category, boards.getTotalElements());

        return boards;
    }

    /**
     * 작성자별 게시글 목록 조회
     *
     * @param authorId 작성자 ID
     * @param pageable 페이징 정보
     * @return 해당 작성자의 게시글 목록
     */
    public Page<Board> getBoardsByAuthor(Long authorId, Pageable pageable) {
        log.info("작성자별 게시글 조회 - 작성자 ID: {}", authorId);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Page<Board> boards = boardRepository.findByAuthorAndIsDeletedFalse(author, pageable);

        log.info("작성자별 게시글 조회 완료 - 작성자: {}, 총 {}개",
                author.getUsername(), boards.getTotalElements());

        return boards;
    }

    /**
     * 상단 고정 게시글 조회
     *
     * @return 상단 고정 게시글 목록 (최신순)
     */
    public List<Board> getPinnedBoards() {
        log.info("상단 고정 게시글 조회");

        List<Board> pinnedBoards = boardRepository
                .findByIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc();

        log.info("상단 고정 게시글 조회 완료 - 총 {}개", pinnedBoards.size());

        return pinnedBoards;
    }

    // ================================
    // 게시글 검색
    // ================================

    /**
     * 게시글 복합 검색 메서드
     *
     * 제목, 내용, 작성자 이름으로 검색하고,
     * 카테고리와 작성자 ID로 필터링할 수 있습니다.
     *
     * @param keyword 검색어 (제목, 내용, 작성자 이름)
     * @param category 카테고리 필터 (null이면 전체)
     * @param authorId 작성자 ID 필터 (null이면 전체)
     * @param pageable 페이징 정보
     * @return 검색 결과 (페이징)
     *
     * 사용 예시:
     * // 제목/내용에 "spring"이 포함된 모든 게시글
     * Page<Board> results = boardService.searchBoards("spring", null, null, pageable);
     *
     * // "NOTICE" 카테고리에서 "공지" 검색
     * Page<Board> notices = boardService.searchBoards("공지", "NOTICE", null, pageable);
     */
    public Page<Board> searchBoards(String keyword, String category, Long authorId, Pageable pageable) {
        log.info("게시글 검색 - 검색어: {}, 카테고리: {}, 작성자 ID: {}",
                keyword, category, authorId);

        Page<Board> results = boardRepository.searchBoards(keyword, category, authorId, pageable);

        log.info("게시글 검색 완료 - 검색어: {}, 결과: {}개", keyword, results.getTotalElements());

        return results;
    }

    /**
     * 인기 게시글 조회 (조회수 높은 순)
     *
     * @param pageable 페이징 정보 (주로 상위 10개)
     * @return 인기 게시글 목록
     */
    public Page<Board> getPopularBoards(Pageable pageable) {
        log.info("인기 게시글 조회 - 상위 {}개", pageable.getPageSize());

        Page<Board> popularBoards = boardRepository
                .findByIsDeletedFalseOrderByViewCountDesc(pageable);

        log.info("인기 게시글 조회 완료 - {}개", popularBoards.getNumberOfElements());

        return popularBoards;
    }

    /**
     * 최근 게시글 조회 (작성일 최신순)
     *
     * @param pageable 페이징 정보
     * @return 최근 게시글 목록
     */
    public Page<Board> getRecentBoards(Pageable pageable) {
        log.info("최근 게시글 조회 - 상위 {}개", pageable.getPageSize());

        Page<Board> recentBoards = boardRepository
                .findByIsDeletedFalseOrderByCreatedAtDesc(pageable);

        log.info("최근 게시글 조회 완료 - {}개", recentBoards.getNumberOfElements());

        return recentBoards;
    }

    // ================================
    // 게시글 수정 (Update)
    // ================================

    /**
     * 게시글 수정 메서드
     *
     * 게시글의 제목, 내용, 카테고리를 수정합니다.
     * 본인이 작성한 게시글만 수정할 수 있습니다.
     *
     * @param id 게시글 ID
     * @param title 새 제목
     * @param content 새 내용
     * @param category 새 카테고리
     * @return 수정된 게시글
     * @throws RuntimeException 게시글이 없거나, 권한이 없을 때
     */
    @Transactional
    public Board updateBoard(Long id, String title, String content, String category) {
        log.info("게시글 수정 시작 - ID: {}", id);

        // 1. 게시글 조회
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("게시글 수정 실패 - 존재하지 않는 ID: {}", id);
                    return new RuntimeException("게시글을 찾을 수 없습니다.");
                });

        // 2. 권한 확인 (본인 또는 관리자만 수정 가능)
        User currentUser = getCurrentUser();
        if (!board.isAuthor(currentUser.getUserId()) && !isAdmin(currentUser)) {
            log.error("게시글 수정 실패 - 권한 없음. 사용자: {}, 게시글 작성자: {}",
                    currentUser.getUsername(), board.getAuthor().getUsername());
            throw new RuntimeException("게시글을 수정할 권한이 없습니다.");
        }

        // 3. 게시글 정보 업데이트
        board.update(title, content, category);
        Board updatedBoard = boardRepository.save(board);

        log.info("게시글 수정 완료 - ID: {}, 제목: {}", id, updatedBoard.getTitle());

        return updatedBoard;
    }

    // ================================
    // 게시글 삭제 (Delete)
    // ================================

    /**
     * 게시글 삭제 메서드 (Soft Delete)
     *
     * 실제 데이터베이스에서 삭제하지 않고 isDeleted 플래그만 변경합니다.
     * 본인이 작성한 게시글만 삭제할 수 있습니다.
     *
     * @param id 게시글 ID
     * @throws RuntimeException 게시글이 없거나, 권한이 없을 때
     */
    @Transactional
    public void deleteBoard(Long id) {
        log.info("게시글 삭제 시작 - ID: {}", id);

        // 1. 게시글 조회
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("게시글 삭제 실패 - 존재하지 않는 ID: {}", id);
                    return new RuntimeException("게시글을 찾을 수 없습니다.");
                });

        // 2. 권한 확인 (본인 또는 관리자만 삭제 가능)
        User currentUser = getCurrentUser();
        if (!board.isAuthor(currentUser.getUserId()) && !isAdmin(currentUser)) {
            log.error("게시글 삭제 실패 - 권한 없음. 사용자: {}, 게시글 작성자: {}",
                    currentUser.getUsername(), board.getAuthor().getUsername());
            throw new RuntimeException("게시글을 삭제할 권한이 없습니다.");
        }

        // 3. 논리적 삭제 (Soft Delete)
        board.softDelete();
        boardRepository.save(board);

        log.info("게시글 삭제 완료 - ID: {}, 제목: {}", id, board.getTitle());
    }

    // ================================
    // 상단 고정 관리
    // ================================

    /**
     * 게시글 상단 고정 메서드
     *
     * 관리자만 게시글을 상단에 고정할 수 있습니다.
     *
     * @param id 게시글 ID
     * @throws RuntimeException 게시글이 없거나, 권한이 없을 때
     */
    @Transactional
    public void pinBoard(Long id) {
        log.info("게시글 상단 고정 시작 - ID: {}", id);

        // 1. 권한 확인 (관리자만 가능)
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser)) {
            log.error("게시글 상단 고정 실패 - 권한 없음. 사용자: {}", currentUser.getUsername());
            throw new RuntimeException("게시글을 상단 고정할 권한이 없습니다. (관리자만 가능)");
        }

        // 2. 게시글 조회
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("게시글 상단 고정 실패 - 존재하지 않는 ID: {}", id);
                    return new RuntimeException("게시글을 찾을 수 없습니다.");
                });

        // 3. 상단 고정
        board.pin();
        boardRepository.save(board);

        log.info("게시글 상단 고정 완료 - ID: {}, 제목: {}", id, board.getTitle());
    }

    /**
     * 게시글 상단 고정 해제 메서드
     *
     * @param id 게시글 ID
     */
    @Transactional
    public void unpinBoard(Long id) {
        log.info("게시글 상단 고정 해제 시작 - ID: {}", id);

        // 1. 권한 확인 (관리자만 가능)
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser)) {
            log.error("게시글 상단 고정 해제 실패 - 권한 없음. 사용자: {}", currentUser.getUsername());
            throw new RuntimeException("게시글 상단 고정을 해제할 권한이 없습니다. (관리자만 가능)");
        }

        // 2. 게시글 조회
        Board board = boardRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("게시글 상단 고정 해제 실패 - 존재하지 않는 ID: {}", id);
                    return new RuntimeException("게시글을 찾을 수 없습니다.");
                });

        // 3. 고정 해제
        board.unpin();
        boardRepository.save(board);

        log.info("게시글 상단 고정 해제 완료 - ID: {}, 제목: {}", id, board.getTitle());
    }

    // ================================
    // 통계 정보
    // ================================

    /**
     * 게시판 통계 정보 조회 메서드
     *
     * 관리자 대시보드나 통계 화면에서 사용할 데이터를 제공합니다.
     *
     * @return 통계 정보 Map
     * - totalBoards: 전체 게시글 수
     * - categoryStats: 카테고리별 게시글 수
     * - todayBoards: 오늘 작성된 게시글 수
     * - weekBoards: 최근 7일간 작성된 게시글 수
     *
     * 사용 예시 (Controller에서):
     * Map<String, Object> stats = boardService.getBoardStatistics();
     * Long total = (Long) stats.get("totalBoards");
     */
    public Map<String, Object> getBoardStatistics() {
        log.info("게시판 통계 정보 조회 시작");

        Map<String, Object> statistics = new HashMap<>();

        // 1. 전체 게시글 수
        Long totalBoards = boardRepository.countByIsDeletedFalse();
        statistics.put("totalBoards", totalBoards);

        // 2. 카테고리별 게시글 수
        List<Object[]> categoryStats = boardRepository.countByCategory();
        Map<String, Long> categoryMap = new HashMap<>();
        for (Object[] stat : categoryStats) {
            String category = (String) stat[0];
            Long count = (Long) stat[1];
            categoryMap.put(category != null ? category : "미분류", count);
        }
        statistics.put("categoryStats", categoryMap);

        // 3. 오늘 작성된 게시글 수
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        Long todayBoards = boardRepository.findBoardsCreatedBetween(startOfDay, endOfDay, Pageable.unpaged())
                .getTotalElements();
        statistics.put("todayBoards", todayBoards);

        // 4. 최근 7일간 작성된 게시글 수
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();
        Long weekBoards = boardRepository.findBoardsCreatedBetween(sevenDaysAgo, now, Pageable.unpaged())
                .getTotalElements();
        statistics.put("weekBoards", weekBoards);

        log.info("게시판 통계 정보 조회 완료 - 전체: {}개, 오늘: {}개, 이번 주: {}개",
                totalBoards, todayBoards, weekBoards);

        return statistics;
    }

    // ================================
    // 유틸리티 메서드들
    // ================================

    /**
     * 현재 로그인한 사용자 조회
     *
     * Spring Security의 SecurityContext에서 현재 인증된 사용자 정보를 가져옵니다.
     *
     * @return 현재 사용자
     * @throws RuntimeException 인증되지 않았거나 사용자를 찾을 수 없을 때
     */
    private User getCurrentUser() {
        // 1. SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("현재 사용자 조회 실패 - 인증되지 않음");
            throw new RuntimeException("로그인이 필요합니다.");
        }

        // 2. 인증 정보에서 사용자명 추출
        String username = authentication.getName();

        if (username == null || username.equals("anonymousUser")) {
            log.error("현재 사용자 조회 실패 - 익명 사용자");
            throw new RuntimeException("로그인이 필요합니다.");
        }

        // 3. 사용자명으로 User 엔티티 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("현재 사용자 조회 실패 - 사용자를 찾을 수 없음: {}", username);
                    return new RuntimeException("사용자를 찾을 수 없습니다.");
                });

        return user;
    }

    /**
     * 관리자 권한 확인
     *
     * 사용자가 ROLE_ADMIN 역할을 가지고 있는지 확인합니다.
     *
     * @param user 확인할 사용자
     * @return 관리자이면 true, 아니면 false
     */
    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("ROLE_ADMIN"));
    }

    /**
     * 관리자 또는 게시판 관리자 권한 확인
     *
     * @param user 확인할 사용자
     * @return 관리자 권한이 있으면 true
     */
    private boolean isBoardAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role ->
                        role.getRoleName().equals("ROLE_ADMIN") ||
                                role.getRoleName().equals("ROLE_BOARD_ADMIN")
                );
    }
}

/*
 * ====== 서비스 레이어 설계 원칙 ======
 *
 * 1. 단일 책임 원칙 (Single Responsibility Principle):
 *    - 각 메서드는 하나의 명확한 기능만 수행
 *    - 복잡한 로직은 여러 private 메서드로 분리
 *
 * 2. 트랜잭션 관리:
 *    - 읽기 작업: @Transactional(readOnly = true) (성능 최적화)
 *    - 쓰기 작업: @Transactional (롤백 가능)
 *
 * 3. 예외 처리:
 *    - 비즈니스 예외는 RuntimeException 또는 커스텀 예외 사용
 *    - 로그와 함께 명확한 에러 메시지 제공
 *
 * 4. 로깅:
 *    - 중요한 작업의 시작과 완료를 로그로 기록
 *    - 에러 발생 시 상세 정보 로그
 *
 * 5. 권한 확인:
 *    - 민감한 작업 전에 항상 권한 확인
 *    - 본인 또는 관리자만 수정/삭제 가능하도록
 *
 * 6. DTO 변환:
 *    - 엔티티를 직접 반환하지 말고 DTO로 변환 (향후 추가)
 *    - 민감한 정보 노출 방지
 *    - 순환 참조 방지
 */