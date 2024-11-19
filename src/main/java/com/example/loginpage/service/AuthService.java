package com.example.loginpage.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.loginpage.model.CustomEmployeeResponseDTO;
import com.example.loginpage.model.District;
import com.example.loginpage.model.Employee;
import com.example.loginpage.model.EmployeeDTO;
import com.example.loginpage.model.EmployeeSignin;
import com.example.loginpage.model.Role;
import com.example.loginpage.model.State;
import com.example.loginpage.model.StateDistrictRequest;
import com.example.loginpage.model.UpdateHistoryDTO;
import com.example.loginpage.repository.DistrictRepository;
import com.example.loginpage.repository.EmployeeRepository;
import com.example.loginpage.repository.StateRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class AuthService {
	
	// Temporary storage to hold latest update information
    private final Map<Long, String> lastUpdateInfo = new HashMap<>();
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private StateRepository stateRepository;
    
    @Autowired
    private DistrictRepository districtRepository;
    
    public List<State> getAllStates() {
        return stateRepository.findAll();
    }

    public List<String> getDistrictsByStateName(String statename) {
        return districtRepository.findDistrictsByStateName(statename);
    }
    
   


    @Transactional
    public void signUp(EmployeeDTO employeeDTO) {
        // Log the incoming employeeDTO
        System.out.println("Signing up employee: " + employeeDTO);

        // Check if the email already exists
        if (employeeRepository.findByEmail(employeeDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        // Encode the password
        String encodedPassword = passwordEncoder.encode(employeeDTO.getPassword());

        // Check if the State already exists by name
        Optional<State> existingStateOpt = stateRepository.findByStatename(employeeDTO.getStatename());
        State state;

        if (existingStateOpt.isPresent()) {
            state = existingStateOpt.get(); // Fetch the existing state
        } else {
            throw new RuntimeException("State does not exist: " + employeeDTO.getStatename());
        }

        // Validate the districts against the state's available districts
        List<String> availableDistricts = state.getDistricts()
                                                 .stream()
                                                 .map(District::getDistrictname)
                                                 .collect(Collectors.toList());

        for (String districtName : employeeDTO.getDistricts()) {
            if (!availableDistricts.contains(districtName)) {
                throw new RuntimeException("Invalid district: " + districtName + " for state: " + state.getStatename());
            }
        }

        // Create new Employee instance
        Employee employee = new Employee();
        employee.setName(employeeDTO.getName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setRole(employeeDTO.getRole());
        employee.setPassword(encodedPassword);
        employee.setStatename(state.getStatename()); // Use state name from persisted entity
        employee.setDistricts(employeeDTO.getDistricts()); // Set the list of district names

        // Save the new employee
        employeeRepository.save(employee);
        System.out.println("Employee saved: " + employee);
    }


    
    public ResponseEntity<?> getUserRole(String email) {
        // Fetch the employee as an Optional
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
        
        // Check if the employee is present and return the role
        if (employeeOpt.isPresent()) {
            String role = employeeOpt.get().getRole().name(); // Assuming 'getRole()' returns an enum
            return ResponseEntity.ok(role); // Return 200 OK with role in the response
        } else {
            String errorMessage = "Employee not found with email: " + email;
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage); // Return 404 NOT FOUND with error message
        }
    }



    // SignIn method
    public String signIn(EmployeeSignin employeeSignin) {
        // Check if the employee exists
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(employeeSignin.getEmail());

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            // Check if the password matches
            if (passwordEncoder.matches(employeeSignin.getPassword(), employee.getPassword())) {
                return "Sign-in successful for " + employee.getName();
            } else {
                // If password does not match
                return "Invalid  password";
            }
        } else {
            // If employee is not found
            return "Invalid email or password";
        }
    }
    
    @Transactional
    public State createOrUpdateStateWithDistricts(StateDistrictRequest request) {
        // Retrieve existing state by name
        Optional<State> existingStateOpt = stateRepository.findByStatename(request.getStateName());
        State state;

        if (existingStateOpt.isPresent()) {
            // If the state exists, get it
            state = existingStateOpt.get();
        } else {
            // If the state doesn't exist, create a new one
            state = new State();
            state.setStatename(request.getStateName());
            // Save the new state to make it persistent before using it in districts
            stateRepository.save(state);
        }

        // Create a list to hold the districts to be added/updated
        List<District> updatedDistricts = new ArrayList<>();

        for (String districtName : request.getDistricts()) {
            // Check if the district already exists for the current state
            Optional<District> existingDistrictOpt = districtRepository.findByDistrictnameAndState(districtName, state);

            if (existingDistrictOpt.isPresent()) {
                // If it exists, we can add it to the list without modification
                updatedDistricts.add(existingDistrictOpt.get());
            } else {
                // If it doesn't exist, create a new district
                District newDistrict = new District();
                newDistrict.setDistrictname(districtName);
                newDistrict.setState(state); // Associate with the state
                updatedDistricts.add(newDistrict);
            }
        }

        // Update the state with the districts, replacing old ones
        state.getDistricts().clear(); // Clear existing districts if you want to replace
        state.getDistricts().addAll(updatedDistricts); // Add updated districts

        // Save the state, which will also save the new districts if cascading is set
        stateRepository.save(state);

        return state; // Return the updated state object
    }

    
    
 // Retrieve employees created by the specified admin email
    public List<Employee> getEmployeesCreatedByAdmin(String email) {
        return employeeRepository.findEmployeesByCreatedBy(email);
    }

    public Employee findByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found with email: " + email));
    }
    public Optional<Employee> getByEmail(String email) {
    	return employeeRepository.getByEmail(email);
    }

 // Method to fetch all employees
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll(); // Fetch all employees from the database
    }
    
 
    //
    public List<String> getLatestUpdatedNames() {
        return employeeRepository.findLatestUpdatedNames(); // Fetch only the latest updated names
    }
    
    
    public List<Employee> getAllEmployeesAsMap() {
        List<Employee> employees = employeeRepository.findAll();
        
        // Initialize the list to store EmployeeData objects
        List<Employee> employeeList = new ArrayList<>();

        for (Employee employee : employees) {
            // Get the full name from the employee object
            String fullName = employee.getName();
            String lastName=getLastName(fullName);
            employee.setName(lastName);
           
            // Create EmployeeData object with the last name and other employee information
      
                employeeList.add(employee);

            // Add the EmployeeData object to the list
            
        }
        return employeeList; // Return the list of EmployeeData objects
    }
    
 // Method to get formatted employee update details
