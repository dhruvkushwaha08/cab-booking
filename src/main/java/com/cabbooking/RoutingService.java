
package com.cabbooking;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class RoutingService {

    private final RestClient restClient;

    public RoutingService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://router.project-osrm.org")
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "identity")
                .build();
    }

    public RouteResult getRoute(
            double pickupLongitude,
            double pickupLatitude,
            double dropoffLongitude,
            double dropoffLatitude) {

        try {
            OsrmResponse response = restClient.get()
                    .uri(
                            "/route/v1/driving/{pickupLon},{pickupLat};{dropoffLon},{dropoffLat}?overview=false",
                            pickupLongitude,
                            pickupLatitude,
                            dropoffLongitude,
                            dropoffLatitude)
                    .retrieve()
                    .body(OsrmResponse.class);

            if (response == null
                    || !"Ok".equals(response.code())
                    || response.routes() == null
                    || response.routes().isEmpty()) {
                throw new RoutingException(
                        "No drivable route was found between these locations.");
            }

            OsrmRoute route = response.routes().get(0);

            if (route.distance() < 0 || route.duration() < 0) {
                throw new RoutingException(
                        "The routing service returned invalid route data.");
            }

            return new RouteResult(
                    route.distance() / 1000.0,
                    route.duration() / 60.0);

        } catch (RestClientException exception) {
            throw new RoutingException(
                    "The routing service is temporarily unavailable. Please try again.",
                    exception);
        }
    }

    public record RouteResult(
            double distanceKm,
            double durationMinutes) {
    }

    public record OsrmResponse(
            String code,
            List<OsrmRoute> routes) {
    }

    public record OsrmRoute(
            double distance,
            double duration) {
    }

    public static class RoutingException extends RuntimeException {

        public RoutingException(String message) {
            super(message);
        }

        public RoutingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
