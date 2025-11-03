package at.sunilson.justlift.features.workout.presentation.preview

import com.juul.kable.Characteristic
import com.juul.kable.Descriptor
import com.juul.kable.DiscoveredService
import com.juul.kable.ExperimentalApi
import com.juul.kable.Identifier
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class FakePeripheral(
    peripheralName: String
) : Peripheral {

    override val scope: CoroutineScope
        get() = TODO("Not yet implemented")

    override val state: StateFlow<State>
        get() = TODO("Not yet implemented")

    override val identifier: Identifier
        get() = TODO("Not yet implemented")

    @ExperimentalApi
    override val name: String = peripheralName

    override val services: StateFlow<List<DiscoveredService>?>
        get() = TODO("Not yet implemented")

    override suspend fun connect(): CoroutineScope {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }

    override suspend fun maximumWriteValueLengthForType(writeType: WriteType): Int {
        TODO("Not yet implemented")
    }

    @ExperimentalApi
    override suspend fun rssi(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun read(characteristic: Characteristic): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun write(characteristic: Characteristic, data: ByteArray, writeType: WriteType) {
        TODO("Not yet implemented")
    }

    override suspend fun read(descriptor: Descriptor): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun write(descriptor: Descriptor, data: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun observe(
        characteristic: Characteristic,
        onSubscription: suspend () -> Unit
    ): Flow<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}
