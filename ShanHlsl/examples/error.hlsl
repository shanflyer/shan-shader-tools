float4 PSMain(float2 uv : TEXCOORD0) : SV_Target {
    // Expected: a live error for this unknown name, without running DXC.
    return float4(missingColor, 1.0);
}
