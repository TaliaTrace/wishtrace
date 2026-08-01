import json
import uuid
from typing import cast

import httpx
import pytest
from openai.types.responses.response_text_config_param import ResponseTextConfigParam
from pydantic import SecretStr

from app.openai_ranking import (
    AzureOpenAiRankingGateway,
    OpenAiSdkResponsesTransport,
    StructuredProviderResponse,
    StructuredResponsesTransport,
)
from app.ranking import (
    EvidenceKind,
    ModelDecisionStatus,
    RankingCandidate,
    RankingEvidence,
    RankingGatewayError,
    RankingPackage,
)

FAKE_AZURE_KEY = "azure_test_fixture_key_not_real"


class CapturingTransport(StructuredResponsesTransport):
    def __init__(self, response: StructuredProviderResponse) -> None:
        self.response = response
        self.deployment: str | None = None
        self.instructions: str | None = None
        self.input_text: str | None = None
        self.text: ResponseTextConfigParam | None = None

    async def create(
        self,
        *,
        deployment: str,
        instructions: str,
        input_text: str,
        text: ResponseTextConfigParam,
    ) -> StructuredProviderResponse:
        self.deployment = deployment
        self.instructions = instructions
        self.input_text = input_text
        self.text = text
        return self.response


def _package() -> RankingPackage:
    return RankingPackage(
        run_id=uuid.uuid4(),
        discovery_id=uuid.uuid4(),
        candidates=[
            RankingCandidate(
                id=uuid.uuid4(),
                position=0,
                title="Observed gaming headset",
                variant_title="Black",
                description="A wired headset.",
                categories=["Gaming audio"],
                tags=["headset"],
            )
        ],
        evidence=[
            RankingEvidence(
                id="ev_interest_gaming",
                kind=EvidenceKind.INTEREST,
                value="gaming",
            ),
            RankingEvidence(
                id="ev_hint_private",
                kind=EvidenceKind.HINT,
                value="Mentioned wanting a headset",
            ),
        ],
    )


def _decision_text(package: RankingPackage) -> str:
    candidate_id = str(package.candidates[0].id)
    return json.dumps(
        {
            "status": "SELECTED",
            "selected_candidate_id": candidate_id,
            "alternative_candidate_ids": [],
            "rationales": [
                {
                    "candidate_id": candidate_id,
                    "evidence_ids": ["ev_interest_gaming"],
                    "reason": "Matches the saved gaming interest.",
                }
            ],
            "uncertainty": "LOW",
            "no_selection_reason": None,
        }
    )


async def test_gateway_sends_strict_dynamic_schema_and_minimized_input() -> None:
    package = _package()
    transport = CapturingTransport(
        StructuredProviderResponse(
            request_id="resp_azure_1",
            status="completed",
            output_text=_decision_text(package),
            refused=False,
            duration_ms=37,
        )
    )
    gateway = AzureOpenAiRankingGateway(
        deployment="wishtrace-ranking",
        transport=transport,
    )

    attempt = await gateway.rank(package, repair_reason=None)

    assert attempt.decision.status == ModelDecisionStatus.SELECTED
    assert attempt.request_id == "resp_azure_1"
    assert transport.deployment == "wishtrace-ranking"
    assert transport.input_text is not None
    model_input = json.loads(transport.input_text)
    assert set(model_input) == {"eligible_candidate_ids", "evidence", "candidates"}
    assert "recipient_name" not in transport.input_text
    assert "merchant_url" not in transport.input_text
    assert "price" not in transport.input_text.casefold()

    assert transport.text is not None
    text_format = cast(dict[str, object], transport.text["format"])
    assert text_format["type"] == "json_schema"
    assert text_format["strict"] is True
    schema = cast(dict[str, object], text_format["schema"])
    assert schema["additionalProperties"] is False
    properties = cast(dict[str, object], schema["properties"])
    rationales = cast(dict[str, object], properties["rationales"])
    rationale_items = cast(dict[str, object], rationales["items"])
    assert rationale_items["additionalProperties"] is False


async def test_gateway_marks_malformed_output_repairable_without_echoing_it() -> None:
    package = _package()
    transport = CapturingTransport(
        StructuredProviderResponse(
            request_id="resp_invalid_1",
            status="completed",
            output_text="not-json-and-must-not-be-echoed",
            refused=False,
            duration_ms=20,
        )
    )
    gateway = AzureOpenAiRankingGateway(
        deployment="wishtrace-ranking",
        transport=transport,
    )

    with pytest.raises(RankingGatewayError) as captured:
        await gateway.rank(package, repair_reason=None)

    assert captured.value.category == "MODEL_OUTPUT_INVALID"
    assert captured.value.repairable is True
    assert "not-json" not in captured.value.repair_reason
    assert "not-json" not in str(captured.value)


async def test_official_sdk_uses_azure_v1_and_disables_provider_storage() -> None:
    package = _package()
    observed: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/openai/v1/responses"
        assert request.headers["Authorization"] == f"Bearer {FAKE_AZURE_KEY}"
        observed.update(json.loads(request.content))
        output_text = _decision_text(package)
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "id": "resp_azure_sdk_1",
                "object": "response",
                "created_at": 1785596400,
                "status": "completed",
                "completed_at": 1785596401,
                "error": None,
                "incomplete_details": None,
                "instructions": None,
                "max_output_tokens": 700,
                "model": "wishtrace-ranking",
                "output": [
                    {
                        "id": "msg_azure_sdk_1",
                        "type": "message",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "output_text",
                                "text": output_text,
                                "annotations": [],
                            }
                        ],
                    }
                ],
                "parallel_tool_calls": True,
                "previous_response_id": None,
                "reasoning": None,
                "store": False,
                "temperature": None,
                "text": {"format": {"type": "json_schema"}},
                "tool_choice": "auto",
                "tools": [],
                "top_p": None,
                "truncation": "disabled",
                "usage": {
                    "input_tokens": 100,
                    "input_tokens_details": {"cached_tokens": 0},
                    "output_tokens": 30,
                    "output_tokens_details": {"reasoning_tokens": 0},
                    "total_tokens": 130,
                },
            },
        )

    transport = OpenAiSdkResponsesTransport(
        base_url="https://wishtrace.services.ai.azure.com/openai/v1/",
        api_key=SecretStr(FAKE_AZURE_KEY),
        transport=httpx.MockTransport(handler),
    )
    gateway = AzureOpenAiRankingGateway(
        deployment="wishtrace-ranking",
        transport=transport,
    )

    result = await gateway.rank(package, repair_reason=None)

    assert result.request_id == "resp_azure_sdk_1"
    assert observed["model"] == "wishtrace-ranking"
    assert observed["store"] is False
    text = cast(dict[str, object], observed["text"])
    text_format = cast(dict[str, object], text["format"])
    assert text_format["strict"] is True
    assert FAKE_AZURE_KEY not in json.dumps(observed)
