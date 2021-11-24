package com.google.api.server.spi.config.annotationreader;

import static com.google.api.server.spi.config.annotationreader.AnnotationUtil.getNamedParameter;
import static com.google.api.server.spi.config.annotationreader.AnnotationUtil.getNullableParameter;
import static com.google.api.server.spi.config.annotationreader.AnnotationUtil.getParameterAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;

public final class ApiConfigAnnotations {
	
	private Annotation parameterName;
	private Annotation description;
	private Annotation nullable;
	private Annotation defaultValue;
	private Annotation pattern;
	private Annotation min;
	private Annotation max;
	private Annotation decimalMin;
	private Annotation decimalMax;
	private Annotation size;
	
	public ApiConfigAnnotations(Method method, int parameterIndex, Map<String, Class<? extends Annotation>> annotationTypes) {
		this.parameterName = getNamedParameter(method, parameterIndex, annotationTypes.get("Named"));
		this.description = getParameterAnnotation(method, parameterIndex, annotationTypes.get("Description"));
		this.nullable = getNullableParameter(method, parameterIndex, annotationTypes.get("Nullable"));
		this.defaultValue = getParameterAnnotation(method, parameterIndex, annotationTypes.get("DefaultValue"));
		this.pattern = getParameterAnnotation(method, parameterIndex, annotationTypes.get("Pattern"));
		this.min = getParameterAnnotation(method, parameterIndex, annotationTypes.get("Min"));
		this.max = getParameterAnnotation(method, parameterIndex, annotationTypes.get("Max"));
		this.decimalMin = getParameterAnnotation(method, parameterIndex, annotationTypes.get("DecimalMin"));
		this.decimalMax = getParameterAnnotation(method, parameterIndex, annotationTypes.get("DecimalMax"));
		this.size = getParameterAnnotation(method, parameterIndex, annotationTypes.get("Size"));
	}
	
	public ApiConfigAnnotations(AnnotatedElement annotatedElement, Map<String, Class<? extends Annotation>> annotationTypes) {
		this.parameterName = annotatedElement.getAnnotation(annotationTypes.get("Named"));
		this.description = annotatedElement.getAnnotation(annotationTypes.get("Description"));
		this.nullable = annotatedElement.getAnnotation(annotationTypes.get("Nullable"));
		this.defaultValue = annotatedElement.getAnnotation(annotationTypes.get("DefaultValue"));
		this.pattern = annotatedElement.getAnnotation(annotationTypes.get("Pattern"));
		this.min = annotatedElement.getAnnotation(annotationTypes.get("Min"));
		this.max = annotatedElement.getAnnotation(annotationTypes.get("Max"));
		this.decimalMin = annotatedElement.getAnnotation(annotationTypes.get("DecimalMin"));
		this.decimalMax = annotatedElement.getAnnotation(annotationTypes.get("DecimalMax"));
		this.size = annotatedElement.getAnnotation(annotationTypes.get("Size"));
	}
	
	public Annotation getParameterName() {
		return parameterName;
	}
	
	public Annotation getDescription() {
		return description;
	}
	
	public Annotation getNullable() {
		return nullable;
	}
	
	public Annotation getDefaultValue() {
		return defaultValue;
	}
	
	public Annotation getPattern() {
		return pattern;
	}
	
	public Annotation getMin() {
		return min;
	}
	
	public Annotation getMax() {
		return max;
	}
	
	public Annotation getDecimalMin() {
		return decimalMin;
	}
	
	public Annotation getDecimalMax() {
		return decimalMax;
	}
	
	public Annotation getSize() {
		return size;
	}
}
