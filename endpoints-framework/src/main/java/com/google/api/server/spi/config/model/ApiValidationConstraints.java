package com.google.api.server.spi.config.model;

import java.util.Objects;
import java.util.stream.Stream;

public class ApiValidationConstraints {
	
	private final String pattern;
	
	private final Long min;
	private final Long max;
	
	private final String decimalMin;
	private final String decimalMax;
	
	private final Boolean decimalMinInclusive;
	private final Boolean decimalMaxInclusive;
	
	private final Integer minSize;
	private final Integer maxSize;

	public ApiValidationConstraints(String pattern, Long min, Long max, String decimalMin, String decimalMax,
			Boolean decimalMinInclusive, Boolean decimalMaxInclusive, Integer minSize, Integer maxSize) {
		this.pattern = pattern;
		this.min = min;
		this.max = max;
		this.decimalMin = decimalMin;
		this.decimalMax = decimalMax;
		this.decimalMinInclusive = decimalMinInclusive;
		this.decimalMaxInclusive = decimalMaxInclusive;
		this.minSize = minSize;
		this.maxSize = maxSize;
	}
	
	public ApiValidationConstraints(ApiValidationConstraints original) {
		this(original.pattern, original.min, original.max, original.decimalMin, original.decimalMax, 
				original.decimalMinInclusive, original.decimalMaxInclusive, original.minSize, original.maxSize);
	}
	
	public String getPattern() {
		return pattern;
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
	
	public Integer getMinSize() {
		return minSize;
	}
	
	public Integer getMaxSize() {
		return maxSize;
	}
	
	public boolean isEmpty() {
		return Stream.of(pattern, min, max, decimalMin, decimalMax, decimalMinInclusive, decimalMaxInclusive, minSize, maxSize)
				.allMatch(Objects::isNull);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ApiValidationConstraints that = (ApiValidationConstraints) o;
		return Objects.equals(pattern, that.pattern) && Objects.equals(min, that.min) && Objects.equals(max, that.max) && Objects.equals(decimalMin, that.decimalMin) && Objects.equals(decimalMax, that.decimalMax) && Objects.equals(decimalMinInclusive, that.decimalMinInclusive) && Objects.equals(decimalMaxInclusive, that.decimalMaxInclusive) && Objects.equals(minSize, that.minSize) && Objects.equals(maxSize, that.maxSize);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(pattern, min, max, decimalMin, decimalMax, decimalMinInclusive, decimalMaxInclusive, minSize, maxSize);
	}
}
