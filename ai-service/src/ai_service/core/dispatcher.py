from ai_service.core.scenario import Scenario, ScenarioResult
from ai_service.utils.logger import get_logger

logger = get_logger(__name__)

class Dispatcher:
    def __init__(self, scenarios: list[Scenario]):
        self._scenarios = scenarios

    def dispatch(self, event_type: str, payload: dict) -> ScenarioResult | None:
        for scenario in self._scenarios:
            if scenario.can_handle(event_type, payload):
                logger.info("dispatching event=%s to scenario=%s", event_type, scenario.name)
                return scenario.handle(payload)
        logger.info("no_scenario_matched event=%s", event_type)
        return None
