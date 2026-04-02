package com.nuvi.online_renting.currency.controller;

import com.nuvi.online_renting.currency.dto.ExchangeRateRequestDTO;
import com.nuvi.online_renting.currency.dto.ExchangeRateResponseDTO;
import com.nuvi.online_renting.currency.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Currency", description = "Exchange rate management. Items are always priced in LKR; rates here drive display-only conversion.")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Operation(summary = "List all supported currencies and their rates (public)")
    @GetMapping("/currencies")
    public ResponseEntity<List<ExchangeRateResponseDTO>> getAllRates() {
        return ResponseEntity.ok(exchangeRateService.getAllRates());
    }

    @Operation(summary = "Add or update an exchange rate (admin)", description = "Creates the currency entry if it does not exist, or updates the rate if it does.")
    @PutMapping("/admin/currencies")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<ExchangeRateResponseDTO> upsertRate(@Valid @RequestBody ExchangeRateRequestDTO request) {
        return ResponseEntity.ok(exchangeRateService.upsertRate(request));
    }

    @Operation(summary = "Remove a currency (admin)")
    @DeleteMapping("/admin/currencies/{code}")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<Void> deleteRate(@PathVariable String code) {
        exchangeRateService.deleteRate(code);
        return ResponseEntity.noContent().build();
    }
}
