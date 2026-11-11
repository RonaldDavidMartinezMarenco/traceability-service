package com.foodcourt.traceability.infrastructure.input.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.foodcourt.traceability.application.handler.GetOrderEfficiencyHandler;
import com.foodcourt.traceability.domain.model.OrderEfficiency;
import com.foodcourt.traceability.domain.model.PageQuery;
import com.foodcourt.traceability.infrastructure.input.rest.dto.OrderEfficiencyResponse;
import com.foodcourt.traceability.infrastructure.input.rest.mapper.OrderEfficiencyRestMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Lets an owner read how long each finished order took and how the employees compare.
 * The class is not final: method security needs a CGLIB subclass.
 */
@RestController
@Validated
@Tag(name = "Order efficiency")
@SecurityRequirement(name = "bearerAuth")
class OrderEfficiencyController {

    private final GetOrderEfficiencyHandler handler;
    private final OrderEfficiencyRestMapper mapper = new OrderEfficiencyRestMapper();

    OrderEfficiencyController(GetOrderEfficiencyHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/restaurants/{restaurantId}/efficiency")
    @Operation(
            summary = "Read order efficiency for a restaurant",
            description = "The owner of the restaurant sees each finished order and a ranking of employees. "
                    + "A finished order starts at IN_PREPARATION and ends at DELIVERED. "
                    + "Pending, cancelled, and unfinished orders are left out. "
                    + "The ranking uses every finished order, not only the page.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Durations and ranking. Empty when nothing is finished",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderEfficiencyResponse.class),
                            examples = @ExampleObject(name = "One delivered order", value = """
                                    {
                                      "restaurantId": 1,
                                      "completedOrders": 1,
                                      "averageDurationSeconds": 600,
                                      "orders": {
                                        "items": [
                                          {
                                            "orderId": 1,
                                            "employeeId": 5,
                                            "startedAt": "2026-11-10T17:05:00Z",
                                            "finishedAt": "2026-11-10T17:15:00Z",
                                            "durationSeconds": 600,
                                            "slowerThanAverage": false
                                          }
                                        ],
                                        "page": 0,
                                        "size": 10,
                                        "totalItems": 1,
                                        "totalPages": 1
                                      },
                                      "employeeRanking": [
                                        {
                                          "rank": 1,
                                          "employeeId": 5,
                                          "completedOrders": 1,
                                          "averageDurationSeconds": 600
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid restaurant id, page, or size"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "403", description = "The caller does not own this restaurant"),
            @ApiResponse(responseCode = "404", description = "The account or the restaurant does not exist"),
            @ApiResponse(responseCode = "503", description = "User service or plaza service is unavailable")
    })
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OrderEfficiencyResponse> efficiency(
            @Parameter(description = "La Parrilla de Carlos after the seed", example = "1")
            @PathVariable @Positive long restaurantId,
            @Parameter(description = "Page index, starting at zero", example = "0")
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @Parameter(description = "Finished orders per page, from 1 to 50", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        OrderEfficiency report = handler.get(restaurantId, mapper.toPageQuery(page, size));
        return ResponseEntity.ok(mapper.toResponse(report));
    }
}
