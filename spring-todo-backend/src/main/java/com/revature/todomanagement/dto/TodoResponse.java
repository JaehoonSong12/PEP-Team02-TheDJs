package com.revature.todomanagement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TodoResponse {
    private Long todoId;
    private Long accountId;
    private String title;
    private String description;
    private boolean completed;
}
