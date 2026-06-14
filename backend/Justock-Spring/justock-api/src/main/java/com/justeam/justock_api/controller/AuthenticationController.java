package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.CurrentAccountResponseDTO;
import com.justeam.justock_api.dto.LoginResponseDTO;
import com.justeam.justock_api.dto.UserResponseDTO;
import com.justeam.justock_api.model.User;
import com.justeam.justock_api.request.LoginRequest;
import com.justeam.justock_api.request.ProfileUpdateRequest;
import com.justeam.justock_api.request.UserCreateRequest;
import com.justeam.justock_api.security.CustomUserDetailsService;
import com.justeam.justock_api.security.JwtBlacklistService;
import com.justeam.justock_api.security.JwtUtil;
import com.justeam.justock_api.service.CurrentAccountService;
import com.justeam.justock_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentAccountService currentAccountService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(new ApiResponseDTO<>(401, "Email ou senha inválidos!", null));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        String token = jwtUtil.generateToken(userDetails.getUsername(), role);
        CurrentAccountResponseDTO account = currentAccountService.resolveForLogin(request.getEmail(), role);

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                account.getId(),
                account.getDashboardUserId(),
                account.getEmail(),
                account.getName(),
                account.getNumero(),
                account.getRole(),
                account.isPrimaryAdmin()
        );
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Login realizado com sucesso!", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> register(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.createuser(request);
        UserResponseDTO response = new UserResponseDTO(
                user.getIdUsuario(),
                user.getNomeUsuario(),
                user.getEmailCorporativo(),
                user.getNumero()
        );

        return ResponseEntity.status(201)
                .body(new ApiResponseDTO<>(201, "Conta criada com sucesso!", response));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<CurrentAccountResponseDTO>> me() {
        CurrentAccountResponseDTO response = currentAccountService.getCurrentAccount();
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Perfil carregado com sucesso!", response));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<CurrentAccountResponseDTO>> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        CurrentAccountResponseDTO response = currentAccountService.updateCurrentProfile(request);
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Perfil atualizado com sucesso!", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDTO<String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            long expiration = jwtUtil.extractExpiration(token).getTime();
            jwtBlacklistService.addToBlacklist(token, expiration);
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Logout realizado com sucesso!", "Token invalidado"));
        }
        return ResponseEntity.badRequest().body(new ApiResponseDTO<>(400, "Token não fornecido!", null));
    }
}
