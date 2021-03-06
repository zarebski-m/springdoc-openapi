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

package org.springdoc.data.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.api.AbstractOpenApiResource;
import org.springdoc.core.RepositoryRestResourceProvider;
import org.springdoc.core.fn.RouterOperation;
import org.springdoc.data.rest.core.DataRestRouterOperationBuilder;

import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.MethodResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.mapping.SearchResourceMappings;
import org.springframework.data.rest.webmvc.BasePathAwareHandlerMapping;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping;
import org.springframework.data.rest.webmvc.alps.AlpsController;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.DelegatingHandlerMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

public class SpringRepositoryRestResourceProvider implements RepositoryRestResourceProvider {

	public static final String REPOSITORY_ENTITY_CONTROLLER = "org.springframework.data.rest.webmvc.RepositoryEntityController";

	public static final String REPOSITORY_SERACH_CONTROLLER = "org.springframework.data.rest.webmvc.RepositorySearchController";

	public static final String REPOSITORY_SCHEMA_CONTROLLER = "org.springframework.data.rest.webmvc.RepositorySchemaController";

	public static final String REPOSITORY_PROPERTY_CONTROLLER = "org.springframework.data.rest.webmvc.RepositoryPropertyReferenceController";

	private ResourceMappings mappings;

	private Repositories repositories;

	private Associations associations;

	private DelegatingHandlerMapping delegatingHandlerMapping;

	private DataRestRouterOperationBuilder dataRestRouterOperationBuilder;

	public SpringRepositoryRestResourceProvider(ResourceMappings mappings, Repositories repositories, Associations associations,
			DelegatingHandlerMapping delegatingHandlerMapping, DataRestRouterOperationBuilder dataRestRouterOperationBuilder) {
		this.mappings = mappings;
		this.repositories = repositories;
		this.associations = associations;
		this.delegatingHandlerMapping = delegatingHandlerMapping;
		this.dataRestRouterOperationBuilder = dataRestRouterOperationBuilder;
	}

	public List<RouterOperation> getRouterOperations(OpenAPI openAPI) {
		List<RouterOperation> routerOperationList = new ArrayList<>();
		List<HandlerMapping> handlerMappingList = delegatingHandlerMapping.getDelegates();
		boolean profileIsDone = false;
		for (Class<?> domainType : repositories) {
			ResourceMetadata resourceMetadata = mappings.getMetadataFor(domainType);
			if (resourceMetadata.isExported()) {
				for (HandlerMapping handlerMapping : handlerMappingList) {
					if (handlerMapping instanceof RepositoryRestHandlerMapping) {
						RepositoryRestHandlerMapping repositoryRestHandlerMapping = (RepositoryRestHandlerMapping) handlerMapping;
						Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = repositoryRestHandlerMapping.getHandlerMethods();
						Map<RequestMappingInfo, HandlerMethod> handlerMethodMapFiltered = handlerMethodMap.entrySet().stream()
								.filter(requestMappingInfoHandlerMethodEntry -> REPOSITORY_ENTITY_CONTROLLER.equals(requestMappingInfoHandlerMethodEntry
										.getValue().getBeanType().getName()) || REPOSITORY_PROPERTY_CONTROLLER.equals(requestMappingInfoHandlerMethodEntry
										.getValue().getBeanType().getName()))
								.filter(controller -> !AbstractOpenApiResource.isHiddenRestControllers(controller.getValue().getBeanType()))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a1, a2) -> a1));
						findControllers(routerOperationList, handlerMethodMapFiltered, resourceMetadata, domainType, openAPI);
					}
					 else if (handlerMapping instanceof BasePathAwareHandlerMapping) {
						BasePathAwareHandlerMapping beanBasePathAwareHandlerMapping = (BasePathAwareHandlerMapping) handlerMapping;
						Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = beanBasePathAwareHandlerMapping.getHandlerMethods();
						Map<RequestMappingInfo, HandlerMethod> handlerMethodMapFiltered = handlerMethodMap.entrySet().stream()
								.filter(requestMappingInfoHandlerMethodEntry -> REPOSITORY_SCHEMA_CONTROLLER.equals(requestMappingInfoHandlerMethodEntry
										.getValue().getBeanType().getName()))
								.filter(controller -> !AbstractOpenApiResource.isHiddenRestControllers(controller.getValue().getBeanType()))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a1, a2) -> a1));

						findControllers(routerOperationList, handlerMethodMapFiltered, resourceMetadata, domainType, openAPI);
						if (profileIsDone)
							continue;
						handlerMethodMapFiltered = handlerMethodMap.entrySet().stream()
								.filter(requestMappingInfoHandlerMethodEntry -> ProfileController.class.equals(requestMappingInfoHandlerMethodEntry
										.getValue().getBeanType()) || AlpsController.class.equals(requestMappingInfoHandlerMethodEntry
										.getValue().getBeanType()))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a1, a2) -> a1));
						findControllers(routerOperationList, handlerMethodMapFiltered, resourceMetadata, null, openAPI);
					}
				}
			}
			// search
			for (HandlerMapping handlerMapping : handlerMappingList) {
				if (handlerMapping instanceof RepositoryRestHandlerMapping) {
					RepositoryRestHandlerMapping repositoryRestHandlerMapping = (RepositoryRestHandlerMapping) handlerMapping;
					Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = repositoryRestHandlerMapping.getHandlerMethods();
					Map<RequestMappingInfo, HandlerMethod> handlerMethodMapFiltered = handlerMethodMap.entrySet().stream()
							.filter(requestMappingInfoHandlerMethodEntry -> REPOSITORY_SERACH_CONTROLLER.equals(requestMappingInfoHandlerMethodEntry
									.getValue().getBeanType().getName()))
							.filter(controller -> !AbstractOpenApiResource.isHiddenRestControllers(controller.getValue().getBeanType()))
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a1, a2) -> a1));
					ResourceMetadata metadata = associations.getMetadataFor(domainType);
					SearchResourceMappings searchResourceMappings = metadata.getSearchResourceMappings();
					if (searchResourceMappings.isExported()) {
						findSearchControllers(routerOperationList, handlerMethodMapFiltered, resourceMetadata, domainType, openAPI, searchResourceMappings);
					}
				}
			}
		}
		return routerOperationList;
	}

	private List<RouterOperation> findSearchControllers(List<RouterOperation> routerOperationList,
			Map<RequestMappingInfo, HandlerMethod> handlerMethodMap, ResourceMetadata resourceMetadata, Class<?> domainType, OpenAPI openAPI, SearchResourceMappings searchResourceMappings) {
		Stream<MethodResourceMapping> methodResourceMappingStream = searchResourceMappings.getExportedMappings();
		methodResourceMappingStream.forEach(methodResourceMapping -> dataRestRouterOperationBuilder.buildSearchRouterOperationList(
				routerOperationList, handlerMethodMap, resourceMetadata, domainType, openAPI, methodResourceMapping));
		return routerOperationList;
	}


	private List<RouterOperation> findControllers
			(List<RouterOperation> routerOperationList,
					Map<RequestMappingInfo, HandlerMethod> handlerMethodMap, ResourceMetadata resourceMetadata,
					Class<?> domainType, OpenAPI openAPI) {
		dataRestRouterOperationBuilder.buildEntityRouterOperationList(routerOperationList, handlerMethodMap, resourceMetadata,
				domainType, openAPI);
		return routerOperationList;
	}

}