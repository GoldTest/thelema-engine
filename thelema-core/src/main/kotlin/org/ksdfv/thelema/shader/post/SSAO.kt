/*
 * Copyright 2020 Anton Trushkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ksdfv.thelema.shader.post

import org.intellij.lang.annotations.Language
import org.ksdfv.thelema.data.DATA
import org.ksdfv.thelema.g3d.ActiveCamera
import org.ksdfv.thelema.gl.*
import org.ksdfv.thelema.mesh.IScreenQuad
import org.ksdfv.thelema.texture.IFrameBuffer
import org.ksdfv.thelema.texture.ITexture
import org.ksdfv.thelema.texture.SimpleFrameBuffer
import org.ksdfv.thelema.texture.Texture2D
import kotlin.random.Random

/** @author zeganstyl */
class SSAO(
    var colorMap: ITexture,
    var normalMap: ITexture,
    var viewSpacePositionMap: ITexture,

    auxBufferSSAO: IFrameBuffer = SimpleFrameBuffer(
        GL.mainFrameBufferWidth,
        GL.mainFrameBufferHeight,
        GL_RGB
    ),
    var auxBufferBlur: IFrameBuffer = SimpleFrameBuffer(
        GL.mainFrameBufferWidth,
        GL.mainFrameBufferHeight,
        GL_RGB
    ),
    val noiseTextureSideSize: Int = 4,
    occlusionSamplesNum: Int = 64
): PostShader(ssaoCode(occlusionSamplesNum)) {
    // generate noise texture
    var noiseTexture: Texture2D = Texture2D()

    private var uProjectionMatrix = -1

    var noiseMapUnit = GL.grabTextureUnit()
        private set

    var normalMapUnit = GL.grabTextureUnit()
        private set

    var positionMapUnit = GL.grabTextureUnit()
        private set

    var auxBufferSsaoUnit = GL.grabTextureUnit()
        private set

    var auxBufferBlurUnit = GL.grabTextureUnit()
        private set

    var colorMapUnit = GL.grabTextureUnit()
        private set

    val blurShader = PostShader(ssaoBlurCode)

    val combineShader = PostShader(ssaoCombineCode)

    var radius = 0.1f
        set(value) {
            field = value
            bind()
            set("uRadius", radius)
        }

    var range = 300f
        set(value) {
            field = value
            bind()
            set("uRange", range)
        }

    var bias = 0.05f
        set(value) {
            field = value
            bind()
            set("uBias", bias)
        }

    /** Auxiliary buffer for caching intermediate result. Caches SSAO result */
    var auxBufferSsao: IFrameBuffer = auxBufferSSAO
        set(value) {
            field = value

            bind()
            set(
                "uNoiseScale",
                value.width.toFloat()/noiseTextureSideSize.toFloat(),
                value.height.toFloat()/noiseTextureSideSize.toFloat()
            )
        }

    var visualizeSsao: Boolean = false

    init {
        uProjectionMatrix = this["uProjectionMatrix"]

        val ssaoNoiseBuffer = DATA.bytes(noiseTextureSideSize * noiseTextureSideSize * 3 * 4)
        ssaoNoiseBuffer.apply {
            var index = 0
            for (i in 0 until noiseTextureSideSize * noiseTextureSideSize) {
                put(index, Random.nextBits(8).toByte())
                index++
                put(index, Random.nextBits(8).toByte())
                index++
                put(index, 0)
                index++
            }
            position = 0
        }

        noiseTexture.load(
            width = noiseTextureSideSize,
            height = noiseTextureSideSize,
            pixelFormat = GL_RGB,
            type = GL_UNSIGNED_BYTE,
            pixels = ssaoNoiseBuffer,
            internalFormat = GL_RGB,
            minFilter = GL_NEAREST,
            magFilter = GL_NEAREST,
            sWrap = GL_REPEAT,
            tWrap = GL_REPEAT
        )

        bind()
        this["uRadius"] = radius
        this["uRange"] = range
        this["uBias"] = bias
        this["uNormalMap"] = normalMapUnit
        this["uNoiseTexture"] = noiseMapUnit
        this["uViewSpacePositionMap"] = positionMapUnit
        set3("uOcclusionSamples", FloatArray(occlusionSamplesNum * 3) { Random.nextFloat() })
        set("uNoiseScale", auxBufferSSAO.width.toFloat()/noiseTextureSideSize.toFloat(), auxBufferSSAO.height.toFloat()/noiseTextureSideSize.toFloat())

        blurShader.bind()
        blurShader["uSsaoMap"] = auxBufferSsaoUnit

        val numOffsets = 4
        val startOffset = -numOffsets * 0.5f + 0.5f
        for (i in 0 until numOffsets) {
            val offset = startOffset + i
            blurShader["uSsaoOffsetsX[$i]"] = offset / auxBufferBlur.width
            blurShader["uSsaoOffsetsY[$i]"] = offset / auxBufferBlur.height
        }

        combineShader.bind()
        combineShader["uColorMap"] = colorMapUnit
        combineShader["uSsaoMap"] = auxBufferBlurUnit
    }

    /** By default result will be saved to [auxBufferSsao] */
    fun render(screenQuad: IScreenQuad, out: IFrameBuffer? = auxBufferSsao) {
        val camera = ActiveCamera
        if (visualizeSsao) {
            screenQuad.render(this, null) {
                if (camera.projectionMatrix.det() != 0f) {
                    this@SSAO[uProjectionMatrix] = camera.projectionMatrix

                    normalMap.bind(normalMapUnit)
                    noiseTexture.bind(noiseMapUnit)
                    viewSpacePositionMap.bind(positionMapUnit)
                }
            }
        } else {
            screenQuad.render(this, auxBufferSsao) {
                if (camera.projectionMatrix.det() != 0f) {
                    this@SSAO[uProjectionMatrix] = camera.projectionMatrix

                    normalMap.bind(normalMapUnit)
                    noiseTexture.bind(noiseMapUnit)
                    viewSpacePositionMap.bind(positionMapUnit)
                }
            }

            screenQuad.render(blurShader, auxBufferBlur) {
                auxBufferSsao.getTexture(0).bind(auxBufferSsaoUnit)
            }

            screenQuad.render(combineShader, out) {
                colorMap.bind(colorMapUnit)
                auxBufferBlur.getTexture(0).bind(auxBufferBlurUnit)
            }
        }
    }

    override fun destroy() {
        super.destroy()
        blurShader.destroy()
        combineShader.destroy()
        noiseTexture.destroy()
    }

    companion object {
        // https://habr.com/ru/post/421385/
        // https://github.com/JoeyDeVries/LearnOpenGL/blob/master/src/5.advanced_lighting/9.ssao/9.ssao.fs
        @Language("GLSL")
        fun ssaoCode(occlusionSamplesNum: Int = 64): String = """
varying vec2 uv;

uniform sampler2D uNormalMap;
uniform sampler2D uViewSpacePositionMap;

uniform mat4 uProjectionMatrix;

uniform sampler2D uNoiseTexture;
// tile noise texture over screen based on screen dimensions divided by noise size
uniform vec2 uNoiseScale;

uniform vec3 uOcclusionSamples[$occlusionSamplesNum];

// parameters
uniform float uRadius;
uniform float uStrength;
uniform float uBias;
uniform float uRange;

void main() {
    vec3 fragPos = texture2D(uViewSpacePositionMap, uv).rgb;
    vec3 normal = normalize(texture2D(uNormalMap, uv).rgb);
    vec3 randomVec = normalize(texture2D(uNoiseTexture, uv * uNoiseScale).xyz);
    // create TBN change-of-basis matrix: from tangent-space to view-space
    vec3 tangent = normalize(randomVec - normal * dot(randomVec, normal));
    vec3 bitangent = cross(normal, tangent);
    mat3 TBN = mat3(tangent, bitangent, normal);
    // iterate over the sample kernel and calculate occlusion factor
    float occlusion = 0.0;
    if (length(fragPos) < uRange) {
        for(int i = 0; i < $occlusionSamplesNum; ++i) {
            // get sample position
            vec3 samp = TBN * uOcclusionSamples[i]; // from tangent to view-space
            samp = fragPos + samp * uRadius;

            // project sample position (to sample texture) (to get position on screen/texture)
            vec4 offset = vec4(samp, 1.0);
            offset = uProjectionMatrix * offset; // from view to clip-space
            offset.xyz /= offset.w; // perspective divide
            offset.xyz = offset.xyz * 0.5 + 0.5; // transform to range 0.0 - 1.0

            // get sample depth
            float sampleDepth = texture2D(uViewSpacePositionMap, offset.xy).z; // get depth value of kernel sample
            // range check & accumulate
            float rangeCheck = smoothstep(0.0, 1.0, uRadius / abs(fragPos.z - sampleDepth));
            occlusion += (sampleDepth >= samp.z + uBias ? 1.0 : 0.0) * rangeCheck;
        }
    }

    occlusion = 1.0 - (occlusion / $occlusionSamplesNum.0);

    gl_FragColor = vec4(occlusion, occlusion, occlusion, 1.0);
}"""

        @Language("GLSL")
        val ssaoBlurCode: String = """
varying vec2 uv;

uniform sampler2D uSsaoMap;
uniform float uSsaoOffsetsX[4];
uniform float uSsaoOffsetsY[4];

void main() {
    vec3 Color = vec3(0.0);

    for (int i = 0 ; i < 4 ; i++) {
        for (int j = 0 ; j < 4 ; j++) {
            vec2 tc = uv;
            tc.x = uv.x + uSsaoOffsetsX[j];
            tc.y = uv.y + uSsaoOffsetsY[i];
            Color += texture2D(uSsaoMap, tc).rgb;
        }
    }

    Color /= 16.0;
    float alpha = texture2D(uSsaoMap, uv).a;

    gl_FragColor = vec4(Color, alpha);
}"""

        @Language("GLSL")
        val ssaoCombineCode: String = """
varying vec2 uv;

uniform sampler2D uColorMap;
uniform sampler2D uSsaoMap;

void main() {
    vec4 c = texture2D(uColorMap, uv);
    gl_FragColor = vec4(c.xyz * texture2D(uSsaoMap, uv).r, c.a);
}"""
    }
}