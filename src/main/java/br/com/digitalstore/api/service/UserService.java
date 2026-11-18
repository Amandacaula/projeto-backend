package br.com.digitalstore.api.service;

import br.com.digitalstore.api.dto.user.UserCreateRequest;
import br.com.digitalstore.api.dto.user.UserResponse;
import br.com.digitalstore.api.dto.user.UserUpdateRequest;
import br.com.digitalstore.api.exception.BadRequestException;
import br.com.digitalstore.api.exception.NotFoundException;
import br.com.digitalstore.api.model.User;
import br.com.digitalstore.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    static final String EMAIL_TAKEN = "E-mail já cadastrado";
    static final String PASSWORDS_DIFFER = "As senhas não conferem";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Integer id) {
        return UserResponse.from(getOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw BadRequestException.forField("confirmPassword", PASSWORDS_DIFFER);
        }
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw BadRequestException.forField("email", EMAIL_TAKEN);
        }

        User user = new User();
        user.setFirstname(request.firstname().trim());
        user.setSurname(request.surname().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void update(Integer id, UserUpdateRequest request) {
        User user = getOrThrow(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw BadRequestException.forField("email", EMAIL_TAKEN);
        }
        user.setFirstname(request.firstname().trim());
        user.setSurname(request.surname().trim());
        user.setEmail(email);
    }

    @Transactional
    public void delete(Integer id) {
        userRepository.delete(getOrThrow(id));
    }

    private User getOrThrow(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário " + id + " não encontrado"));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
