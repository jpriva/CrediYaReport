package co.com.pragma.api.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access =  lombok.AccessLevel.PRIVATE)
public class ApiConstants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Schemas{
        public static final String ERROR_SCHEMA_NAME = "Error Response";
        public static final String ERROR_SCHEMA_DESCRIPTION = "Error Response Details";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ApiParams {
        public static final String METRIC_NAME_PARAM = "metric";
        public static final String METRIC_NAME_DESC = "Name of the metric to retrieve.";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ApiPaths {
        public static final String BASE_PATH = "/api/v1";
        public static final String REPORT_PATH = BASE_PATH + "/reportes";
        public static final String REPORT_BY_NAME_PATH = REPORT_PATH + "/{" + ApiParams.METRIC_NAME_PARAM + "}";
        public static final String SWAGGER_PATH = "/swagger-ui.html";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ApiPathMatchers {
        //PERMIT ALL
        public static final String API_DOCS_MATCHER = "/v3/api-docs/**";
        public static final String SWAGGER_UI_MATCHER = "/swagger-ui/**";
        //SUPER_USER
        public static final String ACTUATOR_MATCHER = "/actuator/**";
        public static final String HEALTH_CHECK_MATCHER = "/actuator/health/**";
        //ADMIN
        public static final String REPORT_MATCHER = ApiPaths.REPORT_PATH +"/**";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Role {
        public static final String SUPER_USER_ROLE_NAME = "SUPER_USER";
        public static final String CLIENT_ROLE_NAME = "CLIENTE";
        public static final String ADMIN_ROLE_NAME = "ADMIN";
        public static final String ADVISOR_ROLE_NAME = "ASESOR";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ErrorDocs {
        public static final String ERROR_TIMESTAMP_DESCRIPTION = "Timestamp of when the error occurred.";
        public static final String ERROR_PATH_DESCRIPTION = "The path where the error occurred.";
        public static final String ERROR_CODE_DESCRIPTION = "A unique code identifying the error.";
        public static final String ERROR_MESSAGE_DESCRIPTION = "A human-readable message describing the error.";
        public static final String EXAMPLE_ERROR_TIMESTAMP = "2025-01-01T00:00:00.000Z";
        public static final String EXAMPLE_ERROR_CODE = "DOM-001";
        public static final String EXAMPLE_ERROR_MESSAGE = "An error occurred while processing the request.";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class MetricDoc {

        public static final String METRIC_OP_SUMMARY = "Retrive a metric value by name.";
        public static final String METRIC_OP_DESC = "Retrive a metric value by name.";
        public static final String OPERATION_GET_METRIC_ID = "getMetric";
        public static final String METRIC_DTO_NAME = "Metric";
        public static final String METRIC_DTO_DESC = "Represents a metric with its name and value.";
        public static final String METRIC_VALUE_DESC = "Value of the metric.";
        public static final String METRIC_VALUE_EXAMPLE = "150.00";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ApiResponses {
        public static final String RESPONSE_OK_CODE = "200";
        public static final String RESPONSE_BAD_REQUEST_CODE = "400";
        public static final String RESPONSE_NOT_FOUND_CODE = "404";
        public static final String RESPONSE_METRIC_OK_DESC = "Fetch Metric Successfully";
        public static final String RESPONSE_SAVE_SOLICITUDE_BAD_REQUEST_DESC = "Invalid request (e.g. metric parameter is invalid)";
        public static final String RESPONSE_UPDATE_SOLICITUDE_NOT_FOUND_DESC = "The metric with the specified name was not found.";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ApiConfig {
        public static final String DESCRIPTION_BEARER_AUTH = "Enter the JWT token obtained from the login endpoint.";
        public static final String NAME_BEARER_AUTH = "bearerAuth";
        public static final String SCHEME_BEARER = "bearer";
        public static final String BEARER_FORMAT_JWT = "JWT";
        public static final String TITLE_API = "Crediya Report API Microservice";
        public static final String VERSION_API = "1.0.0";
        public static final String DESCRIPTION_API = "This is the API for Crediya Report Microservice";
    }
}
