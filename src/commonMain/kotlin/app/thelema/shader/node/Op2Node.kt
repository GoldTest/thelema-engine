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

package app.thelema.shader.node

/** With this node there is ability to define any operation on input data.
 *
 * If you want change single argument, you can use `setInput("arg1", value)`
 *
 * @author zeganstyl */
class Op2Node(
    in1: IShaderData,
    in2: IShaderData,

    /** You can write complex calculation here, See [GLSL operations](https://en.wikibooks.org/wiki/GLSL_Programming/Vector_and_Matrix_Operations).
     *
     * Arguments will not be converted to result type, you must do it by yourself in code. */
    var function: String = "vec4(in1.xyz + in2.xyz, in1.a)",

    /** See [GLSLType] */
    resultType: String = in1.type
): ShaderNode() {
    constructor(): this(GLSL.oneFloat, GLSL.oneFloat)

    override val componentName: String
        get() = "Op2"

    var in1: IShaderData
        get() = input["in1"] ?: GLSL.oneFloat
        set(value) = setInput("in1", value)

    var in2: IShaderData
        get() = input["in2"] ?: GLSL.oneFloat
        set(value) = setInput("in2", value)

    var resultType: String
        get() = result.type
        set(value) { result.type = value }

    val result = GLSLValue("op", resultType).apply { defOut(this) }

    var isFragment = true
    var isVarying = true

    init {
        this.in1 = in1
        this.in2 = in2
    }

    override fun executionVert(out: StringBuilder) {
        if (!isFragment) {
            val f = function.replace("in1", in1.ref).replace("in2", in2.ref)
            if (isVarying) {
                out.append("${result.ref} = $f;\n")
            } else {
                out.append("${result.typedRef} = $f;\n")
            }
        }
    }

    override fun declarationVert(out: StringBuilder) {
        super.declarationVert(out)

        if (isVarying) {
            out.append("varying ${result.typedRef};\n")
        }
    }

    override fun declarationFrag(out: StringBuilder) {
        super.declarationFrag(out)

        if (isVarying) {
            out.append("varying ${result.typedRef};\n")
        }
    }

    override fun executionFrag(out: StringBuilder) {
        if (isFragment) {
            var f = function
            input.forEach {
                f = f.replace(it.key, it.value.ref)
            }
            out.append("${result.typedRef} = $f;\n")
        }
    }
}