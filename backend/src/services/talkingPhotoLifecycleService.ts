import AWS from "aws-sdk";

function parseVersionFromKey(key: string): number | null {
  const match = key.match(/\/v(\d+)\//);
  if (!match) return null;
  const v = Number(match[1]);
  return Number.isFinite(v) ? v : null;
}

export async function cleanupArtifactVersionsInS3(
  imageId: string,
  keepLatestN: number
): Promise<{ deleted: number; kept: number }> {
  const bucket = process.env.AWS_S3_BUCKET;
  if (!bucket || keepLatestN < 1) {
    return { deleted: 0, kept: 0 };
  }

  const s3 = new AWS.S3({ region: process.env.AWS_REGION || "us-east-1" });
  const prefix = `talking-photo/${imageId}/`;
  const listResp = await s3
    .listObjectsV2({
      Bucket: bucket,
      Prefix: prefix,
      MaxKeys: 1000,
    })
    .promise();

  const keys = (listResp.Contents || [])
    .map((o) => o.Key)
    .filter((k): k is string => typeof k === "string");
  if (keys.length === 0) return { deleted: 0, kept: 0 };

  const groups = new Map<number, string[]>();
  for (const key of keys) {
    const version = parseVersionFromKey(key);
    if (version == null) continue;
    const arr = groups.get(version) || [];
    arr.push(key);
    groups.set(version, arr);
  }

  const versions = Array.from(groups.keys()).sort((a, b) => b - a);
  const keepVersions = new Set(versions.slice(0, keepLatestN));
  const toDelete: string[] = [];
  for (const [version, versionKeys] of groups.entries()) {
    if (!keepVersions.has(version)) {
      toDelete.push(...versionKeys);
    }
  }

  if (toDelete.length > 0) {
    await s3
      .deleteObjects({
        Bucket: bucket,
        Delete: {
          Objects: toDelete.map((Key) => ({ Key })),
          Quiet: true,
        },
      })
      .promise();
  }

  return { deleted: toDelete.length, kept: keys.length - toDelete.length };
}
