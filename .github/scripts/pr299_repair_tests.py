from pathlib import Path

path = Path('third_party/LevyraExtractor/extractor/src/test/java/org/schabi/newpipe/extractor/services/youtube/sabr/YoutubeSabrSessionStreamProtectionTest.java')
text = path.read_text(encoding='utf-8')

old_pending = '''    @Test
    void fetchSegmentRotatesMediaBearingPendingAttestationBeforeReturningTarget()
            throws Exception {
        final byte[] targetData = new byte[]{4, 5, 6, 7};
        final FakeDownloader downloader = new FakeDownloader(
                protectionMediaResponse(SabrStreamProtectionStatus.ATTESTATION_PENDING,
                        20, 2_000, 299, 1, targetData),
                protectionResponse(1, 0, 0));
        final AtomicInteger invalidations = new AtomicInteger();
        final AtomicInteger tokenRequests = new AtomicInteger();
        final SabrPoTokenProvider tokenProvider = new SabrPoTokenProvider() {
            @Nullable
            @Override
            public byte[] getPoToken(@Nonnull final YoutubeSabrInfo info,
                                     @Nonnull final YoutubeSabrStreamState streamState) {
                tokenRequests.incrementAndGet();
                return new byte[]{9, 9, 9};
            }

            @Override
            public boolean invalidatePoTokenIdentity(@Nonnull final YoutubeSabrInfo info) {
                invalidations.incrementAndGet();
                return true;
            }
        };
        final YoutubeSabrInfo freshInfo = sabrInfo(
                "fresh-visitor", "https://example.com/fresh-sabr", true);
        final Fixture fixture = createFixture(downloader, tokenProvider,
                (videoId, profile, localization, contentCountry,
                 rotationProvider) -> freshInfo);
        fixture.session.getStreamState().setPoToken(new byte[]{1, 2, 3});
        fixture.session.getStreamState().ingestInitializationData(
                fixture.videoFormat, new byte[]{0});

        final SabrMediaSegment segment = fixture.session.fetchSegment(
                SabrSegmentRequest.media(fixture.videoFormat, 1), LOCALIZATION);

        assertArrayEquals(targetData, segment.getData());
        assertEquals(1, invalidations.get());
        assertEquals(1, tokenRequests.get());
        assertEquals(List.of(
                        "https://example.com/sabr?alr=yes&cpn=cpn&rn=0",
                        "https://example.com/fresh-sabr?alr=yes&cpn=cpn&rn=0"),
                downloader.requestUrls);
        assertTrue(fixture.session.getDiagnosticTrace().contains(
                "attestation_identity_rotation attempt=1 visitorChanged=true bytes=3"));
        assertTrue(fixture.session.getDiagnosticTrace().contains("protection=1/0"));
        assertEquals(1, fixture.session.getStreamState().getMaxSegment(fixture.videoFormat));
        assertTrue(fixture.session.getDiagnosticTrace().contains(
                "ranges=itag=299:seq=1-1:time=0+1000:timescale=1000"));
    }
'''
new_pending = '''    @Test
    void fetchSegmentAcceptsMediaBearingPendingAttestationWithoutRotation()
            throws Exception {
        final byte[] targetData = new byte[]{4, 5, 6, 7};
        final FakeDownloader downloader = new FakeDownloader(
                protectionMediaResponse(SabrStreamProtectionStatus.ATTESTATION_PENDING,
                        20, 2_000, 299, 1, targetData));
        final AtomicInteger invalidations = new AtomicInteger();
        final AtomicInteger tokenRequests = new AtomicInteger();
        final SabrPoTokenProvider tokenProvider = new SabrPoTokenProvider() {
            @Nullable
            @Override
            public byte[] getPoToken(@Nonnull final YoutubeSabrInfo info,
                                     @Nonnull final YoutubeSabrStreamState streamState) {
                tokenRequests.incrementAndGet();
                return new byte[]{9, 9, 9};
            }

            @Override
            public boolean invalidatePoTokenIdentity(@Nonnull final YoutubeSabrInfo info) {
                invalidations.incrementAndGet();
                return true;
            }
        };
        final Fixture fixture = createFixture(downloader, tokenProvider);
        fixture.session.getStreamState().setPoToken(new byte[]{1, 2, 3});
        fixture.session.getStreamState().ingestInitializationData(
                fixture.videoFormat, new byte[]{0});

        final SabrMediaSegment segment = fixture.session.fetchSegment(
                SabrSegmentRequest.media(fixture.videoFormat, 1), LOCALIZATION);

        assertArrayEquals(targetData, segment.getData());
        assertEquals(0, invalidations.get());
        assertEquals(0, tokenRequests.get());
        assertArrayEquals(new byte[]{1, 2, 3}, fixture.session.getStreamState().getPoToken());
        assertEquals(List.of("https://example.com/sabr?alr=yes&cpn=cpn&rn=0"),
                downloader.requestUrls);
        assertFalse(fixture.session.getDiagnosticTrace().contains(
                "attestation_identity_rotation attempt="));
        assertEquals(SabrStreamProtectionStatus.ATTESTATION_PENDING,
                fixture.session.getMaxStreamProtectionStatus());
        assertEquals(1, fixture.session.getStreamState().getMaxSegment(fixture.videoFormat));
    }
'''

old_required = '''    @Test
    void fetchSegmentRejectsMediaBearingRequiredAttestationBeforeReturningTarget()
            throws Exception {
        final FakeDownloader downloader = new FakeDownloader(
                protectionMediaResponse(SabrStreamProtectionStatus.ATTESTATION_REQUIRED,
                        20, 59_000, 299, 1, new byte[]{4, 5, 6, 7}));
        final Fixture fixture = createFixture(downloader, null);

        final SabrProtocolException failure = assertThrows(SabrProtocolException.class,
                () -> fixture.session.fetchSegment(
                        SabrSegmentRequest.media(fixture.videoFormat, 1), LOCALIZATION));

        assertTrue(failure.getMessage().contains("attestation required"));
        assertEquals(1, downloader.requestCount.get());
        assertEquals(SabrStreamProtectionStatus.ATTESTATION_REQUIRED,
                fixture.session.getMaxStreamProtectionStatus());
    }
'''
new_required = '''    @Test
    void fetchSegmentAcceptsMediaBearingRequiredStatus()
            throws Exception {
        final byte[] targetData = new byte[]{4, 5, 6, 7};
        final FakeDownloader downloader = new FakeDownloader(
                protectionMediaResponse(SabrStreamProtectionStatus.ATTESTATION_REQUIRED,
                        20, 59_000, 299, 1, targetData));
        final Fixture fixture = createFixture(downloader, null);

        final SabrMediaSegment segment = fixture.session.fetchSegment(
                SabrSegmentRequest.media(fixture.videoFormat, 1), LOCALIZATION);

        assertArrayEquals(targetData, segment.getData());
        assertEquals(1, downloader.requestCount.get());
        assertEquals(SabrStreamProtectionStatus.ATTESTATION_REQUIRED,
                fixture.session.getMaxStreamProtectionStatus());
    }
'''

if text.count(old_pending) != 1:
    raise SystemExit('pending media-bearing test anchor mismatch')
if text.count(old_required) != 1:
    raise SystemExit('required media-bearing test anchor mismatch')
text = text.replace(old_pending, new_pending, 1)
text = text.replace(old_required, new_required, 1)
path.write_text(text, encoding='utf-8')
