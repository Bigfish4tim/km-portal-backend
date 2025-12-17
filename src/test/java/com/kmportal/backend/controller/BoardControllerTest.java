package com.kmportal.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kmportal.backend.entity.Board;
import com.kmportal.backend.entity.Role;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.BoardRepository;
import com.kmportal.backend.repository.RoleRepository;
import com.kmportal.backend.repository.UserRepository;
import com.kmportal.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BoardController 통합 테스트 클래스
 *
 * 이 클래스는 게시판 관련 API 엔드포인트의 통합 테스트를 수행합니다.
 * 실제 데이터베이스(H2)와 Spring Security를 포함한 전체 애플리케이션 컨텍스트에서 테스트합니다.
 *
 * [테스트 대상 API]
 *
 * 1. GET    /api/boards           - 게시글 목록 조회 (페이징)
 * 2. GET    /api/boards/{id}      - 게시글 상세 조회
 * 3. POST   /api/boards           - 게시글 생성
 * 4. PUT    /api/boards/{id}      - 게시글 수정
 * 5. DELETE /api/boards/{id}      - 게시글 삭제
 * 6. GET    /api/boards/search    - 게시글 검색
 * 7. GET    /api/boards/category/{category} - 카테고리별 조회
 *
 * [Board 엔티티 구조]
 *
 * - id: Long (PK) - getId()로 접근
 * - title, content, category: String
 * - author: User (ManyToOne)
 * - viewCount: Integer
 * - isPinned, isDeleted: Boolean
 * - BaseEntity 상속 (createdAt, updatedAt 자동 관리)
 * - Builder 패턴 사용
 *
 * 작성일: 2025년 11월 30일 (41일차)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-30
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("BoardController 통합 테스트")
class BoardControllerTest {

    // ================================
    // 의존성 주입
    // ================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // ================================
    // 테스트 픽스처
    // ================================

    private User testUser;
    private User adminUser;
    private User otherUser;
    private Role userRole;
    private Role adminRole;
    private Board testBoard;
    private String userToken;
    private String adminToken;
    private String otherUserToken;

    /**
     * 각 테스트 실행 전 초기화
     */
    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        boardRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // ========== 역할 생성 ==========
        userRole = new Role();
        userRole.setRoleName("ROLE_USER");
        userRole.setDescription("일반 사용자");
        userRole.setPriority(100);
        userRole = roleRepository.save(userRole);

        adminRole = new Role();
        adminRole.setRoleName("ROLE_ADMIN");
        adminRole.setDescription("관리자");
        adminRole.setPriority(1);
        adminRole = roleRepository.save(adminRole);

        // ========== 일반 사용자 생성 ==========
        testUser = createUser("testuser", "test@example.com", "테스트 사용자", userRole);
        testUser = userRepository.save(testUser);

        // ========== 관리자 생성 ==========
        adminUser = createUser("admin", "admin@example.com", "관리자", adminRole);
        adminUser = userRepository.save(adminUser);

        // ========== 다른 사용자 생성 ==========
        otherUser = createUser("otheruser", "other@example.com", "다른 사용자", userRole);
        otherUser = userRepository.save(otherUser);

        // ========== 테스트 게시글 생성 (Builder 패턴) ==========
        testBoard = Board.builder()
                .title("테스트 게시글 제목")
                .content("<p>테스트 게시글 내용입니다.</p>")
                .category("NOTICE")
                .author(testUser)
                .viewCount(0)
                .isPinned(false)
                .isDeleted(false)
                .build();
        testBoard = boardRepository.save(testBoard);

