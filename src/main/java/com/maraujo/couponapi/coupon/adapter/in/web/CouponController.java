package com.maraujo.couponapi.coupon.adapter.in.web;

import com.maraujo.couponapi.coupon.adapter.in.web.dto.*;
import com.maraujo.couponapi.coupon.adapter.in.web.mapper.CouponWebMapper;
import com.maraujo.couponapi.coupon.application.port.in.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/coupon")
@SecurityRequirement(name = "bearerAuth")
public class CouponController {
  private final CreateCouponUseCase create;
  private final GetCouponUseCase get;
  private final DeleteCouponUseCase delete;
  private final CouponWebMapper mapper;

  public CouponController(
      CreateCouponUseCase create,
      GetCouponUseCase get,
      DeleteCouponUseCase delete,
      CouponWebMapper mapper) {
    this.create = create;
    this.get = get;
    this.delete = delete;
    this.mapper = mapper;
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Cadastrar cupom",
      description = "Normaliza o código antes de validar seus seis caracteres.")
  @ApiResponse(responseCode = "201", description = "Cupom criado")
  public ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
    request.validateRepresentation();
    var coupon = create.execute(mapper.toCommand(request));
    return ResponseEntity.created(URI.create("/coupon/" + coupon.snapshot().id()))
        .body(mapper.toResponse(coupon.snapshot()));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Consultar cupom", description = "Cupons excluídos retornam 404.")
  public CouponResponse get(@PathVariable UUID id) {
    return mapper.toResponse(get.execute(id).snapshot());
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Excluir cupom",
      description = "Exclusão lógica. Repetir a exclusão retorna 409.")
  @ApiResponse(
      responseCode = "204",
      description = "Excluído, sem corpo",
      content = @io.swagger.v3.oas.annotations.media.Content)
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    delete.execute(id);
    return ResponseEntity.noContent().build();
  }
}
