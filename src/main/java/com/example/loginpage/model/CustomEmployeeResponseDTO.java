package com.example.loginpage.model;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomEmployeeResponseDTO {
    private Employee employee;
    private List<UpdateHistoryDTO> history;

    public CustomEmployeeResponseDTO(Employee employee, List<UpdateHistoryDTO> history) {
        this.employee = employee;
        this.history = history;
    }
}
