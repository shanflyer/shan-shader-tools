#include "lighting.hlsli"

struct VertexInput {
    float3 position : POSITION;
    float3 color : COLOR0;
};

struct PixelInput {
    float4 position : SV_Position;
    float3 color : COLOR0;
};

PixelInput VSMain(VertexInput input) {
    PixelInput output;
    output.position = float4(input.position, 1.0);
    output.color = input.color;
    return output;
}

float4 PSMain(PixelInput input) : SV_Target {
    float3 color = ApplyLighting(input.color, float3(0.0, 0.0, 1.0));
    return float4(color, 1.0);
}

#if 0
// This region should be gray and its inactive error should not be reported.
float excludedValue = missingValue;
#endif
