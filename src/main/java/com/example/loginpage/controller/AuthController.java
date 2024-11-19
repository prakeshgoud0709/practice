package com.example.loginpage.controller;


import java.time.LocalDateTime;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import com.example.loginpage.error.ApiError;
import com.example.loginpage.exception.UserNotFoundException;
import com.example.loginpage.model.CustomEmployeeResponseDTO;
import com.example.loginpage.model.District;
import com.example.loginpage.model.Employee;
import com.example.loginpage.model.EmployeeDTO;
import com.example.loginpage.model.EmployeeSignin;
import com.example.loginpage.model.Role;
import com.example.loginpage.model.State;
import com.example.loginpage.model.StateDistrictRequest;
import com.example.loginpage.model.UpdateHistoryDTO;
import com.example.loginpage.model.UpdatePasswordRequest;
import com.example.loginpage.repository.DistrictRepository;
import com.example.loginpage.repository.EmployeeRepository;
import com.example.loginpage.repository.StateRepository;
import com.example.loginpage.service.AuthService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
	
	@Autowired
    private AuthenticationManager authenticationManager; // Inject AuthenticationManager

	@Autowired
	private EmployeeRepository employeeRepository;
	
    @Autowired
    private AuthService authService;
    
    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;
    
    @GetMapping("/{name}")
    public ResponseEntity<Employee> getEmployeeByName(@PathVariable("name") String name) {
        Employee employee = authService.getByName(name);
        return ResponseEntity.ok(employee);
    }
    
    
    @PostMapping("/createOrUpdate")
    public ResponseEntity<State> createOrUpdateState(@RequestBody StateDistrictRequest request) {
        State updatedState = authService.createOrUpdateStateWithDistricts(request);
        return ResponseEntity.ok(updatedState);
    }
    
    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistrictsByState(@RequestParam("statename") String statename) {
        List<String> districts = districtRepository.findDistrictsByStateName(statename);
        if (districts.isEmpty()) {
            return ResponseEntity.noContent().build(); // Optional: return no content if no districts found
        }
        return ResponseEntity.ok(districts);
    }

    
    @GetMapping("/states")
    public ResponseEntity<List<State>> getAllStates() {
        List<State> states = stateRepository.findAll();
        return ResponseEntity.ok(states);
    }

    
    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // Get existing session, don't create a new one
        return session != null && 
               session.getAttribute("email") != null && 
               session.getAttribute("role") != null;
    }

    @GetMapping("/signup")
    public ModelAndView signUpPage() {
    	return new ModelAndView("signup"); // This will resolve to signup.html
    }
    
    @GetMapping("/signin")
    public ModelAndView signInPage(HttpServletRequest request) {
        // If the user is already authenticated, redirect to the appropriate welcome page
        if (isAuthenticated(request)) {
            String role = (String) request.getSession().getAttribute("role");
            System.out.println("User is authenticated with role: " + role); // Debug log
            if ("ADMIN".equals(role)) {
                return new ModelAndView("redirect:/api/auth/adminwelcome");
            } else {
            	
                return new ModelAndView("redirect:/api/auth/employeewelcome");
            }
        }
        System.out.println("User is not authenticated, showing sign-in page."); // Debug log
        return new ModelAndView("signin"); // This will resolve to signin.html
    }
    
