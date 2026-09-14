package com.maraujo.couponapi.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
  private static Schema<Object> problemSchema() {
    var fieldViolation =
        new Schema<>()
            .type("object")
            .addProperty("field", new StringSchema())
            .addProperty("code", new StringSchema())
            .addProperty("message", new StringSchema());
    var problem =
        new Schema<>()
            .type("object")
            .addProperty("type", new StringSchema().format("uri"))
            .addProperty("title", new StringSchema())
            .addProperty("status", new IntegerSchema())
            .addProperty("detail", new StringSchema())
            .addProperty("instance", new StringSchema().format("uri-reference"))
            .addProperty("code", new StringSchema())
            .addProperty("correlationId", new StringSchema())
            .addProperty("errors", new ArraySchema().items(fieldViolation))
            .required(
                List.of("type", "title", "status", "detail", "instance", "code", "correlationId"));
    return problem;
  }

  @Bean
  OpenAPI couponOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Coupon API")
                .version("1.0.0")
                .description(
                    "Desafio técnico: criação, consulta e exclusão lógica. JWT é uma extensão ao contrato original."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }

  @Bean
  OpenApiCustomizer documentErrorsAndExamples(Clock clock) {
    return api -> {
      api.getComponents().addSchemas("ApiProblem", problemSchema());
      api.getPaths()
          .forEach(
              (path, item) -> {
                item.readOperationsMap()
                    .forEach(
                        (method, operation) -> {
                          addError(
                              operation,
                              "400",
                              "JSON, campos obrigatórios ou identificador inválidos.");
                          addError(operation, "401", "Autenticação ausente ou inválida.");
                          addError(operation, "406", "Representação solicitada não suportada.");
                          addError(operation, "500", "Erro inesperado; consulte o correlationId.");
                          if (path.startsWith("/coupon")) {
                            addError(operation, "403", "Token sem a permissão necessária.");
                          }
                          if (path.equals("/coupon/{id}")) {
                            addError(
                                operation,
                                "404",
                                "Cupom inexistente ou indisponível para consulta.");
                          }
                          if (method == PathItem.HttpMethod.POST) {
                            addError(operation, "413", "Corpo excede o limite técnico de 64 KiB.");
                            addError(operation, "415", "Content-Type deve ser application/json.");
                          }
                          if (method == PathItem.HttpMethod.DELETE) {
                            addError(
                                operation,
                                "409",
                                "Cupom já excluído ou alterado por outra requisição.");
                            operation.getResponses().get("204").setContent(null);
                          }
                        });
                if (path.equals("/coupon") && item.getPost() != null) {
                  addError(
                      item.getPost(),
                      "422",
                      "Regra de negócio ou representação numérica inválida.");
                  var content =
                      item.getPost().getRequestBody().getContent().get("application/json");
                  content.setExample(
                      Map.of(
                          "code",
                          "AB-12!CD",
                          "description",
                          "Cupom de demonstração",
                          "discountValue",
                          0.5,
                          "expirationDate",
                          clock.instant().plus(30, ChronoUnit.DAYS).toString(),
                          "published",
                          false));
                }
                if (path.equals("/auth/token") && item.getPost() != null) {
                  addError(
                      item.getPost(),
                      "429",
                      "Limite de tentativas atingido; respeite Retry-After.");
                }
              });
    };
  }

  private static void addError(Operation operation, String status, String description) {
    operation
        .getResponses()
        .addApiResponse(
            status,
            new ApiResponse()
                .description(description)
                .content(
                    new Content()
                        .addMediaType(
                            "application/problem+json",
                            new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiProblem")))));
  }
}
