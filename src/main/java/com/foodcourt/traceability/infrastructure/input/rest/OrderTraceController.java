package com.foodcourt.traceability.infrastructure.input.rest;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.foodcourt.traceability.application.handler.GetOrderTraceHandler;
import com.foodcourt.traceability.application.handler.RecordStatusChangeHandler;
import com.foodcourt.traceability.domain.model.OrderTrace;
import com.foodcourt.traceability.domain.model.SaveResult;
import com.foodcourt.traceability.infrastructure.input.rest.dto.OrderTraceResponse;
import com.foodcourt.traceability.infrastructure.input.rest.dto.RecordStatusChangeRequest;
import com.foodcourt.traceability.infrastructure.input.rest.dto.StatusChangeResponse;
import com.foodcourt.traceability.infrastructure.input.rest.mapper.OrderTraceRestMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * Records a status change and lets a customer read their own order history.
 * The class is not final: method security needs a CGLIB subclass.
 */
@RestController
@Validated
@Tag(name = "Order trace")
@SecurityRequirement(name = "bearerAuth")
class OrderTraceController {

    private final RecordStatusChangeHandler recorder;
    private final GetOrderTraceHandler reader;
    private final OrderTraceRestMapper mapper = new OrderTraceRestMapper();

    OrderTraceController(RecordStatusChangeHandler recorder, GetOrderTraceHandler reader) {
        this.recorder = recorder;
        this.reader = reader;
    }

    @PostMapping("/order-status-changes")
    @Operation(
            summary = "Record one status change",
            description = "A customer records PENDING or CANCELLED for their own order. "
                    + "An employee records IN_PREPARATION, READY, or DELIVERED for their restaurant. "
                    + "The same change sent again returns the stored row.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The status change was stored"),
            @ApiResponse(responseCode = "200", description = "The same status change was already stored"),
            @ApiResponse(responseCode = "400", description = "The body or the employee does not fit the status"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "403", description = "The caller cannot record this change"),
            @ApiResponse(responseCode = "404", description = "The caller account does not exist"),
            @ApiResponse(responseCode = "409", description = "The status does not follow the stored history"),
            @ApiResponse(responseCode = "503", description = "User service is unavailable")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE')")
    public ResponseEntity<StatusChangeResponse> record(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RecordStatusChangeRequest.class),
                    examples = @ExampleObject(name = "Order placed", value = """
                            {
                              "orderId": 1,
                              "customerId": 9,
                              "restaurantId": 1,
                              "previousStatus": null,
                              "newStatus": "PENDING"
                            }
                            """)))
            @Valid @RequestBody RecordStatusChangeRequest request) {
        SaveResult result = recorder.record(mapper.toDraft(request));
        return response(result);
    }

    @GetMapping("/orders/{orderId}/trace")
    @Operation(
            summary = "Read the status history of one order",
            description = "Only the customer who placed the order can read it. "
                    + "secondsInStatus is the time until the next change, and null on the current status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The history, oldest first", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OrderTraceResponse.class),
                    examples = @ExampleObject(name = "Delivered order", value = """
                            {
                              "orderId": 1,
                              "customerId": 9,
                              "restaurantId": 1,
                              "changes": [
                                {
                                  "previousStatus": null,
                                  "newStatus": "PENDING",
                                  "occurredAt": "2026-11-10T17:00:00Z",
                                  "secondsInStatus": 300
                                },
                                {
                                  "previousStatus": "PENDING",
                                  "newStatus": "IN_PREPARATION",
                                  "occurredAt": "2026-11-10T17:05:00Z",
                                  "secondsInStatus": null
                                }
                              ]
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "The order id is not a positive number"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "403", description = "The caller is not a customer"),
            @ApiResponse(responseCode = "404", description = "This customer has no trace for the order"),
            @ApiResponse(responseCode = "503", description = "User service is unavailable")
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderTraceResponse trace(@PathVariable @Positive long orderId) {
        OrderTrace trace = reader.get(orderId);
        return mapper.toResponse(trace);
    }

    private ResponseEntity<StatusChangeResponse> response(SaveResult result) {
        StatusChangeResponse body = mapper.toResponse(result.change());
        if (result.created()) {
            return ResponseEntity.created(location(result)).body(body);
        }
        return ResponseEntity.ok(body);
    }

    private static URI location(SaveResult result) {
        return URI.create("/orders/" + result.change().orderId() + "/trace");
    }
}
