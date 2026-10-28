#ifndef SHAN_LIGHTING_HLSLI
#define SHAN_LIGHTING_HLSLI

float3 ApplyLighting(float3 color, float3 normal) {
    const float diffuse = saturate(dot(normalize(normal), normalize(float3(0.3, 0.8, 0.5))));
    return color * (0.2 + 0.8 * diffuse);
}

#endif
