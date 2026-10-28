Texture2D albedo : register(t0);
SamplerState linearSampler : register(s0);

float4 PSMain(float2 uv : TEXCOORD0) : SV_Target {
    return albedo.Sample(linearSampler, uv);
}
