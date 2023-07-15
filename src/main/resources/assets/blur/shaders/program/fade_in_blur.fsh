#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform float Radius;
uniform float Progress;
uniform mat4 Kernel;

out vec4 fragColor;

void main() {
    int progRadius = int(floor(Radius * Progress));
    if (progRadius == 0) {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }

    vec3 blurred = vec3(0.0);
    float totalAlpha = 0.0;
    for (int r = -15; r < 16; ++r) {
        int kx = abs(r / 4);
        int ky = abs(r % 4);
        vec4 sample = texture(DiffuseSampler, texCoord + oneTexel * BlurDir * (r / 2.0));
        blurred += sample.rgb * Kernel[kx][ky];
        totalAlpha += sample.a;
    }
    fragColor = vec4(blurred, totalAlpha);
}
