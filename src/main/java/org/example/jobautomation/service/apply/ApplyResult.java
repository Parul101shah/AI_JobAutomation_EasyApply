package org.example.jobautomation.service.apply;

public record ApplyResult(Outcome outcome, String detail) {
    public enum Outcome {
        SUBMITTED,
        DRY_RUN_OK,
        NEEDS_MANUAL,
        FAILED
    }
    public static ApplyResult of(Outcome o, String d) {
        return new ApplyResult(o, d);
    }
}
//The Outcome enum is nested inside ApplyResult because it is conceptually owned by ApplyResult and is not intended to be reused elsewhere.


