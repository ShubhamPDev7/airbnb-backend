package com.codingshuttle.projects.airBnbApp.dto;

import lombok.Data;

@Data
public class VerifyEmailDto {
    private String email;
    private String code;
}