//    @GetMapping("/adminwelcome")
//    public ModelAndView adminWelcomePage(HttpServletRequest request) {
//        String adminEmail = (String) request.getSession().getAttribute("email");
//
//        // Check admin email
//        System.out.println("Session email: " + adminEmail);
//
//        if (adminEmail == null) {
//            return new ModelAndView("redirect:/api/auth/signin");
//        }
//
//        // Fetch admin details
//        Employee admin = authService.findByEmail(adminEmail);
//        if (admin == null) {
//            return new ModelAndView("redirect:/api/auth/signin");
//        }
//
//        // Fetch all employees
//        List<Employee> allEmployees = authService.getAllEmployees();
//     // Store the adminName in the session
//        request.getSession().setAttribute("adminName", admin.getName());
//        // Debug log to check the size of employees fetched
//        System.out.println("Total number of employees fetched: " + allEmployees.size());
//
//        ModelAndView modelAndView = new ModelAndView("adminwelcome");
//        modelAndView.addObject("adminRole", admin.getRole());
//        modelAndView.addObject("adminName", admin.getName());
//        modelAndView.addObject("employees", allEmployees); // Pass all employees to the view
//
//        return modelAndView;
//    }
    
    
    @GetMapping("/adminwelcome")
    public ModelAndView adminWelcomePage(HttpServletRequest request) {
        String adminEmail = (String) request.getSession().getAttribute("email");
        System.out.println("Session email: " + adminEmail);

        // Redirect to sign-in if email is null or empty
        if (adminEmail == null || adminEmail.isEmpty()) {
            System.out.println("No email in session, redirecting to sign-in.");
            return new ModelAndView("redirect:/api/auth/signin");
        }

        // Fetch the admin details
        Employee admin = authService.findByEmail(adminEmail);
        if (admin == null) {
            System.out.println("Admin not found, redirecting to sign-in.");
            return new ModelAndView("redirect:/api/auth/signin");
        }

        // Get all employees
        List<Employee> allEmployees = authService.getAllEmployeesAsMap();
//        List<Employee> allEmployees = authService.getAllEmployees();
        System.out.println("Total number of employees fetched: " + (allEmployees != null ? allEmployees.size() : 0));
        request.getSession().setAttribute("adminName", admin.getName());

        // Prepare ModelAndView with data for the view
        ModelAndView modelAndView = new ModelAndView("adminwelcome");
        modelAndView.addObject("adminRole", admin.getRole());
        modelAndView.addObject("adminName", admin.getName());
        modelAndView.addObject("employees", allEmployees);

        // Debugging output
        System.out.println("Admin Role: " + admin.getRole());
        System.out.println("Admin Name: " + admin.getName());

        return modelAndView;
    }




//
//    @GetMapping("/he")
//    public List<EmployeeData> get() {
//    	return authService.getAllEmployeesAsMap();
//    }

   
    

	@GetMapping("/employeewelcome")
    public ModelAndView employeeWelcomePage(HttpServletRequest request) {
        String employeeEmail = (String) request.getSession().getAttribute("email");
        
        // Ensure the employee email is present in the session
        if (employeeEmail == null) {
            return new ModelAndView("redirect:/api/auth/signin"); // Redirect to sign-in if not authenticated
        }
        
        ModelAndView modelAndView = new ModelAndView("employeewelcome");
        
        // Fetch employee details
        Employee employee = authService.findByEmail(employeeEmail);
        if (employee == null) {
            return new ModelAndView("redirect:/api/auth/signin"); // Redirect if employee not found
        }
        
        // Add employee details to the model
        modelAndView.addObject("employee", employee);
        
        return modelAndView;
    }

    @GetMapping("/createemployee")
    public ModelAndView createEmployeePage() {
        // This method returns the view for the employee creation page
        return new ModelAndView("createEmployee"); // Resolves to createEmployee.html
    }
    
    
 // Get employee by ID
 // Get employee by ID
    @GetMapping("/updatename/{id}")
    public ModelAndView updateNamePage(@PathVariable("id") Long id) {
        System.out.println("Fetching employee with ID: " + id); // Debugging line
        Employee employee = authService.findById(id);
        
        if (employee != null) {
            System.out.println("Employee found: " + employee.getName());
            ModelAndView modelAndView = new ModelAndView("updatename");
            modelAndView.addObject("employee", employee);
            return modelAndView;
        } else {
            System.out.println("Employee not found, redirecting to admin welcome.");
            return new ModelAndView("redirect:/api/auth/adminwelcome");
        }
    }





    // Update employee name
    @PostMapping("/updatename/{id}")
    public ResponseEntity<String> updateEmployee(@PathVariable("id") Long id, @RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        try {
            String newName = requestBody.get("name");

            // Retrieve the admin's name from the session
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session not found.");
            }

            String adminName = (String)  request.getSession().getAttribute("adminName");
            if (adminName == null || adminName.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin name not found in session.");
            }

            // Update employee name with the admin's name
            authService.updateEmployeeName(id, newName, adminName);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error.");
        }
    }

