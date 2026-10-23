package app.ambr.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ambr.data.Category
import app.ambr.data.CoverageEntity
import app.ambr.data.CoverageType
import app.ambr.data.ItemEntity
import app.ambr.data.ItemRepository
import app.ambr.data.ProofEntity
import app.ambr.data.Room
import app.ambr.domain.BrandHint
import app.ambr.domain.Brands
import app.ambr.platform.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

enum class Field { Name, Store, Price, Date, Brand, Model, Serial }

/** What the receipt showed, offered as tap-to-fill choices under the form's fields. */
data class Suggestions(
    val names: List<String> = emptyList(),
    val stores: List<String> = emptyList(),
    val prices: List<Long> = emptyList(),
    val dates: List<LocalDate> = emptyList(),
    /** Everything the reader saw, so the user can check it or copy from it. */
    val rawText: String = "",
)

/** The form's working copy. Text fields stay strings until save so typing is never fought. */
data class Draft(
    val id: Long = 0,
    val name: String = "",
    val category: Category? = null,
    val room: Room? = null,
    val brand: String = "",
    val model: String = "",
    val serial: String = "",
    val price: String = "",
    val purchaseDate: LocalDate? = LocalDate.now(),
    val store: String = "",
    val notes: String = "",
    val serviceName: String = "",
    val servicePhone: String = "",
    val serviceAddress: String = "",
    val photoPath: String? = null,
    val coverages: List<CoverageEntity> = emptyList(),
    val newProofs: List<ProofEntity> = emptyList(),
    /** Fields the receipt reader was not sure about. Cleared as the user edits them. */
    val unsure: Set<Field> = emptySet(),
    val readCount: Int = 0,
    val suggestions: Suggestions? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val priceTaka: Long? get() = price.filter { it.isDigit() }.toLongOrNull()
    val hint: BrandHint? get() = Brands.find(brand.ifBlank { name })
    val canSave get() = name.isNotBlank()
}

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult
    data object LimitReached : SaveResult
}

class EditorViewModel(private val repo: ItemRepository, private val settings: Settings, private val id: Long, pending: Draft?) : ViewModel() {
    private val _draft = MutableStateFlow(pending ?: Draft())
    val draft: StateFlow<Draft> = _draft.asStateFlow()
    val isNew get() = id == 0L

    init {
        if (id == 0L && _draft.value.coverages.isEmpty()) setMainMonths(12)
        if (id != 0L) viewModelScope.launch {
            val d = repo.item(id).first() ?: return@launch
            val i = d.item
            _draft.value = Draft(
                id = i.id, name = i.name, category = i.category, room = i.room, brand = i.brand.orEmpty(), model = i.model.orEmpty(),
                serial = i.serial.orEmpty(), price = i.priceTaka?.toString().orEmpty(), purchaseDate = i.purchaseDate, store = i.store.orEmpty(),
                notes = i.notes.orEmpty(), serviceName = i.serviceName.orEmpty(), servicePhone = i.servicePhone.orEmpty(),
                serviceAddress = i.serviceAddress.orEmpty(), photoPath = i.photoPath, coverages = d.coverages, createdAt = i.createdAt,
            )
        }
    }

    fun edit(field: Field? = null, change: (Draft) -> Draft) = _draft.update { d ->
        change(d).let { n -> if (field != null) n.copy(unsure = n.unsure - field) else n }
    }

    /** The main warranty length chip; keeps any extra coverages the user added. */
    fun setMainMonths(months: Int) = _draft.update { d ->
        val start = d.purchaseDate ?: LocalDate.now()
        val main = d.coverages.firstOrNull { it.type != CoverageType.FreeServices && it.label == "Full warranty" }
        val updated = main?.copy(months = months, startDate = start) ?: CoverageEntity(itemId = d.id, label = "Full warranty", startDate = start, months = months)
        d.copy(coverages = listOf(updated) + d.coverages.filter { it !== main })
    }

    fun addCoverage(c: CoverageEntity) = _draft.update { it.copy(coverages = it.coverages + c) }
    fun replaceCoverage(old: CoverageEntity, new: CoverageEntity?) = _draft.update { d ->
        d.copy(coverages = d.coverages.mapNotNull { if (it === old) new else it })
    }

    fun setDate(date: LocalDate) = _draft.update { d ->
        // Coverages that began on the old purchase date follow the new one.
        d.copy(purchaseDate = date, unsure = d.unsure - Field.Date, coverages = d.coverages.map { if (it.startDate == d.purchaseDate) it.copy(startDate = date) else it })
    }

    fun save(onResult: (SaveResult) -> Unit) = viewModelScope.launch {
        val d = _draft.value
        if (!d.canSave) return@launch
        if (isNew && !settings.isPro.value && repo.activeCount() >= Settings.FREE_LIMIT) { onResult(SaveResult.LimitReached); return@launch }
        val hint = d.hint
        val item = ItemEntity(
            id = d.id, name = d.name.trim(), category = d.category ?: Brands.categoryFor(d.name) ?: Category.Other, room = d.room ?: Room.Other,
            brand = d.brand.trim().ifBlank { hint?.brand }, model = d.model.trim().ifBlank { null }, serial = d.serial.trim().ifBlank { null },
            priceTaka = d.priceTaka, purchaseDate = d.purchaseDate, store = d.store.trim().ifBlank { null }, photoPath = d.photoPath,
            notes = d.notes.trim().ifBlank { null }, serviceName = d.serviceName.trim().ifBlank { hint?.serviceName },
            servicePhone = d.servicePhone.trim().ifBlank { null }, serviceAddress = d.serviceAddress.trim().ifBlank { null }, createdAt = d.createdAt,
        )
        val saved = repo.save(item, d.coverages, d.newProofs)
        onResult(SaveResult.Saved(saved))
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        repo.item(id).first()?.let { repo.delete(it.item) }
        onDone()
    }

    /** Leaving a new item without saving removes the photos taken for it. */
    fun discard() {
        if (isNew) _draft.value.newProofs.forEach { File(it.filePath).delete() }
    }
}
