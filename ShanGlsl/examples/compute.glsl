#version 460

// A .glsl file with local_size_x is detected as a compute shader.
layout(local_size_x = 64) in;
layout(set = 0, binding = 0) buffer Values {
    float values[];
};

void main() {
    uint index = gl_GlobalInvocationID.x;
    values[index] = sqrt(values[index]);
}
