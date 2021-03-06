/*
 *
 *  *
 *  *  *
 *  *  *  * Copyright 2019-2020 the original author or authors.
 *  *  *  *
 *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  * You may obtain a copy of the License at
 *  *  *  *
 *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *
 *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  * See the License for the specific language governing permissions and
 *  *  *  * limitations under the License.
 *  *  *
 *  *
 *
 *
 */

package org.springdoc.data.rest.core;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.OpenAPIBuilder;

import org.springframework.web.method.HandlerMethod;

public class DataRestTagsBuilder {

	private OpenAPIBuilder openAPIBuilder;

	public DataRestTagsBuilder(OpenAPIBuilder openAPIBuilder) {
		this.openAPIBuilder = openAPIBuilder;
	}

	public void buildSearchTags(Operation operation, OpenAPI openAPI, HandlerMethod handlerMethod,
			Class<?> domainType) {
		buildTags(operation, openAPI, handlerMethod, domainType, "RepositorySearch", "Search");
	}

	public void buildEntityTags(Operation operation, OpenAPI openAPI, HandlerMethod handlerMethod,
			Class<?> domainType) {
		buildTags(operation, openAPI, handlerMethod, domainType, "RepositoryEntity", "Entity");
	}

	private void buildTags(Operation operation, OpenAPI openAPI, HandlerMethod handlerMethod,
			Class<?> domainType, String searchString, String replacement) {
		if (openAPIBuilder.isAutoTagClasses(operation)) {
			String tagName = handlerMethod.getBeanType().getSimpleName();
			if (domainType != null)
				tagName = tagName.replace(searchString, domainType.getSimpleName() + replacement);
			operation.addTagsItem(OpenAPIBuilder.splitCamelCase(tagName));
		}
		openAPIBuilder.buildTags(handlerMethod, operation, openAPI);
	}
}
