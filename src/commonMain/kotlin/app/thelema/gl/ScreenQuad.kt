/*
 * Copyright 2020-2021 Anton Trushkov
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

package app.thelema.gl

import app.thelema.img.IFrameBuffer
import app.thelema.img.ITexture
import app.thelema.img.renderNoClear
import app.thelema.shader.IShader
import app.thelema.shader.post.PostShader
import kotlin.native.concurrent.ThreadLocal

/** Default screen quad, that may be used for post processing shaders.
 *
 * Attribute names: POSITION - vertex position, UV - texture coordinates
 *
 * @author zeganstyl */
@ThreadLocal
object ScreenQuad {
    var defaultClearMask: Int? = GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT

    val mesh: IMesh by lazy {
        Mesh {
            primitiveType = GL_TRIANGLE_FAN

            addVertexBuffer {
                addAttribute(Vertex.POSITION)
                addAttribute(Vertex.TEXCOORD_0)
                initVertexBuffer(4) {
                    putFloats(-1f, -1f,  0f, 0f)
                    putFloats(1f, -1f,  1f, 0f)
                    putFloats(1f, 1f,  1f, 1f)
                    putFloats(-1f, 1f,  0f, 1f)
                }
            }
        }
    }

    val textureRenderShader: IShader by lazy {
        PostShader(
            flipY = false,
            fragCode = """            
varying vec2 uv;
uniform sampler2D tex;

void main() {
    gl_FragColor = texture2D(tex, uv);
}"""
        )
    }

    val flippedTextureRenderShader: IShader by lazy {
        PostShader(
            flipY = true,
            fragCode = """            
varying vec2 uv;
uniform sampler2D tex;

void main() {
    gl_FragColor = texture2D(tex, uv);
}"""
        )
    }

    fun render(
        texture: ITexture,
        flipY: Boolean = true,
        out: IFrameBuffer? = null,
        clearMask: Int? = defaultClearMask
    ) {
        texture.bind(0)
        render(if (flipY) flippedTextureRenderShader else textureRenderShader, out, clearMask)
    }

    /** Render 2D texture on screen */
    fun render(
        textureHandle: Int,
        flipY: Boolean = true,
        out: IFrameBuffer? = null,
        clearMask: Int? = defaultClearMask
    ) {
        GL.activeTexture = 0
        GL.glBindTexture(GL_TEXTURE_2D, textureHandle)
        render(if (flipY) flippedTextureRenderShader else textureRenderShader, out, clearMask)
    }

    /**
     * Render screen quad mesh with shader
     * @param clearMask if null, glClear will not be called
     * @param set may be used to update shader uniforms. Called after shader bound, so you may not to bind it again.
     * */
    fun render(
        shader: IShader,
        out: IFrameBuffer? = null,
        clearMask: Int? = defaultClearMask
    ) {
        if (out != null) {
            out.renderNoClear {
                if (clearMask != null) GL.glClear(clearMask)
                mesh.render(shader)
            }
        } else {
            if (clearMask != null) GL.glClear(clearMask)
            mesh.render(shader)
        }
    }
}