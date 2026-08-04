from pathlib import Path

path = Path('.github/scripts/pr299_fix_extractor.py')
text = path.read_text(encoding='utf-8')
start = text.index('# Increment rotation budget only after the provider actually invalidates its identity.')
end = text.index('write(path, text)', start)
end += len('write(path, text)')
replacement = '''# Increment rotation budget only after the provider actually invalidates its identity.
text = replace_once(
    text,
    """        attestationIdentityRotations++;
        if (!rotationPoTokenProvider.invalidatePoTokenIdentity(info)) {
            addDiagnosticEvent(\"attestation_identity_rotation_unavailable attempt=\"
                    + attestationIdentityRotations);
            return false;
        }
        final String previousVisitorData = info.getVisitorData();
""",
    """        if (!rotationPoTokenProvider.invalidatePoTokenIdentity(info)) {
            addDiagnosticEvent(\"attestation_identity_rotation_unavailable attempted=\"
                    + (attestationIdentityRotations + 1));
            return false;
        }
        attestationIdentityRotations++;
        final String previousVisitorData = info.getVisitorData();
""",
    'attestation rotation budget',
)
write(path, text)'''
path.write_text(text[:start] + replacement + text[end:], encoding='utf-8')
