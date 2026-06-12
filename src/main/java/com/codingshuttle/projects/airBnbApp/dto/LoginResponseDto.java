package com.codingshuttle.projects.airBnbApp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private UserDto user;

    // Carried internally for the httpOnly cookie — never sent in response body
    @JsonIgnore
    private String refreshToken;

    // 2-arg constructor so existing code still compiles
    public LoginResponseDto(String accessToken, UserDto user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}