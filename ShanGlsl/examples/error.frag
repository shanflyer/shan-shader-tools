#version 460

layout(location = 0) out vec4 fragmentColor;

void main() {
    // Expected: red underline on the next line, with an undeclared identifier diagnostic.
    fragmentColor = vec4(missingVariable, 1.0);
}
