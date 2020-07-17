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

package org.ksdfv.thelema.teavm

import org.ksdfv.thelema.data.IDataArray
import org.teavm.jso.typedarrays.ArrayBufferView

abstract class TvmArrayBufferView<T>: IDataArray<T> {
    abstract val array: ArrayBufferView

    override val sourceObject: Any
        get() = array

    override var position: Int = 0

    override val capacity: Int
        get() = array.length

    override fun rewind() {}

    override fun flip() {}
}