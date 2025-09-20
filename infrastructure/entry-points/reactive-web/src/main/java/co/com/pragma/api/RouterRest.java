package co.com.pragma.api;

import co.com.pragma.api.constants.ApiConstants;
import co.com.pragma.api.dto.ErrorDTO;
import co.com.pragma.api.dto.MetricApiDTO;
import co.com.pragma.model.constants.Metrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static co.com.pragma.api.constants.ApiConstants.ApiParams.METRIC_NAME_DESC;
import static co.com.pragma.api.constants.ApiConstants.ApiPaths.REPORT_BY_NAME_PATH;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = REPORT_BY_NAME_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE},
                    method = RequestMethod.GET,
                    beanMethod = "listenGETMetricUseCase",
                    operation = @Operation(
                            summary = ApiConstants.MetricDoc.METRIC_OP_SUMMARY,
                            description = ApiConstants.MetricDoc.METRIC_OP_DESC,
                            operationId = ApiConstants.MetricDoc.OPERATION_GET_METRIC_ID,
                            security = @SecurityRequirement(name = "bearerAuth"),
                            parameters = {
                                    @Parameter(
                                            in = ParameterIn.PATH,
                                            name = ApiConstants.ApiParams.METRIC_NAME_PARAM,
                                            description = METRIC_NAME_DESC,
                                            required = true,
                                            example = Metrics.QUANTITY_METRIC,
                                            schema = @Schema(type = "string", allowableValues = {Metrics.QUANTITY_METRIC, Metrics.AMOUNT_METRIC})
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = ApiConstants.ApiResponses.RESPONSE_OK_CODE,
                                            description = ApiConstants.ApiResponses.RESPONSE_METRIC_OK_DESC,
                                            content = @Content(schema = @Schema(implementation = MetricApiDTO.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = ApiConstants.ApiResponses.RESPONSE_BAD_REQUEST_CODE,
                                            description = ApiConstants.ApiResponses.RESPONSE_SAVE_SOLICITUDE_BAD_REQUEST_DESC,
                                            content = @Content(schema = @Schema(implementation = ErrorDTO.class))
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(GET(REPORT_BY_NAME_PATH), handler::listenGETMetricUseCase);
    }
}
