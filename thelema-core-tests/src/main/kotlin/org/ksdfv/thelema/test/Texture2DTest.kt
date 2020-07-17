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

package org.ksdfv.thelema.test

import org.ksdfv.thelema.gl.GL
import org.ksdfv.thelema.gl.GL_LINEAR
import org.ksdfv.thelema.mesh.ScreenQuad
import org.ksdfv.thelema.texture.Texture2D

/** @author zeganstyl */
class Texture2DTest: Test("Texture 2D") {
    override fun testMain() {
        val screenQuad = ScreenQuad.TextureRenderer()

        val texture = Texture2D(
            url = "thelema-logo.png",
            minFilter = GL_LINEAR,
            magFilter = GL_LINEAR
        )

        GL.render {
            screenQuad.render(texture)
        }
    }
}
