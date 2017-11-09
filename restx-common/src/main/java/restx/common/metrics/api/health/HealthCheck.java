package restx.common.metrics.api.health;

import restx.common.metrics.api.health.exceptions.HealthCheckException;

public interface HealthCheck {

    void check() throws HealthCheckException;

}
