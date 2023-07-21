#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform int Strength;
uniform mat4 Kernel;

out vec4 fragColor;

// Gaussian Filter with 31 kernel sizes
void main() {
    if (Strength <= 0) {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }

    vec3 blurred = vec3(0.0);
    for (int r = -15; r < 16; ++r) {
        int kx = abs(r / 4);
        int ky = abs(r % 4);
        float k = Kernel[kx][ky];
        if (k > 0.001) {
            vec4 sample = texture(DiffuseSampler, texCoord + oneTexel * BlurDir * r);
            blurred += sample.rgb * k;
        }
    }
    fragColor = vec4(blurred, 1.0);
}
