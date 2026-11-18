package br.com.digitalstore.api.service;

import br.com.digitalstore.api.dto.user.UserCreateRequest;
import br.com.digitalstore.api.dto.user.UserUpdateRequest;
import br.com.digitalstore.api.exception.BadRequestException;
import br.com.digitalstore.api.exception.NotFoundException;
import br.com.digitalstore.api.model.User;
import br.com.digitalstore.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static UserCreateRequest createRequest(String email, String password, String confirm) {
        return new UserCreateRequest(" Ana ", " Silva ", email, password, confirm);
    }

    @Test
    void createRejeitaSenhasDiferentesSemConsultarOBanco() {
        assertThatThrownBy(() -> userService.create(createRequest("ana@mail.com", "123456", "654321")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(UserService.PASSWORDS_DIFFER);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createRejeitaEmailDuplicadoAposNormalizar() {
        when(userRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(createRequest(" ANA@Mail.com ", "123456", "123456")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(UserService.EMAIL_TAKEN);
    }

    @Test
    void createNormalizaDadosEGravaHashDaSenha() {
        when(userRepository.existsByEmail("ana@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1);
            return u;
        });

        var response = userService.create(createRequest("ANA@mail.com", "123456", "123456"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getFirstname()).isEqualTo("Ana");
        assertThat(saved.getSurname()).isEqualTo("Silva");
        assertThat(saved.getEmail()).isEqualTo("ana@mail.com");
        assertThat(saved.getPassword()).isEqualTo("hash");
        assertThat(response.id()).isEqualTo(1);
    }

    @Test
    void updateRejeitaEmailDeOutroUsuario() {
        User existing = new User();
        existing.setId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("outro@mail.com", 1)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1, new UserUpdateRequest("A", "B", "outro@mail.com")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(UserService.EMAIL_TAKEN);
    }

    @Test
    void updateLancaNotFoundParaUsuarioInexistente() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99, new UserUpdateRequest("A", "B", "a@mail.com")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteLancaNotFoundParaUsuarioInexistente() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Usuário 99 não encontrado");
    }
}
