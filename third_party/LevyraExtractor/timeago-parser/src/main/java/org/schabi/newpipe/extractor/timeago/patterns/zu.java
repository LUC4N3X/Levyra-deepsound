package org.schabi.newpipe.extractor.timeago.patterns;

import org.schabi.newpipe.extractor.timeago.PatternsHolder;

import java.time.temporal.ChronoUnit;

public class zu extends PatternsHolder {
    private static final String WORD_SEPARATOR = " ";
    private static final String[]
            SECONDS  /**/ = {"amasekhondi", "isekhondi", "emasekhondini", "umzuzwana", "imizuzwana", "emizuzwaneni"},
            MINUTES  /**/ = {"amaminithi", "iminithi", "emaminithini", "umzuzu", "imizuzu", "emizuzwini"},
            HOURS    /**/ = {"amahora", "ihora", "emahoreni"},
            DAYS     /**/ = {"izinsuku", "usuku", "osukwini", "ezinsukwini"},
            WEEKS    /**/ = {"amaviki", "iviki", "emavikini", "isonto", "amasonto", "emasontweni"},
            MONTHS   /**/ = {"inyanga", "izinyanga", "ezinyangeni"},
            YEARS    /**/ = {"iminyaka", "unyaka", "eminyakeni"};

    private static final zu INSTANCE = new zu();

    public static zu getInstance() {
        return INSTANCE;
    }

    private zu() {
        super(WORD_SEPARATOR, SECONDS, MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS);
        putSpecialCase(ChronoUnit.SECONDS, "manje", 0); // now
        putSpecialCase(ChronoUnit.DAYS, "izolo", 1);    // yesterday
    }
}