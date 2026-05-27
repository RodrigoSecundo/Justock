package com.justeam.justock_api.service;

import com.justeam.justock_api.dto.CurrentAccountResponseDTO;
import com.justeam.justock_api.exception.BadRequestException;
import com.justeam.justock_api.exception.UnauthorizedException;
import com.justeam.justock_api.model.Administrator;
import com.justeam.justock_api.model.User;
import com.justeam.justock_api.repository.AdministratorRepository;
import com.justeam.justock_api.repository.UserRepository;
import com.justeam.justock_api.request.ProfileUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CurrentAccountService {

    private final UserRepository userRepository;
    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${justock.primary-admin.email:testeAdminSEC@exemplo.com}")
    private String primaryAdminEmail;

    @Value("${mercadolivre.shared.usuario-id:1}")
    private Integer sharedDashboardUserId;

    public CurrentAccountService(
            UserRepository userRepository,
            AdministratorRepository administratorRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.administratorRepository = administratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CurrentAccountResponseDTO getCurrentAccount() {
        return toDto(resolveAuthenticatedAccount());
    }

    public Integer getDashboardUserId() {
        return resolveAuthenticatedAccount().dashboardUserId();
    }

    public boolean isPrimaryAdmin() {
        return resolveAuthenticatedAccount().primaryAdmin();
    }

    public CurrentAccountResponseDTO resolveForLogin(String email, String fallbackRole) {
        return toDto(resolveByEmail(email, fallbackRole));
    }

    public CurrentAccountResponseDTO updateCurrentProfile(ProfileUpdateRequest request) {
        ResolvedAccount account = resolveAuthenticatedAccount();
        validatePasswordConfirmation(request.getPassword(), request.getPasswordConfirmation());

        return userRepository.findByEmailCorporativo(account.email())
                .map(user -> updateUserProfile(user, request))
                .orElseGet(() -> administratorRepository.findByEmailCorporativo(account.email())
                        .map(admin -> updateAdministratorProfile(admin, request))
                        .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado.")));
    }

    private CurrentAccountResponseDTO updateUserProfile(User user, ProfileUpdateRequest request) {
        if (request.getName() != null) {
            String normalizedName = normalizeNullable(request.getName());
            if (normalizedName != null) {
                user.setNomeUsuario(normalizedName);
            }
        }
        if (request.getNumero() != null) {
            user.setNumero(normalizeNullable(request.getNumero()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setSenha(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(user);
        return toDto(resolveUser(savedUser, savedUser.getRole()));
    }

    private CurrentAccountResponseDTO updateAdministratorProfile(Administrator administrator, ProfileUpdateRequest request) {
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            administrator.setSenha(passwordEncoder.encode(request.getPassword()));
            administratorRepository.save(administrator);
        }

        return toDto(resolveAdministrator(administrator, administrator.getRole()));
    }

    private void validatePasswordConfirmation(String password, String passwordConfirmation) {
        if (password == null || password.isBlank()) {
            return;
        }

        if (passwordConfirmation == null || !password.equals(passwordConfirmation)) {
            throw new BadRequestException("As senhas não coincidem!");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private ResolvedAccount resolveAuthenticatedAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthorizedException("Usuário não autenticado.");
        }

        String fallbackRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse(null);

        return resolveByEmail(authentication.getName(), fallbackRole);
    }

    private ResolvedAccount resolveByEmail(String email, String fallbackRole) {
        return userRepository.findByEmailCorporativo(email)
                .map(user -> resolveUser(user, fallbackRole))
                .orElseGet(() -> administratorRepository.findByEmailCorporativo(email)
                        .map(admin -> resolveAdministrator(admin, fallbackRole))
                        .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado.")));
    }

    private ResolvedAccount resolveUser(User user, String fallbackRole) {
        String email = user.getEmailCorporativo();
        boolean primaryAdmin = isPrimaryAdminEmail(email);
        String resolvedRole = normalizeRole(user.getRole(), fallbackRole, "USER");

        return new ResolvedAccount(
                user.getIdUsuario(),
                primaryAdmin ? sharedDashboardUserId : user.getIdUsuario(),
                email,
                user.getNomeUsuario(),
                user.getNumero(),
                resolvedRole,
                primaryAdmin);
    }

    private ResolvedAccount resolveAdministrator(Administrator administrator, String fallbackRole) {
        String email = administrator.getEmailCorporativo();
        boolean primaryAdmin = isPrimaryAdminEmail(email);
        String resolvedRole = normalizeRole(administrator.getRole(), fallbackRole, "ADMIN");

        return new ResolvedAccount(
                administrator.getIdAdministrador(),
                primaryAdmin ? sharedDashboardUserId : administrator.getIdAdministrador(),
                email,
                primaryAdmin ? "Administrador principal" : "Administrador",
                null,
                resolvedRole,
                primaryAdmin);
    }

    private String normalizeRole(String preferredRole, String fallbackRole, String defaultRole) {
        if (preferredRole != null && !preferredRole.isBlank()) {
            return preferredRole;
        }
        if (fallbackRole != null && !fallbackRole.isBlank()) {
            return fallbackRole;
        }
        return defaultRole;
    }

    private boolean isPrimaryAdminEmail(String email) {
        return email != null && email.equalsIgnoreCase(primaryAdminEmail);
    }

    private CurrentAccountResponseDTO toDto(ResolvedAccount account) {
        return new CurrentAccountResponseDTO(
                account.id(),
                account.dashboardUserId(),
                account.email(),
                account.name(),
                account.numero(),
                account.role(),
                account.primaryAdmin());
    }

    private record ResolvedAccount(
            Integer id,
            Integer dashboardUserId,
            String email,
            String name,
            String numero,
            String role,
            boolean primaryAdmin) {
    }
}