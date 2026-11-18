package br.com.digitalstore.api.controller;

import br.com.digitalstore.api.controller.docs.UserControllerDocs;
import br.com.digitalstore.api.dto.user.UserCreateRequest;
import br.com.digitalstore.api.dto.user.UserResponse;
import br.com.digitalstore.api.dto.user.UserUpdateRequest;
import br.com.digitalstore.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/v1/user")
public class UserController implements UserControllerDocs {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Integer id) {
        return userService.findById(id);
    }

    @Override
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(URI.create("/v1/user/" + created.id())).body(created);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @Valid @RequestBody UserUpdateRequest request) {
        userService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
