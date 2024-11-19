package com.example.loginpage.model;


import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class UpdateHistoryDTO {
    private String oldName;
    private String newName;
    private String updatedBy;
    private String updatedAt;

    public UpdateHistoryDTO(String oldName, String newName, String updatedBy, String updatedAt) {
        this.oldName = oldName;
        this.newName = newName;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }
}
