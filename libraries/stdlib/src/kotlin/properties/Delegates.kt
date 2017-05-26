package kotlin.properties

import kotlin.reflect.KProperty

/**
 * Standard property delegates.
 */
public object Delegates {
    /**
     * Returns a property delegate for a read/write property with a non-`null` value that is initialized not during
     * object construction time but at a later time. Trying to read the property before the initial value has been
     * assigned results in an exception.
     */
    public fun <T: Any> notNull(): ReadWriteProperty<Any?, T> = NotNullVar()

    /**
     * Returns a property delegate for a read/write property that has no initial value set
     * and allows it to be set exactly once.
     *
     * - Trying to read the property before the initial value has been assigned results in an [IllegalStateException].
     * - Trying to write the property after the initial value has been assigned also results in an [IllegalStateException].
     */
    @SinceKotlin("1.2")
    public fun <T: Any> initOnce(): ReadWriteProperty<Any?, T> = InitOnce()

    /**
     * Returns a property delegate for a read/write property that calls a specified callback function when changed.
     * @param initialValue the initial value of the property.
     * @param onChange the callback which is called after the change of the property is made. The value of the property
     *  has already been changed when this callback is invoked.
     */
    public inline fun <T> observable(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Unit):
        ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange(property, oldValue, newValue)
        }

    /**
     * Returns a property delegate for a read/write property that calls a specified callback function when changed,
     * allowing the callback to veto the modification.
     * @param initialValue the initial value of the property.
     * @param onChange the callback which is called before a change to the property value is attempted.
     *  The value of the property hasn't been changed yet, when this callback is invoked.
     *  If the callback returns `true` the value of the property is being set to the new value,
     *  and if the callback returns `false` the new value is discarded and the property remains its old value.
     */
    public inline fun <T> vetoable(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Boolean):
        ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
            override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = onChange(property, oldValue, newValue)
        }

}


private class NotNullVar<T: Any>() : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException("Property ${property.name} must be initialized before access to its value.")
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

@JvmVersion
internal class InitOnce<T> : ReadWriteProperty<Any?, T> {

    @Volatile
    private var value: Any? = unset
    // this final field is required to enable safe publication of constructed instance
    private val final = this

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val value = this.value
        if (value === unset) throw IllegalStateException("Property ${property.name} must be initialized before access to its value.")
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (!valueUpdater.compareAndSet(this, unset, value)) {
            throw IllegalStateException("Property ${property.name} can be set only once.")
        }
    }

    private companion object {
        private val unset = Any()
        private val valueUpdater = java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater(
                InitOnce::class.java,
                Any::class.java,
                InitOnce<*>::value.name)
    }
}