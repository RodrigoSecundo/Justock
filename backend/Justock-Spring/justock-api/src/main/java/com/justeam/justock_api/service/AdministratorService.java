package com.justeam.justock_api.service;

import com.justeam.justock_api.model.Administrator;
import com.justeam.justock_api.repository.AdministratorRepository;
import com.justeam.justock_api.request.AdministratorCreateRequest;
import com.justeam.justock_api.request.AdministratorUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class AdministratorService {

    @Autowired
    private AdministratorRepository administratorRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<Administrator> listAllAdministrators() {
        return administratorRepository.findAll();
    }

    public Administrator findAdministrator(int id) {
        return administratorRepository.findById(id).orElse(null);
    }

    public Administrator createAdministrator(AdministratorCreateRequest dto) {
        Administrator administrator = new Administrator();
        administrator.setEmailCorporativo(dto.getEmail());
        administrator.setSenha(passwordEncoder.encode(dto.getPassword()));
        return administratorRepository.save(administrator);
    }

    @Transactional
    public Administrator updateAdministrator(int id, AdministratorUpdateRequest dto) {
        Administrator existingAdministrator = administratorRepository.findById(id).orElse(null);
        if (existingAdministrator != null) {
            existingAdministrator.setEmailCorporativo(dto.getEmail());
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                existingAdministrator.setSenha(passwordEncoder.encode(dto.getPassword()));
            }
            return administratorRepository.save(existingAdministrator);
        }
        return null;
    }

    public boolean deleteAdministrator(int id) {
        if (administratorRepository.existsById(id)) {
            administratorRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
