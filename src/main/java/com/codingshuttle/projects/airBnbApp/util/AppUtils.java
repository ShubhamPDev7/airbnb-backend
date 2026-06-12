package com.codingshuttle.projects.airBnbApp.util;

import com.codingshuttle.projects.airBnbApp.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.stream.Collectors;

public class AppUtils {
    public static User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Title-cases every word in the input, e.g. "new york" -> "New York",
     * "NAVI MUMBAI" -> "Navi Mumbai". Collapses repeated whitespace and trims.
     * Returns the input unchanged if it's null or blank.
     */
    public static String toTitleCase(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        return Arrays.stream(input.trim().split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