//    public List<String> getEmployeeDetails() {
//        // Assume this list is fetched from the database
//        List<Employee> employees = employeeRepository.findAll(); 
//        
//        // List to store the formatted strings with employee info
//        List<String> employeeDetails = new ArrayList<>();
//        
//        // Iterate through each employee
//        for (Employee employee : employees) {
//            // Get all names, updatedBy, and updatedAt
//            List<String> names = employee.getNames();
//            List<String> updatedByList = employee.getUpdatedBy();
//            List<String> updatedAtList = employee.getUpdatedAt();
//            
//            // Initialize a string to hold the formatted details
//            StringBuilder employeeInfo = new StringBuilder();
//            
//            // Iterate over all elements of names, updatedBy, and updatedAt
//            int size = Math.min(names.size(), Math.min(updatedByList.size(), updatedAtList.size()));
//            
//            for (int i = 0; i < size; i++) {
//                // Concatenate each element in the format: Name updatedBy UpdatedName at UpdatedTime
//                employeeInfo.append(names.get(i))
//                             .append(" updatedBy ")
//                             .append(updatedByList.get(i))
//                             .append(" at ")
//                             .append(updatedAtList.get(i))
//                             .append("<br>"); // Use <br> for line break in HTML
//            }
//
//            // Add the formatted string to the employeeDetails list
//            employeeDetails.add(employeeInfo.toString());
//        }
//        
//        return employeeDetails;
//    }
    
//    public List<EmployeeData> getAllEmployeesAsMap() {
//        return employeeRepository.findAll().stream()
//            .map(employee -> new EmployeeData(
//                getLastName(employee.getName()), // Extract last name
//                employee.getEmail(),
//                employee.getRole(),
//                employee.getCreatedBy()
//            ))
//            .collect(Collectors.toList());
//    }

    // Helper method to extract the last name
    public String getLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return ""; // Return empty string if fullName is null or empty
        }
        // Split by whitespace or commas and get the last segment
        String[] nameParts = fullName.trim().split("[\\s,]+");
        return nameParts[nameParts.length - 1]; // Return the last segment
    }
    
//    public String getData(String name, String fulludatedBy, String fullupdatedAt) {
//    	if(name==null ||name.trim().isEmpty()) {
//    		return "";    	}
//    	String[] nameparts= name.trim().
//    	return null;
//    }
    
   
   

