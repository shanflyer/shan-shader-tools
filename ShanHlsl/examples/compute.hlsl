RWStructuredBuffer<float> values : register(u0);

[numthreads(64, 1, 1)]
void CSMain(uint3 dispatchId : SV_DispatchThreadID) {
    values[dispatchId.x] = sqrt(values[dispatchId.x]);
}