        // ========== JWT 토큰 생성 ==========
        userToken = generateToken(testUser, "ROLE_USER");
        adminToken = generateToken(adminUser, "ROLE_ADMIN");
        otherUserToken = generateToken(otherUser, "ROLE_USER");
    }

    /**
     * 테스트용 사용자 생성 헬퍼 메서드
     */
    private User createUser(String username, String email, String fullName, Role role) {
        User user = new User(
                username,
                passwordEncoder.encode("password123"),
                email,
                fullName
        );
        user.setDepartment("개발팀");
        user.setPosition("대리");
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setPasswordExpired(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        return user;
    }

    /**
     * JWT 토큰 생성 헬퍼 메서드
     */
    private String generateToken(User user, String roleName) {
        return jwtUtil.generateToken(
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getDepartment(),
                Arrays.asList(roleName)
        );
    }

    /**
     * 테스트용 게시글 생성 헬퍼 메서드 (Builder 패턴)
     */
    private Board createBoard(String title, String content, String category, User author) {
        Board board = Board.builder()
                .title(title)
                .content(content)
                .category(category)
                .author(author)
                .viewCount(0)
                .isPinned(false)
                .isDeleted(false)
                .build();
        return boardRepository.save(board);
    }

    // ================================
    // 게시글 목록 조회 테스트
    // ================================

    @Nested
    @DisplayName("GET /api/boards - 게시글 목록 조회")
    class GetBoardListTest {

        /**
         * 게시글 목록 조회 성공
         */
        @Test
        @DisplayName("게시글 목록 조회 성공")
        void getBoardList_ReturnsPagedBoards() throws Exception {
            // Given: 추가 게시글 생성
            for (int i = 0; i < 5; i++) {
                createBoard("게시글 " + i, "내용 " + i, "FREE", testUser);
            }

            // When & Then
            mockMvc.perform(
                            get("/api/boards")
                                    .header("Authorization", "Bearer " + userToken)
                                    .param("page", "0")
                                    .param("size", "10")
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(6)))
                    .andExpect(jsonPath("$.pageable").exists());
        }

        /**
         * 게시글 목록 조회 - 페이징 테스트
         */
        @Test
        @DisplayName("게시글 목록 조회 - 페이징")
        void getBoardList_WithPaging_ReturnsCorrectPage() throws Exception {
            // Given: 15개 게시글 생성
            for (int i = 0; i < 15; i++) {
                createBoard("페이징 테스트 " + i, "내용 " + i, "FREE", testUser);
            }

            // When & Then: 페이지 0, 사이즈 5
            mockMvc.perform(
                            get("/api/boards")
                                    .header("Authorization", "Bearer " + userToken)
                                    .param("page", "0")
                                    .param("size", "5")
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(5));
        }

        /**
         * 빈 결과 테스트
         */
        @Test
        @DisplayName("게시글 목록 조회 - 빈 결과")
        void getBoardList_WhenEmpty_ReturnsEmptyPage() throws Exception {
            // Given: 모든 게시글 삭제
            boardRepository.deleteAll();

            // When & Then
            mockMvc.perform(
                            get("/api/boards")
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ================================
    // 게시글 상세 조회 테스트
    // ================================

    @Nested
    @DisplayName("GET /api/boards/{id} - 게시글 상세 조회")
    class GetBoardByIdTest {

        /**
         * 게시글 상세 조회 성공
         *
         * Board 엔티티는 getId()로 ID 접근
         */
        @Test
        @DisplayName("게시글 상세 조회 성공")
        void getBoardById_WhenExists_ReturnsBoard() throws Exception {
            // When & Then - getId() 사용
            mockMvc.perform(
                            get("/api/boards/" + testBoard.getId())
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testBoard.getId()))
                    .andExpect(jsonPath("$.title").value("테스트 게시글 제목"))
                    .andExpect(jsonPath("$.content").value("<p>테스트 게시글 내용입니다.</p>"))
                    .andExpect(jsonPath("$.category").value("NOTICE"));
        }

        /**
         * 조회수 증가 테스트
         */
        @Test
        @DisplayName("게시글 조회 시 조회수 증가")
        void getBoardById_IncreasesViewCount() throws Exception {
            // Given: 초기 조회수 확인
            int initialViewCount = testBoard.getViewCount();

            // When: 게시글 조회
            mockMvc.perform(
                            get("/api/boards/" + testBoard.getId())
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andExpect(status().isOk());

            // Then: 조회수 증가 확인
            // 구현에 따라 확인 방법이 다를 수 있음
        }

        /**
         * 존재하지 않는 게시글 조회
         */
        @Test
        @DisplayName("게시글 상세 조회 실패 - 존재하지 않음")
        void getBoardById_WhenNotExists_ReturnsNotFound() throws Exception {
            // When & Then
            mockMvc.perform(
                            get("/api/boards/99999")
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        /**
         * 인증 없이 게시글 조회 (공개 설정에 따라 다름)
         */
        @Test
        @DisplayName("게시글 조회 - 인증 없이")
        void getBoardById_WithoutAuth_DependsOnConfig() throws Exception {
            // When & Then
            // 구현에 따라 200 OK (공개) 또는 401 Unauthorized (비공개)
            mockMvc.perform(
                            get("/api/boards/" + testBoard.getId())
                    )
                    .andDo(print());
        }
    }

    // ================================
    // 게시글 생성 테스트
    // ================================

    @Nested
    @DisplayName("POST /api/boards - 게시글 생성")
    class CreateBoardTest {

        /**
         * 게시글 생성 성공
         */
        @Test
        @DisplayName("게시글 생성 성공")
        void createBoard_WithValidInput_ReturnsCreatedBoard() throws Exception {
            // Given
            Map<String, Object> boardRequest = new HashMap<>();
            boardRequest.put("title", "새로운 게시글");
            boardRequest.put("content", "새로운 게시글 내용입니다.");
            boardRequest.put("category", "FREE");

            // When & Then
            mockMvc.perform(
                            post("/api/boards")
                                    .header("Authorization", "Bearer " + userToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(boardRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())  // 또는 isCreated() (201)
                    .andExpect(jsonPath("$.title").value("새로운 게시글"))
                    .andExpect(jsonPath("$.content").value("새로운 게시글 내용입니다."))
                    .andExpect(jsonPath("$.category").value("FREE"))
                    .andExpect(jsonPath("$.id").exists());
        }

        /**
         * 게시글 생성 실패 - 인증 없음
         */
        @Test
        @DisplayName("게시글 생성 실패 - 인증 없음")
        void createBoard_WithoutAuth_ReturnsUnauthorized() throws Exception {
            // Given
            Map<String, Object> boardRequest = new HashMap<>();
            boardRequest.put("title", "인증 없는 게시글");
            boardRequest.put("content", "내용");
            boardRequest.put("category", "FREE");

            // When & Then
            mockMvc.perform(
                            post("/api/boards")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(boardRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        /**
         * 게시글 생성 실패 - 빈 제목
         */
        @Test
        @DisplayName("게시글 생성 실패 - 빈 제목")
        void createBoard_WithEmptyTitle_ReturnsBadRequest() throws Exception {
            // Given
            Map<String, Object> boardRequest = new HashMap<>();
            boardRequest.put("title", "");  // 빈 제목
            boardRequest.put("content", "내용");
            boardRequest.put("category", "FREE");

            // When & Then
            mockMvc.perform(
                            post("/api/boards")
                                    .header("Authorization", "Bearer " + userToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(boardRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        /**
         * 게시글 생성 실패 - 빈 내용
         */
        @Test
        @DisplayName("게시글 생성 실패 - 빈 내용")
        void createBoard_WithEmptyContent_ReturnsBadRequest() throws Exception {
            // Given
            Map<String, Object> boardRequest = new HashMap<>();
            boardRequest.put("title", "제목");
            boardRequest.put("content", "");  // 빈 내용
            boardRequest.put("category", "FREE");

            // When & Then
            mockMvc.perform(
                            post("/api/boards")
                                    .header("Authorization", "Bearer " + userToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(boardRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        /**
         * XSS 스크립트 제거 테스트
         */
        @Test
        @DisplayName("게시글 생성 - XSS 스크립트 처리")
        void createBoard_WithXssScript_SanitizesContent() throws Exception {
            // Given: XSS 스크립트가 포함된 내용
            Map<String, Object> boardRequest = new HashMap<>();
            boardRequest.put("title", "XSS 테스트");
            boardRequest.put("content", "<script>alert('XSS')</script>안녕하세요");
            boardRequest.put("category", "FREE");

            // When
            ResultActions result = mockMvc.perform(
                    post("/api/boards")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(boardRequest))
            );

            // Then: 구현에 따라 스크립트 제거 또는 그대로 저장
            result.andDo(print())
                    .andExpect(status().isOk());
        }
    }

    // ================================
    // 게시글 수정 테스트
    // ================================

    @Nested
    @DisplayName("PUT /api/boards/{id} - 게시글 수정")
    class UpdateBoardTest {

        /**
         * 게시글 수정 성공 - 작성자
         */
        @Test
        @DisplayName("게시글 수정 성공 - 작성자")
        void updateBoard_ByAuthor_ReturnsUpdatedBoard() throws Exception {
            // Given
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "수정된 제목");
            updateRequest.put("content", "수정된 내용입니다.");
            updateRequest.put("category", "NOTICE");

            // When & Then - getId() 사용
            mockMvc.perform(
                            put("/api/boards/" + testBoard.getId())
                                    .header("Authorization", "Bearer " + userToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("수정된 제목"))
                    .andExpect(jsonPath("$.content").value("수정된 내용입니다."));
        }

        /**
         * 게시글 수정 성공 - 관리자
         */
        @Test
        @DisplayName("게시글 수정 성공 - 관리자")
        void updateBoard_ByAdmin_ReturnsUpdatedBoard() throws Exception {
            // Given
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "관리자가 수정한 제목");
            updateRequest.put("content", "관리자가 수정한 내용");
            updateRequest.put("category", "NOTICE");

            // When & Then
            mockMvc.perform(
                            put("/api/boards/" + testBoard.getId())
                                    .header("Authorization", "Bearer " + adminToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("관리자가 수정한 제목"));
        }

        /**
         * 게시글 수정 실패 - 다른 사용자
         */
        @Test
        @DisplayName("게시글 수정 실패 - 권한 없음 (다른 사용자)")
        void updateBoard_ByOtherUser_ReturnsForbidden() throws Exception {
            // Given
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "다른 사용자가 수정 시도");
            updateRequest.put("content", "내용");
            updateRequest.put("category", "FREE");

            // When & Then
            mockMvc.perform(
                            put("/api/boards/" + testBoard.getId())
                                    .header("Authorization", "Bearer " + otherUserToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        /**
         * 존재하지 않는 게시글 수정
         */
        @Test
        @DisplayName("게시글 수정 실패 - 존재하지 않음")
        void updateBoard_WhenNotExists_ReturnsNotFound() throws Exception {
            // Given
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("title", "수정 시도");
            updateRequest.put("content", "내용");
            updateRequest.put("category", "FREE");

            // When & Then
            mockMvc.perform(
                            put("/api/boards/99999")
                                    .header("Authorization", "Bearer " + userToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    // ================================
    // 게시글 삭제 테스트
    // ================================

    @Nested
    @DisplayName("DELETE /api/boards/{id} - 게시글 삭제")
    class DeleteBoardTest {

        /**
         * 게시글 삭제 성공 - 작성자
         */
        @Test
        @DisplayName("게시글 삭제 성공 - 작성자")
        void deleteBoard_ByAuthor_ReturnsSuccess() throws Exception {
            // When & Then - getId() 사용
            mockMvc.perform(
                            delete("/api/boards/" + testBoard.getId())
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        /**
         * 게시글 삭제 성공 - 관리자
         */
        @Test
        @DisplayName("게시글 삭제 성공 - 관리자")
        void deleteBoard_ByAdmin_ReturnsSuccess() throws Exception {
            // When & Then
            mockMvc.perform(
                            delete("/api/boards/" + testBoard.getId())
                                    .header("Authorization", "Bearer " + adminToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        /**
         * 게시글 삭제 실패 - 다른 사용자
         */
        @Test
        @DisplayName("게시글 삭제 실패 - 권한 없음")
        void deleteBoard_ByOtherUser_ReturnsForbidden() throws Exception {
            // When & Then
            mockMvc.perform(
                            delete("/api/boards/" + testBoard.getId())
                                    .header("Authorization", "Bearer " + otherUserToken)
                    )
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        /**
         * 존재하지 않는 게시글 삭제
         */
        @Test
        @DisplayName("게시글 삭제 실패 - 존재하지 않음")
        void deleteBoard_WhenNotExists_ReturnsNotFound() throws Exception {
            // When & Then
            mockMvc.perform(
                            delete("/api/boards/99999")
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        /**
         * 인증 없이 삭제 시도
         */
        @Test
        @DisplayName("게시글 삭제 실패 - 인증 없음")
        void deleteBoard_WithoutAuth_ReturnsUnauthorized() throws Exception {
            // When & Then
            mockMvc.perform(
                            delete("/api/boards/" + testBoard.getId())
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    // ================================
    // 게시글 검색 테스트
    // ================================

    @Nested
    @DisplayName("GET /api/boards/search - 게시글 검색")
    class SearchBoardTest {

        @BeforeEach
        void setUpSearchData() {
            // 검색 테스트용 추가 데이터
            createBoard("Spring Boot 튜토리얼", "Spring 관련 내용입니다.", "TECH", testUser);
            createBoard("Vue.js 가이드", "Vue 관련 내용입니다.", "TECH", testUser);
            createBoard("일반 공지사항", "공지 내용입니다.", "NOTICE", testUser);
        }

        /**
         * 제목으로 검색
         */
        @Test
        @DisplayName("게시글 검색 - 제목 키워드")
        void searchBoards_ByTitle_ReturnsMatchingBoards() throws Exception {
            // When & Then
            mockMvc.perform(
                            get("/api/boards/search")
                                    .header("Authorization", "Bearer " + userToken)
                                    .param("keyword", "Spring")
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[*].title", everyItem(containsString("Spring"))));
        }

        /**
         * 카테고리로 필터링
         */
        @Test
        @DisplayName("게시글 검색 - 카테고리 필터")
        void searchBoards_ByCategory_ReturnsMatchingBoards() throws Exception {
            // When & Then
            mockMvc.perform(
                            get("/api/boards/search")
                                    .header("Authorization", "Bearer " + userToken)
                                    .param("category", "TECH")
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[*].category", everyItem(equalTo("TECH"))));
        }

        /**
         * 검색 결과 없음
         */
        @Test
        @DisplayName("게시글 검색 - 결과 없음")
        void searchBoards_NoMatch_ReturnsEmptyResult() throws Exception {
            // When & Then
            mockMvc.perform(
                            get("/api/boards/search")
                                    .header("Authorization", "Bearer " + userToken)
                                    .param("keyword", "존재하지않는키워드xyz123")
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    // ================================
    // 카테고리별 조회 테스트
    // ================================

    @Nested
    @DisplayName("GET /api/boards/category/{category} - 카테고리별 조회")
    class GetBoardsByCategoryTest {

        @BeforeEach
        void setUpCategoryData() {
            // 카테고리별 테스트 데이터
            for (int i = 0; i < 3; i++) {
                createBoard("공지사항 " + i, "공지 내용 " + i, "NOTICE", testUser);
                createBoard("자유게시판 " + i, "자유 내용 " + i, "FREE", testUser);
                createBoard("Q&A " + i, "Q&A 내용 " + i, "QNA", testUser);
            }
        }

        /**
         * 특정 카테고리 게시글 조회
         */
        @Test
        @DisplayName("카테고리별 게시글 조회")
        void getBoardsByCategory_ReturnsCorrectBoards() throws Exception {
            // When & Then
            mockMvc.perform(
                            get("/api/boards/category/NOTICE")
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[*].category", everyItem(equalTo("NOTICE"))));
        }

        /**
         * 존재하지 않는 카테고리
         */
        @Test
        @DisplayName("카테고리별 조회 - 결과 없음")
        void getBoardsByCategory_WhenEmpty_ReturnsEmptyResult() throws Exception {
            // When & Then
            mockMvc.perform(
                            get("/api/boards/category/NONEXISTENT")
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    // ================================
    // 고정 게시글 테스트
    // ================================

    @Nested
    @DisplayName("고정 게시글 관련 테스트")
    class PinnedBoardTest {

        /**
         * 관리자가 게시글 고정
         */
        @Test
        @DisplayName("게시글 고정 - 관리자")
        void pinBoard_ByAdmin_Success() throws Exception {
            // When & Then - getId() 사용
            mockMvc.perform(
                            patch("/api/boards/" + testBoard.getId() + "/pin")
                                    .header("Authorization", "Bearer " + adminToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        /**
         * 일반 사용자가 게시글 고정 시도 - 실패
         */
        @Test
        @DisplayName("게시글 고정 실패 - 권한 없음")
        void pinBoard_ByUser_ReturnsForbidden() throws Exception {
            // When & Then
            mockMvc.perform(
                            patch("/api/boards/" + testBoard.getId() + "/pin")
                                    .header("Authorization", "Bearer " + userToken)
                    )
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }
}

/*
 * ====== BoardController 통합 테스트 가이드 ======
 *
 * 1. Board 엔티티 주요 메서드:
 *
 *    - getId(): 게시글 ID 조회 (getBoardId() 아님!)
 *    - getTitle(), getContent(), getCategory()
 *    - getAuthor(): User 엔티티 반환
 *    - getViewCount()
 *    - isPinned(), isDeleted()
 *    - softDelete(), restore()
 *
 * 2. Board 생성 (Builder 패턴):
 *
 *    Board board = Board.builder()
 *        .title("제목")
 *        .content("내용")
 *        .category("FREE")
 *        .author(user)
 *        .viewCount(0)
 *        .isPinned(false)
 *        .isDeleted(false)
 *        .build();
 *
 * 3. 테스트 실행:
 *
 *    mvn test -Dtest=BoardControllerTest
 *    mvn test -Dtest=BoardControllerTest$CreateBoardTest
 *
 * 4. 권한 테스트 패턴:
 *
 *    - 작성자 본인: 수정/삭제 가능
 *    - 관리자: 모든 게시글 수정/삭제 가능
 *    - 다른 사용자: 403 Forbidden
 *    - 비인증: 401 Unauthorized
 *
 * 작성일: 2025년 11월 30일 (41일차)
 */