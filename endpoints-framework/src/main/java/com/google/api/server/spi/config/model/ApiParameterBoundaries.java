package com.google.api.server.spi.config.model;

import java.util.Objects;

public class ApiParameterBoundaries {

	private final Long min;
	private final Long max;

	private final String decimalMin;
	private final String decimalMax;
	
	private final Boolean decimalMinInclusive;
	private final Boolean decimalMaxInclusive;

	public ApiParameterBoundaries(Long min, Long max, String decimalMin, String decimalMax, 
			Boolean decimalMinInclusive, Boolean decimalMaxInclusive) {
		this.min = min;
		this.max = max;
		this.decimalMin = decimalMin;
		this.decimalMax = decimalMax;
		this.decimalMinInclusive = decimalMinInclusive;
		this.decimalMaxInclusive = decimalMaxInclusive;
	}
	
	public ApiParameterBoundaries(ApiParameterBoundaries original) {
		this(original.min, original.max, original.decimalMin, original.decimalMax, 
				original.decimalMinInclusive, original.decimalMaxInclusive);
	}
	
	public Long getMin() {
		return min;
	}
	
	public Long getMax() {
		return max;
	}
	
	public String getDecimalMin() {
		return decimalMin;
	}
	
	public String getDecimalMax() {
		return decimalMax;
	}
	
	public Boolean getDecimalMinInclusive() {
		return decimalMinInclusive;
	}
	
	public Boolean getDecimalMaxInclusive() {
		return decimalMaxInclusive;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ApiParameterBoundaries that = (ApiParameterBoundaries) o;
		return Objects.equals(min, that.min) && Objects.equals(max, that.max) && Objects.equals(decimalMin, that.decimalMin) && Objects.equals(decimalMax, that.decimalMax) && Objects.equals(decimalMinInclusive, that.decimalMinInclusive) && Objects.equals(decimalMaxInclusive, that.decimalMaxInclusive);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(min, max, decimalMin, decimalMax, decimalMinInclusive, decimalMaxInclusive);
	}
}
