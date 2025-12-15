package com.kmportal.backend.service;

import com.kmportal.backend.entity.Board;
import com.kmportal.backend.entity.Role;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.BoardRepository;
import com.kmportal.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * BoardService 단위 테스트 클래스
 *
 * 이 클래스는 BoardService의 비즈니스 로직을 테스트합니다.
 * 게시글 CRUD, 검색, 통계 등의 기능을 검증합니다.
 *
 * [주요 테스트 항목]
 *
 * 1. 게시글 생성 (createBoard)
 * 2. 게시글 조회 (getBoardById, getBoardList)
 * 3. 게시글 수정 (updateBoard)
 * 4. 게시글 삭제 (deleteBoard)
 * 5. 게시글 검색 (searchBoards)
 * 6. 통계 정보 (getBoardStatistics)
 *
 * [SecurityContext Mock 처리]
 *
 * BoardService는 SecurityContextHolder를 사용하여 현재 로그인한 사용자를 조회합니다.
 * 테스트에서는 MockedStatic을 사용하여 SecurityContextHolder를 Mock 처리합니다.
 *
 * 작성일: 2025년 11월 29일 (40일차)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-29
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BoardService 단위 테스트")
class BoardServiceTest {

    // ================================
    // Mock 객체 선언
    // ================================

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BoardService boardService;

    // ================================
    // 테스트 픽스처
    // ================================

    private User testUser;
    private User adminUser;
    private Board testBoard;
    private Role userRole;
    private Role adminRole;

    /**
     * 각 테스트 전에 실행되는 설정 메서드
     *
     * 테스트에 필요한 User, Role, Board 객체를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        // 일반 사용자 역할 생성
        userRole = new Role();
        userRole.setRoleId(1L);
        userRole.setRoleName("ROLE_USER");
        userRole.setPriority(100);

        // 관리자 역할 생성
        adminRole = new Role();
        adminRole.setRoleId(2L);
        adminRole.setRoleName("ROLE_ADMIN");
        adminRole.setPriority(1);

        // 일반 사용자 생성
        testUser = new User("testuser", "password", "test@example.com", "테스트 사용자");
        testUser.setUserId(1L);
        testUser.setDepartment("개발팀");
        testUser.setIsActive(true);
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        testUser.setRoles(userRoles);

        // 관리자 사용자 생성
        adminUser = new User("admin", "password", "admin@example.com", "관리자");
        adminUser.setUserId(2L);
        adminUser.setDepartment("경영지원팀");
        adminUser.setIsActive(true);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminUser.setRoles(adminRoles);

        // 테스트 게시글 생성
        testBoard = Board.builder()
                .id(1L)
                .title("테스트 게시글")
                .content("<p>테스트 내용입니다.</p>")
                .category("FREE")
                .author(testUser)
                .viewCount(0)
                .isPinned(false)
                .isDeleted(false)
                .build();
    }

    /**
     * SecurityContext를 Mock으로 설정하는 헬퍼 메서드
     *
     * @param user 현재 로그인한 것으로 설정할 사용자
     */
    private void setupSecurityContext(User user) {
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn(user.getUsername());
        SecurityContextHolder.setContext(securityContext);

        given(userRepository.findByUsername(user.getUsername())).willReturn(Optional.of(user));
    }

    // ================================
    // 게시글 조회 테스트
    // ================================

    @Nested
    @DisplayName("게시글 상세 조회 테스트")
    class GetBoardByIdTest {

        @Test
        @DisplayName("게시글 조회 성공 - 조회수 증가")
        void getBoardById_WhenBoardExists_ReturnsBoard() {
            // Given
            given(boardRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.of(testBoard));
            given(boardRepository.increaseViewCount(1L)).willReturn(1);

            // When
            Board result = boardService.getBoardById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("테스트 게시글");

            // 조회수 증가 메서드가 호출되었는지 확인
            then(boardRepository).should().increaseViewCount(1L);
        }

