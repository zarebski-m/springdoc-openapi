/*
 *
 *  *
 *  *  * Copyright 2019-2020 the original author or authors.
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package org.springdoc.core;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.apache.commons.lang3.StringUtils;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestPart;

import static org.springdoc.core.SpringDocAnnotationsUtils.mergeSchema;


public class RequestBodyBuilder {

	private final GenericParameterBuilder parameterBuilder;

	public RequestBodyBuilder(GenericParameterBuilder parameterBuilder) {
		super();
		this.parameterBuilder = parameterBuilder;
	}

	public Optional<RequestBody> buildRequestBodyFromDoc(
			io.swagger.v3.oas.annotations.parameters.RequestBody requestBody, RequestBody requestBodyOp, MethodAttributes methodAttributes,
			Components components, JsonView jsonViewAnnotation) {
		String[] classConsumes = methodAttributes.getClassConsumes();
		String[] methodConsumes = methodAttributes.getMethodConsumes();

		if (requestBody == null)
			return Optional.empty();
		RequestBody requestBodyObject = new RequestBody();
		boolean isEmpty = true;

		if (StringUtils.isNotBlank(requestBody.ref())) {
			requestBodyObject.set$ref(requestBody.ref());
			return Optional.of(requestBodyObject);
		}

		if (StringUtils.isNotBlank(requestBody.description())) {
			requestBodyObject.setDescription(requestBody.description());
			isEmpty = false;
		}

		if (requestBody.required()) {
			requestBodyObject.setRequired(requestBody.required());
			isEmpty = false;
		}
		if (requestBody.extensions().length > 0) {
			Map<String, Object> extensions = AnnotationsUtils.getExtensions(requestBody.extensions());
			extensions.forEach(requestBodyObject::addExtension);
			isEmpty = false;
		}

		if (requestBody.content().length > 0)
			isEmpty = false;

		if (isEmpty)
			return Optional.empty();

		buildResquestBodyContent(requestBody, requestBodyOp, methodAttributes, components, jsonViewAnnotation, classConsumes, methodConsumes, requestBodyObject);

		return Optional.of(requestBodyObject);
	}

	private void buildResquestBodyContent(io.swagger.v3.oas.annotations.parameters.RequestBody requestBody, RequestBody requestBodyOp, MethodAttributes methodAttributes, Components components, JsonView jsonViewAnnotation, String[] classConsumes, String[] methodConsumes, RequestBody requestBodyObject) {
		Optional<Content> optionalContent = AnnotationsUtils
				.getContent(requestBody.content(), classConsumes == null ? new String[0] : classConsumes,
						methodConsumes == null ? new String[0] : methodConsumes, null, components, jsonViewAnnotation);
		if (requestBodyOp == null)
			optionalContent.ifPresent(requestBodyObject::setContent);
		else {
			Content existingContent = requestBodyOp.getContent();
			if (optionalContent.isPresent() && existingContent != null) {
				Content newContent = optionalContent.get();
				if (methodAttributes.isMethodOverloaded()) {
					Arrays.stream(methodAttributes.getMethodProduces()).filter(mediaTypeStr -> (newContent.get(mediaTypeStr) != null)).forEach(mediaTypeStr -> {
						if (newContent.get(mediaTypeStr).getSchema() != null)
							mergeSchema(existingContent, newContent.get(mediaTypeStr).getSchema(), mediaTypeStr);
					});
					requestBodyObject.content(existingContent);
				}
				else
					requestBodyObject.content(newContent);
			}
		}
	}

	public Optional<RequestBody> buildRequestBodyFromDoc(io.swagger.v3.oas.annotations.parameters.RequestBody requestBody,
			MethodAttributes methodAttributes, Components components) {
		return this.buildRequestBodyFromDoc(requestBody, null, methodAttributes,
				components, null);
	}

	public Optional<RequestBody> buildRequestBodyFromDoc(io.swagger.v3.oas.annotations.parameters.RequestBody requestBody,
			MethodAttributes methodAttributes, Components components, JsonView jsonViewAnnotation) {
		return this.buildRequestBodyFromDoc(requestBody, null, methodAttributes,
				components, jsonViewAnnotation);
	}

	public Optional<RequestBody> buildRequestBodyFromDoc(
			io.swagger.v3.oas.annotations.parameters.RequestBody requestBody, RequestBody requestBodyOp, MethodAttributes methodAttributes,
			Components components) {
		return this.buildRequestBodyFromDoc(requestBody, requestBodyOp, methodAttributes,
				components, null);
	}

	public void calculateRequestBodyInfo(Components components, MethodAttributes methodAttributes,
			ParameterInfo parameterInfo, RequestBodyInfo requestBodyInfo) {
		RequestBody requestBody = requestBodyInfo.getRequestBody();
		MethodParameter methodParameter = parameterInfo.getMethodParameter();
		// Get it from parameter level, if not present
		if (requestBody == null) {
			io.swagger.v3.oas.annotations.parameters.RequestBody requestBodyDoc = methodParameter.getParameterAnnotation(io.swagger.v3.oas.annotations.parameters.RequestBody.class);
			requestBody = this.buildRequestBodyFromDoc(requestBodyDoc, methodAttributes, components).orElse(null);
		}

		RequestPart requestPart = methodParameter.getParameterAnnotation(RequestPart.class);
		String paramName = null;
		if (requestPart != null)
			paramName = StringUtils.defaultIfEmpty(requestPart.value(), requestPart.name());
		paramName = StringUtils.defaultIfEmpty(paramName, parameterInfo.getpName());
		parameterInfo.setpName(paramName);

		requestBody = buildRequestBody(requestBody, components, methodAttributes, parameterInfo,
				requestBodyInfo);
		requestBodyInfo.setRequestBody(requestBody);
	}

	private RequestBody buildRequestBody(RequestBody requestBody, Components components,
			MethodAttributes methodAttributes,
			ParameterInfo parameterInfo, RequestBodyInfo requestBodyInfo) {
		if (requestBody == null) {
			requestBody = new RequestBody();
			requestBodyInfo.setRequestBody(requestBody);
		}

		if (requestBody.getContent() == null) {
			Schema<?> schema = parameterBuilder.calculateSchema(components, parameterInfo, requestBodyInfo,
					methodAttributes.getJsonViewAnnotationForRequestBody());
			buildContent(requestBody, methodAttributes, schema);
		}
		else {
			Schema<?> schema = parameterBuilder.calculateSchema(components, parameterInfo, requestBodyInfo,
					methodAttributes.getJsonViewAnnotationForRequestBody());
			mergeContent(requestBody, methodAttributes, schema);
		}
		return requestBody;
	}

	private void mergeContent(RequestBody requestBody, MethodAttributes methodAttributes, Schema<?> schema) {
		Content content = requestBody.getContent();
		buildContent(requestBody, methodAttributes, schema, content);
	}

	private void buildContent(RequestBody requestBody, MethodAttributes methodAttributes, Schema<?> schema) {
		Content content = new Content();
		buildContent(requestBody, methodAttributes, schema, content);
	}

	private void buildContent(RequestBody requestBody, MethodAttributes methodAttributes, Schema<?> schema, Content content) {
		for (String value : methodAttributes.getMethodConsumes()) {
			io.swagger.v3.oas.models.media.MediaType mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
			mediaTypeObject.setSchema(schema);
			if (content.get(value) != null) {
				mediaTypeObject.setExample(content.get(value).getExample());
				mediaTypeObject.setExamples(content.get(value).getExamples());
				mediaTypeObject.setEncoding(content.get(value).getEncoding());
			}
			content.addMediaType(value, mediaTypeObject);
		}
		requestBody.setContent(content);
	}
}
