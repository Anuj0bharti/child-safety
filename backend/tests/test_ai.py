import asyncio

import pytest

from app.ai import DisabledProvider, GeminiProvider, NemotronProvider


def test_disabled_provider_is_explicitly_unavailable() -> None:
    result = asyncio.run(DisabledProvider().analyze({"eventType": "SOS"}))
    assert result.provider == "disabled"
    assert result.status == "UNAVAILABLE"


@pytest.mark.parametrize("provider", [GeminiProvider("", ""), NemotronProvider("", "")])
def test_missing_ai_credentials_do_not_raise_or_block(provider) -> None:
    result = asyncio.run(provider.analyze({"eventType": "SOS"}))
    assert result.status == "UNAVAILABLE"
