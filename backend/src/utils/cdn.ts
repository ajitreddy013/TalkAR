import AWS from "aws-sdk";

type SignedUrlResult = {
  url: string;
  expiresAt: string | null;
};

function isHttpUrl(url: string): boolean {
  return /^https?:\/\//i.test(url);
}

export function getSignedArtifactUrl(rawUrl?: string | null): SignedUrlResult {
  if (!rawUrl) {
    return { url: "", expiresAt: null };
  }

  if (!isHttpUrl(rawUrl)) {
    return { url: rawUrl, expiresAt: null };
  }

  const keyPairId = process.env.CLOUDFRONT_KEY_PAIR_ID;
  const privateKey = process.env.CLOUDFRONT_PRIVATE_KEY;
  const ttlSeconds = Number(process.env.CLOUDFRONT_SIGNED_URL_TTL_SECONDS || 600);

  if (!keyPairId || !privateKey) {
    return { url: rawUrl, expiresAt: null };
  }

  try {
    const expiresEpoch = Math.floor(Date.now() / 1000) + Math.max(60, ttlSeconds);
    const signer = new AWS.CloudFront.Signer(
      keyPairId,
      privateKey.includes("\\n") ? privateKey.replace(/\\n/g, "\n") : privateKey
    );
    const signedUrl = signer.getSignedUrl({
      url: rawUrl,
      expires: expiresEpoch,
    });
    return {
      url: signedUrl,
      expiresAt: new Date(expiresEpoch * 1000).toISOString(),
    };
  } catch (error) {
    console.warn("[cdn] failed to sign URL, returning raw URL", {
      message: (error as Error).message,
    });
    return { url: rawUrl, expiresAt: null };
  }
}
