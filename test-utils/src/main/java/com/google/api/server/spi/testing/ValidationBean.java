package com.google.api.server.spi.testing;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.google.api.server.spi.config.ApiResourceProperty;

public class ValidationBean {

	@Pattern(regexp = "^[0-9]{2}$") @Size(min = 2, max = 2)
	@ApiResourceProperty(name = "myPatternTest")
	private String patternTest;
	@Min(2) @Max(6)
	private Long minMaxTest;
	@DecimalMin(value = "3.4", inclusive = false) @DecimalMax("4.5")
	private Double decimalMinMaxTest;
	@Size(min = 3)
	private List<String> arrayTest;
	
	public String getPatternTest() {
		return patternTest;
	}
	
	public void setPatternTest(String patternTest) {
		this.patternTest = patternTest;
	}
	
	public Long getMinMaxTest() {
		return minMaxTest;
	}
	
	public void setMinMaxTest(Long minMaxTest) {
		this.minMaxTest = minMaxTest;
	}
	
	public Double getDecimalMinMaxTest() {
		return decimalMinMaxTest;
	}
	
	public void setDecimalMinMaxTest(Double decimalMinMaxTest) {
		this.decimalMinMaxTest = decimalMinMaxTest;
	}
	
	public List<String> getArrayTest() {
		return arrayTest;
	}
	
	public void setArrayTest(List<String> arrayTest) {
		this.arrayTest = arrayTest;
	}
}
