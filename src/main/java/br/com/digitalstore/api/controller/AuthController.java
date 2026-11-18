package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.controller.docs.AuthControllerDocs;
import br.com.digitalstore.api.dto.user.TokenRequest;
import br.com.digitalstore.api.dto.user.TokenResponse;
import br.com.digitalstore.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/user")
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return authService.authenticate(request);
    }
}
