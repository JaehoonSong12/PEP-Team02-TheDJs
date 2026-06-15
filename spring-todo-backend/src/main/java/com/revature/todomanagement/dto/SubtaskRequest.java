package com.revature.todomanagement.dto;

import lombok.Data;

@Data
public class SubtaskRequest {
    private String title;
    private boolean completed;
}
