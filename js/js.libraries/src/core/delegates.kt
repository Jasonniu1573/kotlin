/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.properties

import kotlin.reflect.KProperty

internal class InitOnce<T> : ReadWriteProperty<Any?, T> {

    private var value: Any? = unset

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val value = this.value
        if (value === unset) throw IllegalStateException("Property ${property.name} must be initialized before get.")
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (this.value !== unset) {
            throw IllegalStateException("Property ${property.name} can be set only once.")
        }
        this.value = value
    }

    private companion object {
        private val unset = Any()
    }
}