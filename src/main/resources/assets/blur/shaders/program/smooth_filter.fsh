#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform int Strength;
uniform int EffectMax;
uniform int DeltaEnd;

out vec4 fragColor;

// Smooth Filter
void main() {
    // clampedLerp
    float delta = clamp(float(Strength) / DeltaEnd, 0.0, 1.0);
    int effect = int(ceil(delta * EffectMax));

    if (effect == 0) {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }

    vec3 smoothed = vec3(0.0);
    for (int r = -effect; r <= effect; ++r) {
        vec4 sample = texture(DiffuseSampler, texCoord + oneTexel * BlurDir * r);
        smoothed += sample.rgb;
    }
    fragColor = vec4(smoothed / (effect * 2.0 + 1.0), 1.0);
}
