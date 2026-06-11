package com.codingshuttle.projects.airBnbApp.dto;

import com.codingshuttle.projects.airBnbApp.entity.enums.Gender;
import com.codingshuttle.projects.airBnbApp.entity.enums.Role;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Set<Role> roles;

}
