package br.com.digitalstore.api.service;

import br.com.digitalstore.api.dto.user.TokenRequest;
import br.com.digitalstore.api.dto.user.TokenResponse;
import br.com.digitalstore.api.exception.BadRequestException;
import br.com.digitalstore.api.repository.UserRepository;
import br.com.digitalstore.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponse authenticate(TokenRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .map(user -> new TokenResponse(jwtService.generate(user)))
                .orElseThrow(() -> new BadRequestException(INVALID_CREDENTIALS));
    }
}
