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

import org.intellij.lang.annotations.Language
import org.ksdfv.thelema.g3d.cam.ActiveCamera
import org.ksdfv.thelema.g3d.cam.Camera
import org.ksdfv.thelema.g3d.Object3D
import org.ksdfv.thelema.g3d.Scene
import org.ksdfv.thelema.math.Mat4
import org.ksdfv.thelema.mesh.build.BoxMeshBuilder
import org.ksdfv.thelema.shader.Shader

/** @author zeganstyl */
class CubeModel(val shader: Shader? = defaultShader()) {
    val mesh = BoxMeshBuilder(xSize = 2f, ySize = 2f, zSize = 2f).build()

    val obj = Object3D().apply { meshes.add(mesh) }

    val cubeMatrix4 = Mat4()

    val scene = Scene()

    init {
        scene.objects.add(obj)

        ActiveCamera.api = Camera().apply {
            position.set(0f, 3f, -3f)
            direction.set(position).nor().scl(-1f)
            updateTransform()
            near = 1f
            far = 10f
            update()
        }
    }

    fun update() {
        cubeMatrix4.rotate(0f, 1f, 0f, 0.01f)

        if (shader != null) {
            obj.node.worldMatrix.set(cubeMatrix4).mulLeft(ActiveCamera.viewProjectionMatrix)
        } else {
            obj.node.worldMatrix.set(cubeMatrix4)
        }
    }

    fun render() {
        if (shader != null) {
            shader.bind()
            shader["projViewModelTrans"] = obj.node.worldMatrix
            mesh.render(shader)
        }
    }

    companion object {
        @Language("GLSL")
        fun defaultShader() = Shader(
            vertCode = """            
attribute vec4 aPosition;
varying vec3 vPosition;
uniform mat4 projViewModelTrans;

void main() {
    vPosition = aPosition.xyz;
    gl_Position = projViewModelTrans * aPosition;
}""",
            fragCode = """
varying vec3 vPosition;
void main() {
    gl_FragColor = vec4(vPosition, 1.0);
}""")
    }
}