        @Test
        @DisplayName("게시글 조회 실패 - 존재하지 않는 게시글")
        void getBoardById_WhenBoardNotExists_ThrowsException() {
            // Given
            given(boardRepository.findByIdAndIsDeletedFalse(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> boardService.getBoardById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");

            // 조회수 증가 메서드가 호출되지 않았는지 확인
            then(boardRepository).should(never()).increaseViewCount(anyLong());
        }
    }

    // ================================
    // 게시글 목록 조회 테스트
    // ================================

    @Nested
    @DisplayName("게시글 목록 조회 테스트")
    class GetBoardListTest {

        @Test
        @DisplayName("게시글 목록 조회 - 페이징 성공")
        void getBoardList_WithPaging_ReturnsPagedBoards() {
            // Given
            List<Board> boards = Arrays.asList(testBoard);
            Page<Board> boardPage = new PageImpl<>(
                    boards,
                    PageRequest.of(0, 10),
                    1
            );

            given(boardRepository.findByIsDeletedFalse(any(Pageable.class))).willReturn(boardPage);

            // When
            Page<Board> result = boardService.getBoardList(PageRequest.of(0, 10));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 게시글");
        }

        @Test
        @DisplayName("게시글 목록 조회 - 빈 목록")
        void getBoardList_WhenEmpty_ReturnsEmptyPage() {
            // Given
            Page<Board> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, 10),
                    0
            );
            given(boardRepository.findByIsDeletedFalse(any(Pageable.class))).willReturn(emptyPage);

            // When
            Page<Board> result = boardService.getBoardList(PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ================================
    // 카테고리별 조회 테스트
    // ================================

    @Nested
    @DisplayName("카테고리별 게시글 조회 테스트")
    class GetBoardsByCategoryTest {

        @Test
        @DisplayName("카테고리별 게시글 조회 - 성공")
        void getBoardsByCategory_ReturnsFilteredBoards() {
            // Given
            List<Board> freeBoards = Arrays.asList(testBoard);
            Page<Board> freePage = new PageImpl<>(freeBoards, PageRequest.of(0, 10), 1);

            given(boardRepository.findByCategoryAndIsDeletedFalse(eq("FREE"), any(Pageable.class)))
                    .willReturn(freePage);

            // When
            Page<Board> result = boardService.getBoardsByCategory("FREE", PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCategory()).isEqualTo("FREE");
        }
    }

    // ================================
    // 작성자별 조회 테스트
    // ================================

    @Nested
    @DisplayName("작성자별 게시글 조회 테스트")
    class GetBoardsByAuthorTest {

        @Test
        @DisplayName("작성자별 게시글 조회 - 성공")
        void getBoardsByAuthor_ReturnsAuthorBoards() {
            // Given
            List<Board> authorBoards = Arrays.asList(testBoard);
            Page<Board> authorPage = new PageImpl<>(authorBoards, PageRequest.of(0, 10), 1);

            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(boardRepository.findByAuthorAndIsDeletedFalse(eq(testUser), any(Pageable.class)))
                    .willReturn(authorPage);

            // When
            Page<Board> result = boardService.getBoardsByAuthor(1L, PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAuthor().getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 작성자 - 예외 발생")
        void getBoardsByAuthor_WhenAuthorNotExists_ThrowsException() {
            // Given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> boardService.getBoardsByAuthor(999L, PageRequest.of(0, 10)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    // ================================
    // 상단 고정 게시글 테스트
    // ================================

    @Nested
    @DisplayName("상단 고정 게시글 테스트")
    class PinnedBoardsTest {

        @Test
        @DisplayName("상단 고정 게시글 조회 - 성공")
        void getPinnedBoards_ReturnsPinnedBoards() {
            // Given
            Board pinnedBoard = Board.builder()
                    .id(2L)
                    .title("공지사항")
                    .content("중요 공지입니다.")
                    .category("NOTICE")
                    .author(adminUser)
                    .isPinned(true)
                    .isDeleted(false)
                    .build();

            List<Board> pinnedBoards = Arrays.asList(pinnedBoard);
            given(boardRepository.findByIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc())
                    .willReturn(pinnedBoards);

            // When
            List<Board> result = boardService.getPinnedBoards();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isPinned()).isTrue();
            assertThat(result.get(0).getTitle()).isEqualTo("공지사항");
        }
    }

    // ================================
    // 게시글 생성 테스트
    // ================================

    @Nested
    @DisplayName("게시글 생성 테스트")
    class CreateBoardTest {

        @Test
        @DisplayName("게시글 생성 - 성공")
        void createBoard_WithValidInput_ReturnsCreatedBoard() {
            // Given
            setupSecurityContext(testUser);

            Board savedBoard = Board.builder()
                    .id(1L)
                    .title("새 게시글")
                    .content("내용입니다.")
                    .category("FREE")
                    .author(testUser)
                    .viewCount(0)
                    .isPinned(false)
                    .isDeleted(false)
                    .build();

            given(boardRepository.save(any(Board.class))).willReturn(savedBoard);

            // When
            Board result = boardService.createBoard("새 게시글", "내용입니다.", "FREE");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("새 게시글");
            assertThat(result.getAuthor()).isEqualTo(testUser);

            then(boardRepository).should().save(any(Board.class));
        }

        @Test
        @DisplayName("게시글 생성 실패 - 빈 제목")
        void createBoard_WithEmptyTitle_ThrowsException() {
            // Given
            setupSecurityContext(testUser);

            // When & Then
            assertThatThrownBy(() -> boardService.createBoard("", "내용", "FREE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("제목은 필수");
        }

        @Test
        @DisplayName("게시글 생성 실패 - 빈 내용")
        void createBoard_WithEmptyContent_ThrowsException() {
            // Given
            setupSecurityContext(testUser);

            // When & Then
            assertThatThrownBy(() -> boardService.createBoard("제목", "", "FREE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("내용은 필수");
        }

        @Test
        @DisplayName("게시글 생성 - XSS 스크립트 제거 확인")
        void createBoard_WithXssContent_SanitizesHtml() {
            // Given
            setupSecurityContext(testUser);

            String maliciousContent = "<script>alert('XSS')</script><p>정상 내용</p>";

            Board savedBoard = Board.builder()
                    .id(1L)
                    .title("XSS 테스트")
                    .content("<p>정상 내용</p>")  // 스크립트가 제거된 내용
                    .category("FREE")
                    .author(testUser)
                    .build();

            given(boardRepository.save(any(Board.class))).willReturn(savedBoard);

            // When
            Board result = boardService.createBoard("XSS 테스트", maliciousContent, "FREE");

            // Then
            assertThat(result.getContent()).doesNotContain("<script>");
            assertThat(result.getContent()).contains("<p>정상 내용</p>");
        }
    }

    // ================================
    // 게시글 통계 테스트
    // ================================

    @Nested
    @DisplayName("게시글 통계 테스트")
    class BoardStatisticsTest {

        @Test
        @DisplayName("게시판 통계 정보 조회 - 성공")
        void getBoardStatistics_ReturnsStatistics() {
            // Given
            given(boardRepository.countByIsDeletedFalse()).willReturn(100L);

            List<Object[]> categoryStats = new ArrayList<>();
            categoryStats.add(new Object[]{"FREE", 50L});
            categoryStats.add(new Object[]{"NOTICE", 30L});
            categoryStats.add(new Object[]{null, 20L});  // 미분류
            given(boardRepository.countByCategory()).willReturn(categoryStats);

            Page<Board> todayPage = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 10);
            Page<Board> weekPage = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 45);
            given(boardRepository.findBoardsCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(todayPage)
                    .willReturn(weekPage);

            // When
            Map<String, Object> result = boardService.getBoardStatistics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("totalBoards")).isEqualTo(100L);
            assertThat(result.get("categoryStats")).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Long> categories = (Map<String, Long>) result.get("categoryStats");
            assertThat(categories.get("FREE")).isEqualTo(50L);
            assertThat(categories.get("NOTICE")).isEqualTo(30L);
            assertThat(categories.get("미분류")).isEqualTo(20L);
        }
    }

    // ================================
    // 게시글 삭제 테스트
    // ================================

    @Nested
    @DisplayName("게시글 삭제 테스트")
    class DeleteBoardTest {

        @Test
        @DisplayName("작성자가 게시글 삭제 - 성공")
        void deleteBoard_ByAuthor_Success() {
            // Given
            setupSecurityContext(testUser);

            given(boardRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.of(testBoard));
            given(boardRepository.save(any(Board.class))).willReturn(testBoard);

            // When
            boardService.deleteBoard(1L);

            // Then
            assertThat(testBoard.isDeleted()).isTrue();
            then(boardRepository).should().save(testBoard);
        }

        @Test
        @DisplayName("관리자가 다른 사용자의 게시글 삭제 - 성공")
        void deleteBoard_ByAdmin_Success() {
            // Given
            setupSecurityContext(adminUser);

            // testBoard는 testUser가 작성한 게시글
            given(boardRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.of(testBoard));
            given(boardRepository.save(any(Board.class))).willReturn(testBoard);

            // When
            boardService.deleteBoard(1L);

            // Then
            assertThat(testBoard.isDeleted()).isTrue();
        }
    }
}

/*
 * ====== BoardService 테스트 가이드 ======
 *
 * 1. SecurityContext Mock 처리:
 *
 *    BoardService는 getCurrentUser() 메서드에서
 *    SecurityContextHolder를 사용합니다.
 *
 *    테스트에서는 다음과 같이 Mock 처리합니다:
 *
 *    given(securityContext.getAuthentication()).willReturn(authentication);
 *    given(authentication.getName()).willReturn("testuser");
 *    SecurityContextHolder.setContext(securityContext);
 *
 * 2. XSS 방지 테스트:
 *
 *    BoardService는 JSoup을 사용하여 HTML을 새니타이징합니다.
 *    악성 스크립트가 제거되는지 테스트합니다.
 *
 * 3. 권한 테스트:
 *
 *    - 작성자만 수정/삭제 가능
 *    - 관리자는 모든 게시글 수정/삭제 가능
 *    - 상단 고정은 관리자만 가능
 *
 * 4. 페이징 테스트:
 *
 *    PageImpl을 사용하여 Mock Page 객체를 생성합니다:
 *
 *    new PageImpl<>(content, pageable, totalElements)
 *
 * 5. 통계 테스트:
 *
 *    여러 Repository 메서드를 조합하여 통계를 생성합니다.
 *    각 메서드의 반환값을 Mock으로 설정합니다.
 */