//    @GetMapping("/employee-details")
//    public ModelAndView getEmployeeDetails() {
//        List<String> employeeDetails = new ArrayList<>();
//        
//        // Get all employees from the repository
//        List<Employee> employees = employeeRepository.findAll();
//        
//        // Iterate over the employees and build the employee details
//        for (Employee employee : employees) {
//            List<String> names = Collections.singletonList(employee.getName());  // Assuming there's one name per employee
//            List<String> updatedByList = Collections.singletonList(employee.getUpdatedBy());  // List of users who updated the employee
//            List<String> updatedAtList = Collections.singletonList(employee.getUpdatedAt());  // List of timestamps of updates
//            
//            StringBuilder employeeInfo = new StringBuilder();
//            
//            // Ensure all lists have the same size to avoid IndexOutOfBoundsException
//            int size = Math.min(names.size(), Math.min(updatedByList.size(), updatedAtList.size()));
//            for (int i = 0; i < size; i++) {
//                employeeInfo.append(names.get(i))
//                             .append(" updatedBy ")
//                             .append(updatedByList.get(i))
//                             .append(" at ")
//                             .append(updatedAtList.get(i))
//                             .append("<br>");
//            }
//
//            // Add the formatted employee info to the list
//            employeeDetails.add(employeeInfo.toString());
//        }
//        
//        // Pass the employeeDetails list to the view
//        ModelAndView modelAndView = new ModelAndView("employee-details");
//        modelAndView.addObject("employeeDetails", employeeDetails);
//        return modelAndView;
//    }

    
//    @GetMapping("/employee/{id}")
//    public ModelAndView getEmployeeDetails(@PathVariable("id") Long id) {
//        // Fetch employee details by id
//        var employee = authService.getEmployeeDetails(id);
//        
//        // Add employee details to the model
//        ModelAndView modelAndView = new ModelAndView("employeeDetails");
//        modelAndView.addObject("employee", employee);
//        
//        return modelAndView; // Returns the view for employee details (employeeDetails.html)
//    }

    
//    @GetMapping("/employee/{id}")
//    public Map<String, Object> getEmployeeDetails(@PathVariable("id") Long id) {
////        ModelAndView modelAndView = new ModelAndView("adminWelcome");  // Send data to adminWelcome page
//
//        // Fetch employee details from the service
//        Map<String, Object> employeeDetails = authService.getEmployeeDetails(id);
//
////        if (employeeDetails.containsKey("error")) {
////            modelAndView.addObject("error", employeeDetails.get("error"));
////        } else {
////            modelAndView.addObject("employee", employeeDetails);
////        }
//
//        return employeeDetails;
//    }
    
 // POST request for fetching employee details
 // POST request for employee details (assuming you're sending employee ID in request body for some reason)
    // POST request for employee update history by ID (returns only history)
 // POST request for employee update history by ID (returns a List<List<String>> for history)
    @PostMapping("/employeeDetails/{id}")
    public ResponseEntity<List<List<String>>> getEmployeeDetailsPost(@PathVariable("id") Long id) {
        try {
            // Fetch employee update history (only history) from the service
            List<List<String>> history = authService.getEmployeeDetails(id);

            // Return the history as a list of lists containing Strings with 200 OK status
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (RuntimeException e) {
            // Return error message with 404 Not Found if employee not found or history incomplete
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Return generic error message with 500 Internal Server Error
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // GET request for employee update history by ID (view-based response)
    @GetMapping("/employeeDetails/{id}")
    public ModelAndView getEmployeeDetails(@PathVariable("id") Long id) {
        ModelAndView modelAndView = new ModelAndView("employeeDetails"); // A view to show only history

        try {
            // Fetch employee update history from the service (only history now)
            List<List<String>> history = authService.getEmployeeDetails(id);

            // If history is empty (meaning no updates found)
            if (history.isEmpty()) {
                modelAndView.setViewName("errorPage"); // Navigate to an error page
                modelAndView.addObject("message", "No update history found for Employee with ID: " + id);
            } else {
                modelAndView.addObject("history", history); // Add update history to the model
            }
        } catch (RuntimeException e) {
            // Handle errors such as employee not found
            modelAndView.setViewName("errorPage");
            modelAndView.addObject("message", "Update Info: " + e.getMessage());
        } catch (Exception e) {
            // Handle other potential errors
            modelAndView.setViewName("errorPage");
            modelAndView.addObject("message", "An unexpected error occurred.");
        }

        return modelAndView;
    }





    @GetMapping("/update-password")
    public ModelAndView updatePasswordPage() {
        return new ModelAndView("updatePassword"); // This will resolve to signup.html
    }
    
    @GetMapping("/logout")
    public ModelAndView logoutPage() {
    	return new ModelAndView("logout"); // This will resolve to signup.html
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody EmployeeDTO employeeDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Collect error messages from all fields
            List<String> errors = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors); // Return detailed error messages
        }
        try {
            authService.signUp(employeeDTO);
            return ResponseEntity.ok("Signup successful.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
//    @PostMapping("/signin")
//    public ModelAndView signIn(@Valid @ModelAttribute EmployeeSignin employeeSignin, HttpServletRequest request) {
//        ModelAndView modelAndView = new ModelAndView();
//
//        try {
//            // Authenticate the user
//            Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(employeeSignin.getEmail(), employeeSignin.getPassword())
//            );
//
//            // Set authentication in the security context
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//
//            // Store email in session
//            request.getSession().setAttribute("email", employeeSignin.getEmail());
//
//            // Retrieve the user role
//            ResponseEntity<?> roleResponse = authService.getUserRole(employeeSignin.getEmail());
//            if (roleResponse.getStatusCode() == HttpStatus.OK) {
//                String role = (String) roleResponse.getBody();
//                request.getSession().setAttribute("role", role);
//
//                // Redirect based on role
//                if ("ADMIN".equals(role)) {
//                    modelAndView.setViewName("redirect:/api/auth/adminwelcome");
//                } else if ("EMPLOYEE".equals(role)) {
//                    modelAndView.setViewName("redirect:/api/auth/employeewelcome");
//                } else {
//                    // Handle any other roles or unexpected cases
//                    modelAndView.setViewName("redirect:/error");
//                }
//            } else {
//                // Handle role retrieval failure
//                modelAndView.addObject("message", "Failed to retrieve user role.");
//                modelAndView.setViewName("signin"); // Redirect to sign-in page with error
//            }
//
//        } catch (BadCredentialsException e) {
//            // Handle invalid credentials
//            modelAndView.addObject("message", "Invalid email or password.");
//            modelAndView.setViewName("signin"); // Redirect back to sign-in page with error
//        } catch (Exception e) {
//            // Handle any other exceptions
//            modelAndView.addObject("message", "An error occurred during sign-in: " + e.getMessage());
//            modelAndView.setViewName("signin"); // Redirect back to sign-in page with error
//        }
//
//        return modelAndView;
//    }
    
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@Valid @RequestBody EmployeeSignin employeeSignin, HttpServletRequest request) {
        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(employeeSignin.getEmail(), employeeSignin.getPassword())
            );

            // Set authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Store email in session
            request.getSession().setAttribute("email", employeeSignin.getEmail());

            // Retrieve the user role
            ResponseEntity<?> roleResponse = authService.getUserRole(employeeSignin.getEmail());
            if (roleResponse.getStatusCode() == HttpStatus.OK) {
                String role = (String) roleResponse.getBody();
                request.getSession().setAttribute("role", role);

                // Create a redirect URL based on the role
                String redirectUrl;
                if ("ADMIN".equals(role)) {
                    redirectUrl = "/api/auth/adminwelcome"; // URL for admin
                } else if ("EMPLOYEE".equals(role)) {
                    redirectUrl = "/api/auth/employeewelcome"; // URL for employee
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unexpected user role.");
                }

                // Return success response with redirect URL
                return ResponseEntity.ok(Map.of("message", "Sign-in successful.", "redirectUrl", redirectUrl));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to retrieve user role.");
            }

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during sign-in: " + e.getMessage());
        }
    }


    
    @PostMapping("/createemployee")
    public ResponseEntity<?> createEmployee(@RequestBody Map<String, String> employee, HttpServletRequest request) {
        // Get admin's name or email from the session
        String createdBy = (String) request.getSession().getAttribute("email");

        System.out.println("Received employee data: " + employee);

        // Extract employee details
        String name = employee.get("name");
        String email = employee.get("email");
        String password = employee.get("password");
        String roleString = employee.get("role") != null ? employee.get("role").toUpperCase() : null;
        String stateName = employee.get("state");
        String districtsValue = employee.get("districts");

        // Validate required fields with specific messages
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Name is required."));
        }
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required."));
        }
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Password is required."));
        }
        if (roleString == null || roleString.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Role is required."));
        }
        if (stateName == null || stateName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "State is required."));
        }

        // Validate districts
        if (districtsValue == null || districtsValue.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Districts are required."));
        }

        // Proceed with employee creation
        List<String> districtNames = Arrays.asList(districtsValue.split(","));

        try {
            // Convert roleString to Role enum
            Role role = Role.valueOf(roleString);

            // Check if the state exists
            Optional<State> existingStateOpt = stateRepository.findByStatename(stateName);
            State state;

            if (existingStateOpt.isPresent()) {
                state = existingStateOpt.get(); // Fetch the existing state
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "State does not exist: " + stateName));
            }

            // Validate the districts against the state's available districts
            List<String> availableDistricts = state.getDistricts()
                                                   .stream()
                                                   .map(District::getDistrictname)
                                                   .collect(Collectors.toList());

            for (String districtName : districtNames) {
                if (!availableDistricts.contains(districtName.trim())) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid district: " + districtName + " for state: " + state.getStatename()));
                }
            }

            // Set createdAt to current time
            LocalDateTime createdAt = LocalDateTime.now();
          
            // Call the service to create a new employee
            Employee createdEmployee = authService.createEmployee(name, email, password, role, createdBy, createdAt, state, districtNames);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Employee created successfully!", "employee", createdEmployee));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid role provided."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    




    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest) {
        String result = authService.updatePassword(updatePasswordRequest.getEmail(), updatePasswordRequest.getNewPassword());
        
        if ("Password updated successfully.".equals(result)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result); // Handle "Employee not found" case
        }
    }
    

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // Invalidate the session to log the user out
        request.getSession().invalidate();
        return ResponseEntity.ok("Logout successful."); // Respond with a success message
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + ex.getMessage());
    }
    
    public class EmployeeNotFoundException extends RuntimeException {
        public EmployeeNotFoundException(String message) {
            super(message);
        }
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<String> handleEmployeeNotFoundException(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Method Not Allowed: " + ex.getMessage());
    }
    
   
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex) {
        ApiError apiError = new ApiError(ex.getMessage(), Collections.emptyList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex, WebRequest request) {
        // Log the exception message
        System.err.println("Exception: " + ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
