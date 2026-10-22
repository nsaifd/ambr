package app.ambr.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ambr.AppContainer
import app.ambr.data.CoverageEntity
import app.ambr.data.ItemWithDetails
import app.ambr.data.ProofEntity
import app.ambr.data.ProofKind
import app.ambr.domain.Brands
import app.ambr.domain.ReceiptParser
import app.ambr.platform.Images
import app.ambr.platform.Recognizer
import app.ambr.ui.state.Draft
import app.ambr.ui.state.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

data class Page(val file: File, val kind: ProofKind, val version: Int = 0)

enum class Stage { Camera, Pages, Reading }

data class CaptureState(
    val stage: Stage = Stage.Camera,
    val pages: List<Page> = emptyList(),
    val kind: ProofKind = ProofKind.Receipt,
    val current: Int = 0,
    val busy: Boolean = false,
)

sealed interface CaptureResult {
    /** A new item: the Confirm form takes over. */
    data class NewItem(val draft: Draft, val duplicateOf: ItemWithDetails?) : CaptureResult
    /** Pages added to an existing item. */
    data object Added : CaptureResult
}

class CaptureViewModel(private val c: AppContainer, private val itemId: Long) : ViewModel() {
    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state.asStateFlow()
    val forExistingItem get() = itemId != 0L

    fun newFile(): File = Images.newFile(c.files)
    fun setKind(k: ProofKind) = _state.update { it.copy(kind = k) }

    fun captured(file: File) = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        withContext(Dispatchers.IO) { Images.normalize(file) }
        addPages(listOf(file))
    }

    fun importImages(uris: List<android.net.Uri>) = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        val files = withContext(Dispatchers.IO) { uris.take(5).mapNotNull { Images.import(c.app, it, c.files) } }
        addPages(files)
    }

    fun importPdf(uri: android.net.Uri) = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        val files = withContext(Dispatchers.IO) { runCatching { Images.importPdf(c.app, uri, c.files) }.getOrDefault(emptyList()) }
        addPages(files)
    }

    private fun addPages(files: List<File>) = _state.update { s ->
        if (files.isEmpty()) s.copy(busy = false)
        else s.copy(pages = s.pages + files.map { Page(it, s.kind) }, stage = Stage.Pages, current = s.pages.size, busy = false)
    }

    fun select(i: Int) = _state.update { it.copy(current = i) }
    fun toPages() = _state.update { it.copy(stage = Stage.Pages, current = it.pages.lastIndex) }
    fun backToCamera() = _state.update { it.copy(stage = Stage.Camera) }

    fun retake() = _state.update { s ->
        s.pages.getOrNull(s.current)?.file?.delete()
        val pages = s.pages.filterIndexed { i, _ -> i != s.current }
        s.copy(pages = pages, stage = Stage.Camera, current = (s.current - 1).coerceAtLeast(0))
    }

    fun remove() = _state.update { s ->
        s.pages.getOrNull(s.current)?.file?.delete()
        val pages = s.pages.filterIndexed { i, _ -> i != s.current }
        s.copy(pages = pages, stage = if (pages.isEmpty()) Stage.Camera else Stage.Pages, current = s.current.coerceAtMost((pages.size - 1).coerceAtLeast(0)))
    }

    fun rotate() = viewModelScope.launch {
        val s = _state.value
        val page = s.pages.getOrNull(s.current) ?: return@launch
        withContext(Dispatchers.IO) { Images.rotate(page.file, 90) }
        // Bump the version so the image reloads instead of showing the cached one.
        _state.update { st -> st.copy(pages = st.pages.map { if (it === page) it.copy(version = it.version + 1) else it }) }
    }

    fun setPageKind(k: ProofKind) = _state.update { s -> s.copy(pages = s.pages.mapIndexed { i, p -> if (i == s.current) p.copy(kind = k) else p }) }

    fun discard() {
        _state.value.pages.forEach { it.file.delete() }
        _state.value = CaptureState()
    }

    fun finish(onDone: (CaptureResult) -> Unit) = viewModelScope.launch {
        val pages = _state.value.pages
        if (pages.isEmpty()) return@launch
        _state.update { it.copy(stage = Stage.Reading) }
        val texts = pages.map { Recognizer.read(c.app, it.file) }

        if (forExistingItem) {
            val existing = c.items.item(itemId).first()
            val start = (existing?.proofs?.maxOfOrNull { it.page } ?: -1) + 1
            pages.forEachIndexed { i, p -> c.items.addProof(ProofEntity(itemId = itemId, kind = p.kind, filePath = p.file.path, page = start + i, ocrText = texts[i])) }
            _state.value = CaptureState()
            onDone(CaptureResult.Added)
            return@launch
        }

        val fields = ReceiptParser.parse(texts.joinToString("\n"))
        val duplicate = fields.fingerprint?.let { c.items.findDuplicate(it) }?.let { c.items.item(it.itemId).first() }
        val date = fields.date ?: LocalDate.now()
        val draft = Draft(
            name = fields.name.orEmpty(),
            category = fields.name?.let(Brands::categoryFor),
            brand = fields.brand.orEmpty(),
            model = fields.model.orEmpty(),
            serial = fields.serial.orEmpty(),
            price = fields.priceTaka?.toString().orEmpty(),
            purchaseDate = date,
            store = fields.store.orEmpty(),
            coverages = listOf(CoverageEntity(itemId = 0, label = "Full warranty", startDate = date, months = fields.warrantyMonths ?: Brands.find(fields.brand)?.let { h -> fields.name?.let(Brands::categoryFor)?.let { h.usualMonths[it] } } ?: 12)),
            newProofs = pages.mapIndexed { i, p -> ProofEntity(itemId = 0, kind = p.kind, filePath = p.file.path, page = i, ocrText = texts[i]) },
            unsure = buildSet {
                if (fields.name == null || fields.unsureName) add(Field.Name)
                if (fields.unsurePrice) add(Field.Price)
                if (fields.date == null || fields.unsureDate) add(Field.Date)
            },
            readCount = fields.readCount,
        )
        // The files now belong to the draft; don't delete them if this screen goes away.
        _state.value = CaptureState()
        onDone(CaptureResult.NewItem(draft, duplicate))
    }
}
