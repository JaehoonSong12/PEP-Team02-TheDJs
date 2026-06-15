package com.revature.todomanagement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubtaskResponse {
    private Long id;
    private Long todoId;
    private String title;
    private boolean completed;
}
