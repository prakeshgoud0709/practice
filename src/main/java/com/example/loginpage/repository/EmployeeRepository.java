package com.example.loginpage.repository;



import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.loginpage.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
	
	Optional<Employee> findByEmail(String email);
	  
	  @Query(value = "SELECT email FROM employee WHERE email = :email", nativeQuery = true)
	  Optional<Employee> getByEmail(@Param("email") String email);
	  
	  // Find all employees created by a specific admin
	    @Query("SELECT e FROM Employee e WHERE e.createdBy = :email ")
	    List<Employee> findEmployeesByCreatedBy(@Param("email") String email);
	  
	  List<Employee> findByRole(String role);
	  
	  Optional<Employee> findByEmailAndPassword(String email, String password);
	  
	  Employee findByName(String name);
	  
	  @Query("SELECT e.name FROM Employee e ORDER BY e.updatedAt DESC")
	  List<String> findLatestUpdatedNames();
}

