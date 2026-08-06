"""Базовый контракт провайдеров данных.

Каждый провайдер возвращает либо реальные данные (is_demo=False),
либо честно помеченные demo-значения (is_demo=True) при недоступности источника.
Ошибки мапятся на уровне API (rate limit → понятное сообщение для UI).
"""

from abc import ABC, abstractmethod
from typing import Any


class ProviderError(Exception):
    """Ошибка провайдера (сеть, 429, недоступность)."""


class BaseProvider(ABC):
    name: str = "base"

    @abstractmethod
    def fetch_quote(self, symbol: str) -> dict[str, Any]:
        """Вернуть котировку в каноническом формате (см. schemas.QuoteOut)."""
        raise NotImplementedError
