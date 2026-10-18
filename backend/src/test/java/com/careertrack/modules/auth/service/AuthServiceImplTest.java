package com.careertrack.modules.auth.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.dto.JwtResponse;
import com.careertrack.modules.auth.dto.LoginRequest;
import com.careertrack.modules.auth.dto.RefreshTokenRequest;
import com.careertrack.modules.auth.entity.RefreshToken;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RefreshTokenRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private com.careertrack.modules.employee.repository.EmployeeRepository employeeRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        Role role = Role.builder().id(UUID.randomUUID()).name(RoleType.ROLE_DRH).build();
        user = User.builder()
                .id(userId)
                .username("drh")
                .email("drh@careertrack.sn")
                .password("hash")
                .roles(Set.of(role))
                .enabled(true)
                .build();

        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);
    }

    @Test
    @DisplayName("Authentification réussie générant un access token et un refresh token")
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setUsername("drh");
        request.setPassword("password123");

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .username("drh")
                .email("drh@careertrack.sn")
                .password("hash")
                .enabled(true)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH")))
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateAccessToken(auth)).thenReturn("dummy-jwt-access-token");
        when(tokenProvider.getExpirationTime()).thenReturn(900000L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken rt = i.getArgument(0);
            rt.setToken("dummy-refresh-token");
            return rt;
        });

        JwtResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("dummy-jwt-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("dummy-refresh-token");
        assertThat(response.getUsername()).isEqualTo("drh");
        assertThat(response.getRoles()).contains("ROLE_DRH");
    }

    @Test
    @DisplayName("Rafraîchissement réussi avec un refresh token valide")
    void shouldRefreshTokenSuccessfully() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        RefreshToken rt = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("valid-refresh-token")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(rt));
        when(tokenProvider.generateAccessTokenFromPrincipal(any(UserPrincipal.class))).thenReturn("new-access-token");
        when(tokenProvider.getExpirationTime()).thenReturn(900000L);

        JwtResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
    }

    @Test
    @DisplayName("Échec si le refresh token est expiré")
    void shouldThrowBadRequestWhenRefreshTokenExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        RefreshToken rt = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("expired-token")
                .expiryDate(Instant.now().minusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expiré");

        verify(refreshTokenRepository).delete(rt);
    }
}
