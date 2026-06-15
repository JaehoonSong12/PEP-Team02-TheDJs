package com.revature.todomanagement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long accountId;
    private String username;
}
