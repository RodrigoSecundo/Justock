package com.justeam.justock_api.security;

import com.justeam.justock_api.model.Administrator;
import com.justeam.justock_api.model.User;
import com.justeam.justock_api.repository.AdministratorRepository;
import com.justeam.justock_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
 
        Optional<Administrator> adminOpt = administratorRepository.findByEmailCorporativo(email);
        if (adminOpt.isPresent()) {
            Administrator admin = adminOpt.get();
            String role = admin.getRole();
            if (role == null || role.isEmpty()) {
                role = "ADMIN";
            }
            return new org.springframework.security.core.userdetails.User(
                    admin.getEmailCorporativo(),
                    admin.getSenha(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );
        }


        Optional<User> userOpt = userRepository.findByEmailCorporativo(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String role = user.getRole();
            if (role == null || role.isEmpty()) {
                role = "USER";
            }
            return new org.springframework.security.core.userdetails.User(
                    user.getEmailCorporativo(),
                    user.getSenha(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );
        }

        throw new UsernameNotFoundException("Usuário não encontrado com email: " + email);
    }
}
