package org.schabi.newpipe.extractor.services.youtube.sabr;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinSabrSessionPolicyTest {
    private static final SabrSessionPolicy.State STATE =
            new SabrSessionPolicy.State(4, 0, 0);

    private final BuiltinSabrSessionPolicy policy = new BuiltinSabrSessionPolicy();

    @Test
    void pendingAttestationDoesNotTriggerRecoveryOrRetry() throws Exception {
        final SabrDecodedResponse response = protectionResponse(2, 20, 59_000);

        assertEquals(List.of(
                        SabrSessionPolicy.ActionType.APPLY_RESPONSE_STATE,
                        SabrSessionPolicy.ActionType.DEFER_BACKOFF,
                        SabrSessionPolicy.ActionType.CONTINUE),
                actions(evaluate(response, SabrSessionPolicy.ControlMode.PUMP, false)));
        assertEquals(List.of(
                        SabrSessionPolicy.ActionType.APPLY_RESPONSE_STATE,
                        SabrSessionPolicy.ActionType.SLEEP_BACKOFF,
                        SabrSessionPolicy.ActionType.CONTINUE),
                actions(evaluate(response, SabrSessionPolicy.ControlMode.FETCH_SEGMENT, true)));
    }

    @Test
    void rejectedAttestationFailsWithoutBackoffOrRecovery() throws Exception {
        final SabrSessionPolicy.Result result = evaluate(
                protectionResponse(3, 20, 59_000),
                SabrSessionPolicy.ControlMode.PUMP,
                true);

        assertEquals(List.of(
                        SabrSessionPolicy.ActionType.APPLY_RESPONSE_STATE,
                        SabrSessionPolicy.ActionType.FAIL_SABR_ERROR),
                actions(result));
        assertTrue(result.getControlDecision().getErrorDetails().contains("attestation"));
        assertFalse(actions(result).contains(SabrSessionPolicy.ActionType.TRY_RELOAD));
    }

    @Test
    void maxRetriesDoesNotChangePendingAttestationControlFlow() throws Exception {
        assertEquals(
                actions(evaluate(protectionResponse(2, 0, 2_000),
                        SabrSessionPolicy.ControlMode.PUMP, false)),
                actions(evaluate(protectionResponse(2, 20, 2_000),
                        SabrSessionPolicy.ControlMode.PUMP, false)));
    }

    @Test
    void onlyExplicitReloadResponseRequestsReload() throws Exception {
        final SabrDecodedResponse ordinary = protectionResponse(2, 20, 2_000);
        final SabrDecodedResponse explicitReload = protectionResponse(2, 20, 2_000);
        explicitReload.setReloadRequested(true);

        assertFalse(actions(evaluate(ordinary, SabrSessionPolicy.ControlMode.PUMP, false))
                .contains(SabrSessionPolicy.ActionType.TRY_RELOAD));
        assertEquals(List.of(
                        SabrSessionPolicy.ActionType.APPLY_RESPONSE_STATE,
                        SabrSessionPolicy.ActionType.TRY_RELOAD),
                actions(evaluate(explicitReload, SabrSessionPolicy.ControlMode.PUMP, false)));
    }

    private SabrSessionPolicy.Result evaluate(final SabrDecodedResponse response,
                                               final SabrSessionPolicy.ControlMode mode,
                                               final boolean honorBackoff) throws Exception {
        return policy.evaluate(STATE,
                new SabrSessionPolicy.ControlResponseEvent(0, honorBackoff, mode, response));
    }

    private static SabrDecodedResponse protectionResponse(final int status,
                                                          final int maxRetries,
                                                          final int backoffMs) {
        final SabrDecodedResponse response = new SabrDecodedResponse();
        response.setStreamProtectionStatus(status);
        response.setStreamProtectionMaxRetries(maxRetries);
        response.setBackoffTimeMs(backoffMs);
        return response;
    }

    private static List<SabrSessionPolicy.ActionType> actions(
            final SabrSessionPolicy.Result result) {
        return result.getActions().stream()
                .map(SabrSessionPolicy.Action::getType)
                .collect(Collectors.toList());
    }
}
