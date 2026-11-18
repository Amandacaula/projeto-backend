package br.com.digitalstore.api.dto.user;

import br.com.digitalstore.api.model.User;

public record UserResponse(Integer id, String firstname, String surname, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFirstname(), user.getSurname(), user.getEmail());
    }
}