//    public Map<String, Object> getEmployeeDetails(Long id) {
//        Map<String, Object> response = new HashMap<>();
//
//        Employee employee = employeeRepository.findById(id).orElse(null);
//
//        if (employee == null) {
//            response.put("error", "Employee not found with ID: " + id);
//            return response;
//        }
//
//        // Assuming 'name' is stored as a comma-separated string, process it
//        String names = employee.getName();
//        String updatedBy = employee.getUpdatedBy();
//        String updatedAt = employee.getUpdatedAt();
//
//        // Split the 'name' field by commas
//        String[] nameList = names.split(",");
//        String[] updatedByList = updatedBy.split(",");
//        String[] updatedAtList = updatedAt.split(",");
//
//        // Build the response map with records
//        for (int i = 0; i < nameList.length; i++) {
//            Map<String, String> record = new HashMap<>();
//
//            // Ensure you don't go out of bounds by checking that i + 1 is valid
//            String oldName = nameList[i];  // Get the old name from the list
//            String newName = (i + 1 < nameList.length) ? nameList[i + 1] : oldName;  // Avoid out-of-bounds access
//
//            // Safe access for 'updatedBy' and 'updatedAt' as well
//            String updatedByValue = (i < updatedByList.length) ? updatedByList[i] : "Unknown";  // Handle missing updatedBy
//            String updatedAtValue = (i < updatedAtList.length) ? updatedAtList[i] : "Unknown";  // Handle missing updatedAt
//
//            record.put("oldname", oldName);
//            record.put("newname", newName);
//            record.put("updatedBy", updatedByValue);
//            record.put("updatedAt", updatedAtValue);
//
//            // Adding the record to the response map
//            response.put("record" + i, record);
//        }
//
//        return response;
//    }
    
    
    //21
//    public List<EmployeeData> getEmployeeDetails(Long id) {
//        Employee employee = employeeRepository.findById(id).orElse(null);
//
//        if (employee == null) {
//            return Collections.emptyList();  // Return empty list if employee is not found
//        }
//
//        List<EmployeeData> detailsList = new ArrayList<>();
//        String[] nameList = employee.getName().split(",");
//        String[] updatedByList = employee.getUpdatedBy().split(",");
//        String[] updatedAtList = employee.getUpdatedAt().split(",");
//
//        for (int i = 0; i < nameList.length; i++) {
//            EmployeeData data = new EmployeeData();
//            data.setOldname(nameList[i]);
//            data.setNewname(i + 1 < nameList.length ? nameList[i + 1] : nameList[i]);
//            data.setUpdatedBy(i < updatedByList.length ? updatedByList[i] : "Unknown");
//            data.setUpdatedAt(i < updatedAtList.length ? updatedAtList[i] : "Unknown");
//
//            detailsList.add(data);
//        }
//
//        return detailsList;
//    }
    
//    public List<UpdateHistoryDTO> getEmployeeDetails(Long id) {
//        // Fetch employee from the database by ID
//        Employee employee = employeeRepository.findById(id).orElse(null);
//        
//        if (employee == null) {
//            throw new RuntimeException("Employee not found");
//        }
//
//        // Processing history as before
//        String nameHistoryStr = employee.getName();
//        String updatedByHistoryStr = employee.getUpdatedBy();
//        String updatedAtHistoryStr = employee.getUpdatedAt();
//
//        List<String> nameHistory = Arrays.asList(nameHistoryStr.split(","));
//        List<String> updatedByHistory = Arrays.asList(updatedByHistoryStr.split(","));
//        List<String> updatedAtHistory = Arrays.asList(updatedAtHistoryStr.split(","));
//
//        List<UpdateHistoryDTO> history = new ArrayList<>();
//        int size = Math.min(nameHistory.size(), Math.min(updatedByHistory.size(), updatedAtHistory.size()));
//
//        for (int i = 0; i < size; i++) {
//            UpdateHistoryDTO updateHistoryDTO = new UpdateHistoryDTO(
//                    nameHistory.get(i),
//                    (i + 1 < nameHistory.size()) ? nameHistory.get(i + 1) : "No update",
//                    updatedByHistory.get(i) != null ? updatedByHistory.get(i) : "Not updated",
//                    updatedAtHistory.get(i) != null ? updatedAtHistory.get(i) : "Not updated"
//            );
//            history.add(updateHistoryDTO);
//        }
//
//        // Returning only the history list
//        return history;
//    }
    
    public List<List<String>> getEmployeeDetails(Long id) {
        // Fetch employee from the database by ID
        Employee employee = employeeRepository.findById(id).orElse(null);

        if (employee == null) {
            throw new RuntimeException("Employee with ID " + id + " not found.");
        }

        // Processing history as before
        String nameHistoryStr = employee.getName();          // String of historical names, comma-separated
        String updatedByHistoryStr = employee.getUpdatedBy(); // String of update by users, comma-separated
        String updatedAtHistoryStr = employee.getUpdatedAt(); // String of update timestamps, comma-separated

        // Ensure all history strings are not null or empty
        if (nameHistoryStr == null || updatedByHistoryStr == null || updatedAtHistoryStr == null ||
            nameHistoryStr.isEmpty() || updatedByHistoryStr.isEmpty() || updatedAtHistoryStr.isEmpty()) {
            throw new RuntimeException("Not Updated Employee with This Name " + employee.getName());
        }

        // Convert the comma-separated strings into lists
        List<String> nameHistory = Arrays.asList(nameHistoryStr.split(","));
        List<String> updatedByHistory = Arrays.asList(updatedByHistoryStr.split(","));
        List<String> updatedAtHistory = Arrays.asList(updatedAtHistoryStr.split(","));

        // Initialize the list to hold the history
        List<List<String>> history = new ArrayList<>();
        int size = Math.min(nameHistory.size(), Math.min(updatedByHistory.size(), updatedAtHistory.size()));

        // Loop through the history and build lists
        for (int i = 0; i < size; i++) {
            List<String> updateHistory = new ArrayList<>();

            // Collect the old name, new name, updated by, updated at values
            String oldName = nameHistory.get(i); // previous name
            String newName = (i + 1 < nameHistory.size()) ? nameHistory.get(i + 1) : "No update"; // updated name
            String updatedBy = updatedByHistory.get(i) != null ? updatedByHistory.get(i) : "Not updated"; // updated by
            String updatedAt = updatedAtHistory.get(i) != null ? updatedAtHistory.get(i) : "Not updated"; // updated at

            // Add the values to the updateHistory list
            updateHistory.add( oldName); // Old name
            updateHistory.add( newName); // New name
            updateHistory.add( updatedBy); // Updated by
            updateHistory.add( updatedAt); // Updated at

            // Add the update history to the final list
            history.add(updateHistory);
        }

        // Returning the history list
        return history;
    }





