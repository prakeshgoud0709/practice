package com.example.loginpage.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.loginpage.model.District;
import com.example.loginpage.model.State;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {
    
    // This method uses Spring Data JPA method naming conventions
    // It searches for districts based on the state name.
	 Optional<District> findByDistrictnameAndState(String districtname, State state);
    
    // This custom query retrieves district names based on the state name.
	@Query("SELECT d.districtname FROM District d WHERE d.state.statename = :statename")
    List<String> findDistrictsByStateName(@Param("statename") String statename);
}
