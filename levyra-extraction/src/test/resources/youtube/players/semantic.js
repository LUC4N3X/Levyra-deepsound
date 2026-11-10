var cfg={signatureTimestamp:20711};
var decoded=decodeURIComponent(cipher.s);
var alias=decoded;
var signed=RenamedSignature(7,alias);
query.set(signatureKey,encodeURIComponent(signed));
var throttle=query.get("n");
var rewritten=RenamedThrottle[3](throttle);
query.set("n",rewritten);
