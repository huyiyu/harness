from abc import ABC, abstractmethod
from dataclasses import dataclass

@dataclass
class ScenarioResult:
    success: bool
    message: str

class Scenario(ABC):
    @property
    @abstractmethod
    def name(self) -> str: ...

    @abstractmethod
    def can_handle(self, event_type: str, payload: dict) -> bool: ...

    @abstractmethod
    def handle(self, payload: dict) -> ScenarioResult: ...
