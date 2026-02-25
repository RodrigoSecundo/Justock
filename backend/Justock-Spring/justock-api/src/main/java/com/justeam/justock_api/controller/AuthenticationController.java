package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.LoginResponseDTO;
import com.justeam.justock_api.model.Administrator;
import com.justeam.justock_api.model.User;
import com.justeam.justock_api.repository.AdministratorRepository;
import com.justeam.justock_api.repository.UserRepository;
import com.justeam.justock_api.request.LoginRequest;
import com.justeam.justock_api.security.CustomUserDetailsService;
import com.justeam.justock_api.security.JwtBlacklistService;
import com.justeam.justock_api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
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
    private UserRepository userRepository;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        String token = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getAuthorities().toString().replace("ROLE_", "").replace("[", "").replace("]", ""));

        String name = "";
        Optional<Administrator> adminOpt = administratorRepository.findByEmailCorporativo(request.getEmail());
        if (adminOpt.isPresent()) {
            name = "Administrator";
        } else {
            Optional<User> userOpt = userRepository.findByEmailCorporativo(request.getEmail());
            if (userOpt.isPresent()) {
                name = userOpt.get().getNomeUsuario();
            }
        }

        String role = userDetails.getAuthorities().toString().replace("ROLE_", "").replace("[", "").replace("]", "");

        LoginResponseDTO response = new LoginResponseDTO(token, request.getEmail(), name, role);
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Login realizado com sucesso!", response));
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
