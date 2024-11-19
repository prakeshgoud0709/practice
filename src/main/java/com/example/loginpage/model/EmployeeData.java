package com.example.loginpage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeData {

	private String name;
    private String updatedBy;
//    private String newname;
    private String updatedAt;
//    private String state; // Assuming this is the same as `statename`
//    private List<String> districts;
}
