package com.google.api.server.spi.config.model;

import java.util.Objects;

public class ApiParameterBoundaries {
	private final Long min;
	private final Long max;
	
	private final String decimalMin;
	private final String decimalMax;
	
	public ApiParameterBoundaries(Long min, Long max, String decimalMin, String decimalMax) {
		this.min = min;
		this.max = max;
		this.decimalMin = decimalMin;
		this.decimalMax = decimalMax;
	}
	
	public ApiParameterBoundaries(ApiParameterBoundaries original) {
		this(original.min, original.max, original.decimalMin, original.decimalMax);
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
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ApiParameterBoundaries that = (ApiParameterBoundaries) o;
		return min.equals(that.min) && max.equals(that.max) && decimalMin.equals(that.decimalMin) && decimalMax.equals(that.decimalMax);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(min, max, decimalMin, decimalMax);
	}
}
