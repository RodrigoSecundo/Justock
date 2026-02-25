package com.justeam.justock_api.service;

import com.justeam.justock_api.model.User;
import com.justeam.justock_api.repository.UserRepository;
import com.justeam.justock_api.request.UserCreateRequest;
import com.justeam.justock_api.request.UserUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<User> listAllusers() {
        return userRepository.findAll();
    }

    public User finduser(int id) {
        return userRepository.findById(id).orElse(null);
    }

    public User createuser(UserCreateRequest dto) {
        User user = new User();
        user.setNomeUsuario(dto.getName());
        user.setEmailCorporativo(dto.getEmail());
        user.setSenha(passwordEncoder.encode(dto.getPassword()));
        user.setNumero(dto.getNumero());
        return userRepository.save(user);
    }

    @Transactional
    public User updateuser(int id, UserUpdateRequest dto) {
        User existinguser = userRepository.findById(id).orElse(null);
        if (existinguser != null) {
            existinguser.setNomeUsuario(dto.getName());
            existinguser.setEmailCorporativo(dto.getEmail());
            existinguser.setNumero(dto.getNumero());
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                existinguser.setSenha(passwordEncoder.encode(dto.getPassword()));
            }
            return userRepository.save(existinguser);
        }
        return null;
    }

    public boolean deleteuser(int id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
}