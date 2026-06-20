package com.revature.todomanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskResponse {

    private UUID id;
    private UUID taskId;
    private String title;
    private boolean completed;
}
