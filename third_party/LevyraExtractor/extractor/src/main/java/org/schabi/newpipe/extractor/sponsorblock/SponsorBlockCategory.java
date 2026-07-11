package org.schabi.newpipe.extractor.sponsorblock;

public enum SponsorBlockCategory {
    SPONSOR("sponsor"),
    INTRO("intro"),
    OUTRO("outro"),
    INTERACTION("interaction"),
    HIGHLIGHT("poi_highlight"),
    SELF_PROMO("selfpromo"),
    NON_MUSIC("music_offtopic"),
    PREVIEW("preview"),
    FILLER("filler"),
    PENDING("pending");

    private final String apiName;

    SponsorBlockCategory(final String apiName) {
        this.apiName = apiName;
    }

    public static SponsorBlockCategory fromApiName(final String apiName) {
        switch (apiName) {
            case "sponsor":
                return SponsorBlockCategory.SPONSOR;
            case "intro":
                return SponsorBlockCategory.INTRO;
            case "outro":
                return SponsorBlockCategory.OUTRO;
            case "interaction":
                return SponsorBlockCategory.INTERACTION;
            case "poi_highlight":
                return SponsorBlockCategory.HIGHLIGHT;
            case "selfpromo":
                return SponsorBlockCategory.SELF_PROMO;
            case "music_offtopic":
                return SponsorBlockCategory.NON_MUSIC;
            case "preview":
                return SponsorBlockCategory.PREVIEW;
            case "filler":
                return SponsorBlockCategory.FILLER;
            default:
                throw new IllegalArgumentException("Invalid API name");
        }
    }

    public String getApiName() {
        return apiName;
    }
}
