package com.hubspot.baragon.service.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.managers.BaragonAuthManager;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;
import com.hubspot.baragon.service.platform.Environment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * How the API reads requests and writes answers: JSON only, through the object mapper the API has
 * always been serialised with; the key a request carries checked once its path is known; path and
 * query parameters read as the API reads them; and the answers framed as the API has always framed
 * them.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
  private final ObjectMapper objectMapper;
  private final BaragonConfiguration config;
  private final BaragonAuthManager authManager;
  private final AuthConfiguration authConfiguration;

  public WebConfiguration(
    @javax.inject.Named(
      com.hubspot.baragon.service.ObjectMapperConfiguration.API_OBJECT_MAPPER
    ) ObjectMapper objectMapper,
    BaragonConfiguration config,
    BaragonAuthManager authManager,
    AuthConfiguration authConfiguration
  ) {
    this.objectMapper = objectMapper;
    this.config = config;
    this.authManager = authManager;
    this.authConfiguration = authConfiguration;
  }

  /**
   * The converters the API reads and writes entities with, and only those: the ones the framework
   * would add of its own write and read what the API never has.
   */
  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.clear();
    // an entity the handler built as bytes is sent as those bytes, whatever media type it is named by
    converters.add(new org.springframework.http.converter.ByteArrayHttpMessageConverter());
    converters.add(new JsonConverter(objectMapper));
    converters.add(new PlainTextConverter());
  }

  /**
   * The API answers for every error itself, or leaves it to the container: no answer of the web
   * framework's own stands in for one of the API's.
   */
  @Override
  public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
    resolvers.clear();
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    StrictNumberConverters.register(registry);
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.strategies(Arrays.asList(new LenientAcceptStrategy()));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new AuthKeyInterceptor(authKeys()));
  }

  /** The settings the service sets the cross-origin filter up with: none of its own. */
  private static final javax.servlet.FilterConfig NO_SETTINGS = new javax.servlet.FilterConfig() {

    @Override
    public String getFilterName() {
      return "Cross Origin Request Filter";
    }

    @Override
    public javax.servlet.ServletContext getServletContext() {
      return null;
    }

    @Override
    public String getInitParameter(String name) {
      return null;
    }

    @Override
    public java.util.Enumeration<String> getInitParameterNames() {
      return java.util.Collections.emptyEnumeration();
    }
  };

  @Bean
  public AuthKeys authKeys() {
    return new AuthKeys(authManager, authConfiguration);
  }

  /** How the values of a request's parameters are read, wherever a handler reads one. */
  @Bean
  public ParameterBinding parameterBinding() {
    return new ParameterBinding();
  }

  @Bean
  public RouteTable routeTable(RequestMappingHandlerMapping mapping) {
    return mapping instanceof BaragonHandlerMapping
      ? ((BaragonHandlerMapping) mapping).routes()
      : new RouteTable(mapping);
  }

  @Bean
  public ErrorEntities errorEntities(RequestMappingHandlerAdapter adapter) {
    return new ErrorEntities(adapter.getMessageConverters());
  }

  @Bean
  public HandlerExceptionResolver baragonExceptionResolver(
    ErrorEntities entities,
    RouteTable routes,
    BaragonExceptionNotifier notifier
  ) {
    return new BaragonExceptionResolver(entities, routes, notifier);
  }

  /** How the entity a handler is given is read, before any of the framework's own ways of reading it. */
  @Bean
  public EntityArgumentProcessor entityArgumentProcessor(
    RequestMappingHandlerAdapter adapter,
    ContentNegotiationManager negotiation
  ) {
    final EntityArgumentProcessor processor = new EntityArgumentProcessor(
      adapter.getMessageConverters(),
      negotiation
    );
    final List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers = new ArrayList<>(
      adapter.getArgumentResolvers()
    );
    resolvers.add(0, processor);
    adapter.setArgumentResolvers(resolvers);
    return processor;
  }

  @Bean
  public ResponseReturnValueHandler responseReturnValueHandler(
    RequestMappingHandlerAdapter adapter,
    ContentNegotiationManager negotiation
  ) {
    final ResponseReturnValueHandler handler = new ResponseReturnValueHandler(
      adapter.getMessageConverters(),
      negotiation
    );
    final List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(
      adapter.getReturnValueHandlers()
    );
    handlers.add(0, handler);
    handlers.add(
      1,
      new OptionalReturnValueHandler(adapter.getMessageConverters(), negotiation)
    );
    handlers.add(2, new NoContentReturnValueHandler());
    handlers.add(3, entityArgumentProcessor(adapter, negotiation));
    adapter.setReturnValueHandlers(handlers);
    return handler;
  }

  /**
   * The filter that answers a request from another origin. The service builds it and sets it up itself, and
   * the container sets it up again when the context starts, so what it was set up with it holds twice.
   */
  @Bean
  public FilterRegistrationBean<CrossOriginFilter> crossOriginFilter() {
    final CrossOriginFilter filter = new CrossOriginFilter();
    try {
      filter.init(NO_SETTINGS);
    } catch (javax.servlet.ServletException e) {
      throw new IllegalStateException(e);
    }
    final FilterRegistrationBean<CrossOriginFilter> registration = new FilterRegistrationBean<>(
      filter
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.setEnabled(config.isEnableCorsFilter());
    return registration;
  }

  @Bean
  public FilterRegistrationBean<RefusedMethodFilter> refusedMethodFilter() {
    final FilterRegistrationBean<RefusedMethodFilter> registration = new FilterRegistrationBean<>(
      new RefusedMethodFilter()
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<RequestReadingFilter> requestReadingFilter() {
    final FilterRegistrationBean<RequestReadingFilter> registration = new FilterRegistrationBean<>(
      new RequestReadingFilter()
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<EntityBufferingFilter> entityBufferingFilter() {
    final FilterRegistrationBean<EntityBufferingFilter> registration = new FilterRegistrationBean<>(
      new EntityBufferingFilter()
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<OptionsFilter> optionsFilter(
    RouteTable routes,
    ErrorEntities entities
  ) {
    final FilterRegistrationBean<OptionsFilter> registration = new FilterRegistrationBean<>(
      new OptionsFilter(routes, authKeys(), entities)
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 4);
    return registration;
  }

  /** The API's own dispatcher, so that no path is answered by the servlet rather than by the API. */
  @Bean
  public DispatcherServlet dispatcherServlet(WebMvcProperties properties) {
    final BaragonDispatcherServlet servlet = new BaragonDispatcherServlet();
    servlet.setDispatchOptionsRequest(properties.isDispatchOptionsRequest());
    servlet.setDispatchTraceRequest(properties.isDispatchTraceRequest());
    servlet.setThrowExceptionIfNoHandlerFound(properties.isThrowExceptionIfNoHandlerFound());
    servlet.setEnableLoggingRequestDetails(properties.isLogRequestDetails());
    return servlet;
  }

  /** The handler mapping of the API, which chooses a handler as the API has always chosen one. */
  @Bean
  public WebMvcRegistrations baragonMvcRegistrations() {
    return new WebMvcRegistrations() {

      @Override
      public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new BaragonHandlerMapping();
      }
    };
  }

  @Bean
  public JettyConfiguration jettyConfiguration() {
    return new JettyConfiguration(config);
  }

  @Bean
  public AdminServer adminServer(Environment environment) {
    return new AdminServer(config, environment);
  }
}
