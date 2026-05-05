from ai_service.core.dispatcher import Dispatcher
from ai_service.core.scenario import Scenario, ScenarioResult

class FakeScenario(Scenario):
    @property
    def name(self) -> str:
        return "fake"

    def can_handle(self, event_type: str, payload: dict) -> bool:
        return event_type == "Merge Request Hook"

    def handle(self, payload: dict) -> ScenarioResult:
        return ScenarioResult(success=True, message="handled")

def test_dispatcher_routes_to_matching_scenario():
    dispatcher = Dispatcher([FakeScenario()])
    result = dispatcher.dispatch("Merge Request Hook", {"object_kind": "merge_request"})
    assert result is not None
    assert result.success is True

def test_dispatcher_returns_none_for_unmatched_event():
    dispatcher = Dispatcher([FakeScenario()])
    result = dispatcher.dispatch("Push Hook", {})
    assert result is None