//    public Map<String, Object> getEmployeeDetails(Long id) {
//        // Fetch employee from the database by ID
//        Employee employee = employeeRepository.findById(id).orElse(null);
//        
//        Map<String, Object> response = new HashMap<>();
//
//        if (employee == null) {
//            response.put("error", "Employee not found");
//            return response;
//        }
//
//        // Retrieve the name and update history from the employee object
//        String nameHistoryStr = employee.getName();
//        String updatedByHistoryStr = employee.getUpdatedBy();
//        String updatedAtHistoryStr = employee.getUpdatedAt();
//
//        // Split the comma-separated strings into lists
//        List<String> nameHistory = Arrays.asList(nameHistoryStr.split(","));
//        List<String> updatedByHistory = Arrays.asList(updatedByHistoryStr.split(","));
//        List<String> updatedAtHistory = Arrays.asList(updatedAtHistoryStr.split(","));
//
//        // Prepare the update history
//        List<Map<String, String>> records = new ArrayList<>();
//
//        // Ensure all lists have the same size
//        int size = Math.min(nameHistory.size(), Math.min(updatedByHistory.size(), updatedAtHistory.size()));
//
//        for (int i = 0; i < size; i++) { // Loop starts from the most recent update
//            Map<String, String> record = new HashMap<>();
//            record.put("oldname", nameHistory.get(i));  // The old name
//            record.put("newname", nameHistory.get(i+1));  // The new name
//            record.put("updatedBy", updatedByHistory.get(i));  // Who updated the name
//            record.put("updatedAt", updatedAtHistory.get(i));  // When the name was updated
//
//            // Add the record to the list of history
//            records.add(record);
//        }
//
//        // Add the history records to the response map
//        response.put("employeeRecords"+1, records);
//        return response;
//    }

    
//    public Map<String, Object> getEmployeeDetails(Long id) {
//        // Fetch employee from the database by ID
//        Employee employee = employeeRepository.findById(id).orElse(null);
//        
//        Map<String, Object> response = new HashMap<>();
//
//        if (employee == null) {
//            response.put("error", "Employee not found");
//            return response;
//        }
//
//        // Retrieve the name and update history from the employee object
//        String nameHistoryStr = employee.getName();
//        String updatedByHistoryStr = employee.getUpdatedBy();
//        String updatedAtHistoryStr = employee.getUpdatedAt();
//
//        // Split the comma-separated strings into lists
//        List<String> nameHistory = Arrays.asList(nameHistoryStr.split(","));
//        List<String> updatedByHistory = Arrays.asList(updatedByHistoryStr.split(","));
//        List<String> updatedAtHistory = Arrays.asList(updatedAtHistoryStr.split(","));
//
//        // Prepare the update history
//        List<Map<String, String>> records = new ArrayList<>();
//
//        // Ensure all lists have the same size
//        int size = Math.min(nameHistory.size(), Math.min(updatedByHistory.size(), updatedAtHistory.size()));
//
//        for (int i = size - 1; i >= 1; i--) { // Loop starts from the most recent update
//            Map<String, String> record = new HashMap<>();
//            record.put("oldname", nameHistory.get(i));  // The old name
//            record.put("newname", nameHistory.get(i - 1));  // The new name
//            record.put("updatedBy", updatedByHistory.get(i));  // Who updated the name
//            record.put("updatedAt", updatedAtHistory.get(i));  // When the name was updated
//
//            // Add the record to the list of history
//            records.add(record);
//        }
//
//        // Add the history records to the response map
//        response.put("employeeRecords"+1, records);
//        return response;
//    }



  // Find employee by ID
 public Employee findById(Long id) {
     return employeeRepository.findById(id).orElse(null);
 }

 // Update employee name
 public void updateEmployeeName(Long id, String newName, String adminName) {
	    Employee employee = employeeRepository.findById(id)
	            .orElseThrow(() -> new EntityNotFoundException("Employee not found with ID: " + id));

	    // Get the old name and get the current time
	    String oldName = employee.getName();
	    String name=oldName+","+newName;
	    String oldupdatedBy=employee.getUpdatedBy();
	    String newupdateBy=adminName+","+oldupdatedBy;
	    LocalDateTime updatedTime = LocalDateTime.now();  // Current update time
	    String oldUpdatedAtHistory = employee.getUpdatedAt();  // Previous update history
	    
	    // Add the new updated time to the history
	    String newUpdatedAtHistory = (oldUpdatedAtHistory != null ? oldUpdatedAtHistory + "," : "") + updatedTime.toString();

	    // Update the employee's name and set the updatedAtHistory field
	    employee.setName(name);
	    employee.setUpdatedAt(newUpdatedAtHistory);  // Save multiple update times as a comma-separated string
	    employee.setUpdatedBy(newupdateBy);    // Set the admin who made the update

	    // Save the updated employee entity
	    employeeRepository.save(employee);

	    System.out.println("Updated name from '" + oldName + "' to '" + newName + "'");
	    System.out.println("Updated at history: " + newUpdatedAtHistory);
	}
 
 	
    //**************************
    // Update an employee's password
    public String updatePassword(String email, String newPassword) {
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            employee.setPassword(passwordEncoder.encode(newPassword)); // Encode new password
            employeeRepository.save(employee);
            return "Password updated successfully.";
        }

        return "Employee not found.";
    }
    
    public List<Employee> findByRole(String role) {
        return employeeRepository.findByRole(role);
    }
   
 // AuthService.java
    // Update the method to accept String for role instead of Role enum
  //Updated createEmployee method to handle state and districts
    public Employee createEmployee(String name, String email, String password, Role role, String createdBy, LocalDateTime createdAt, State state, List<String> districts) {
        // Encode the password
        String encodedPassword = passwordEncoder.encode(password);

        // Create a new Employee instance
        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(email);
        employee.setPassword(encodedPassword);
        employee.setRole(role); // Set the role as an enum
        employee.setCreatedBy(createdBy); // Store the admin's name or email
        employee.setCreatedAt(createdAt);
        employee.setStatename(state.getStatename()); // Set the state name from the persisted entity
        employee.setDistricts(districts); // Set the list of district names

        // Log the employee details before saving
        System.out.println("Creating employee: " + employee);

        // Save the employee to the database
        return employeeRepository.save(employee);
    }

    
    





 // This method should decode the token and retrieve the admin information (e.g., admin ID or username)
    public String getAdminByToken(String token) {
        // Validate the token
        if (isValidToken(token)) {
            // Extract the admin's username or ID from the token
            return extractAdminFromToken(token);
        }
        return null; // Return null if the token is invalid
    }

    private boolean isValidToken(String token) {
        // Implement your token validation logic here
        return token != null && token.startsWith("Bearer ");
    }

    private String extractAdminFromToken(String token) {
        // This is a placeholder implementation; you should use a proper JWT library.
        // Assuming the token format is: "Bearer adminUser"
        return token.substring(7); // Extracts "adminUser" part
    }
    
    public Employee getByName(String name) {
        Employee employee = employeeRepository.findByName(name);
        if (employee == null) {
            throw new EntityNotFoundException("Employee not found with name: " + name);
        }
        return employee;
    }


}

