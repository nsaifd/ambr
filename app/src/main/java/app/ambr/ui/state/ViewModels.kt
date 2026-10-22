package app.ambr.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ambr.data.ArchiveReason
import app.ambr.data.Category
import app.ambr.data.CoverageEntity
import app.ambr.data.ItemEntity
import app.ambr.data.ItemRepository
import app.ambr.data.ItemWithDetails
import app.ambr.data.ProofEntity
import app.ambr.data.RepairEntity
import app.ambr.data.Room
import app.ambr.domain.ItemStatus
import app.ambr.domain.StatusKind
import app.ambr.domain.Warranty
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** An item plus its computed warranty status, ready for any list or card. */
data class ItemUi(val details: ItemWithDetails, val status: ItemStatus) {
    val item get() = details.item
    val id get() = item.id
}

fun ItemWithDetails.toUi(today: LocalDate = LocalDate.now()) = ItemUi(this, Warranty.status(coverages, today))

data class Overview(
    val items: List<ItemUi> = emptyList(),
    val loaded: Boolean = false,
) {
    val totalValue: Long get() = items.filter { it.status.kind == StatusKind.Covered || it.status.kind == StatusKind.EndsSoon }.sumOf { it.item.priceTaka ?: 0 }
    val covered get() = items.count { it.status.kind == StatusKind.Covered }
    val endingSoon get() = items.filter { it.status.kind == StatusKind.EndsSoon }.sortedBy { it.status.daysLeft }
    val expired get() = items.count { it.status.kind == StatusKind.Expired || it.status.kind == StatusKind.NoCoverage }
    val protectedCount get() = covered + endingSoon.size
    val recent get() = items.sortedByDescending { it.item.createdAt }.take(6)
}

class ItemsViewModel(private val repo: ItemRepository) : ViewModel() {
    val overview: StateFlow<Overview> = repo.activeItems()
        .map { list -> Overview(list.map { it.toUi() }, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Overview())

    val archived: StateFlow<List<ItemUi>> = repo.archivedItems()
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unarchive(item: ItemEntity) = viewModelScope.launch { repo.unarchive(item) }

    /** Debug builds only: fills the vault with realistic examples so every screen can be tried. */
    fun loadSamples() = viewModelScope.launch {
        val today = LocalDate.now()
        fun add(name: String, cat: Category, room: Room, brand: String, price: Long, bought: LocalDate, months: Int, store: String, serial: String, extra: List<CoverageEntity> = emptyList()) =
            Triple(ItemEntity(name = name, category = cat, room = room, brand = brand, priceTaka = price, purchaseDate = bought, store = store, serial = serial,
                serviceName = "$brand Service Center", servicePhone = "09612-000000"),
                listOf(CoverageEntity(itemId = 0, label = "Full warranty", startDate = bought, months = months)) + extra, Unit)
        val samples = listOf(
            add("Gree 1.5 ton AC", Category.Cooling, Room.Bedroom, "Gree", 78_500, today.minusMonths(31).minusDays(8), 36, "Gree Brand Shop", "GR18K24030917",
                listOf(CoverageEntity(itemId = 0, type = app.ambr.data.CoverageType.FreeServices, label = "Free servicing", startDate = today.minusMonths(31), months = 0, servicesTotal = 4, servicesUsed = 3))),
            add("Walton fridge", Category.Kitchen, Room.Kitchen, "Walton", 52_990, today.minusDays(2), 12, "Walton Plaza", "WFE3E8-2610",
                listOf(CoverageEntity(itemId = 0, label = "Compressor", startDate = today.minusDays(2), months = 120))),
            add("Samsung 43\" TV", Category.TvAudio, Room.LivingRoom, "Samsung", 46_000, today.minusYears(2).plusDays(15), 24, "Samsung Plaza", "SM43T5-1188"),
            add("Redmi Note 13", Category.Phones, Room.Bedroom, "Xiaomi", 24_999, today.minusYears(1).plusDays(8), 12, "Mi Store", "RN13-77821"),
            add("Yamaha FZ", Category.Vehicles, Room.CarBike, "Yamaha", 2_15_000, today.minusMonths(16), 24, "Yamaha Tejgaon", "FZV3-11902"),
            add("Vision ceiling fan", Category.Cooling, Room.Bedroom, "Vision", 4_200, today.minusMonths(33), 24, "Vision showroom", "VF-56-23110"),
        )
        samples.forEach { (item, coverages, _) -> repo.save(item, coverages) }
    }
}

class ItemViewModel(private val repo: ItemRepository, val id: Long) : ViewModel() {
    val item: StateFlow<ItemUi?> = repo.item(id)
        .map { it?.toUi() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun archive(reason: ArchiveReason, on: LocalDate) = item.value?.let { viewModelScope.launch { repo.archive(it.item, reason, on) } }
    fun delete(onDone: () -> Unit) = item.value?.let { viewModelScope.launch { repo.delete(it.item); onDone() } }
    fun addProof(proof: ProofEntity) = viewModelScope.launch { repo.addProof(proof) }
    fun removeProof(proof: ProofEntity) = viewModelScope.launch {
        repo.removeProof(proof)
        java.io.File(proof.filePath).delete()
    }
    fun logRepair(repair: RepairEntity, countService: Boolean) = viewModelScope.launch {
        val free = item.value?.details?.coverages?.firstOrNull { it.servicesTotal != null && it.servicesUsed < it.servicesTotal }
        repo.logRepair(repair.copy(itemId = id), if (countService) free else null)
    }
    fun dismissReminder() = item.value?.details?.coverages?.forEach { viewModelScope.launch { repo.updateCoverage(it.copy(reminderDismissed = true)) } }
}
