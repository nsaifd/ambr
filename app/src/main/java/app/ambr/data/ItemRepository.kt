package app.ambr.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDate

class ItemRepository(private val dao: ItemDao) {
    fun activeItems(): Flow<List<ItemWithDetails>> = dao.observeActive()
    fun archivedItems(): Flow<List<ItemWithDetails>> = dao.observeArchived()
    fun item(id: Long): Flow<ItemWithDetails?> = dao.observe(id)
    suspend fun activeCount(): Int = dao.activeCount()
    suspend fun activeOnce(): List<ItemWithDetails> = dao.activeOnce()

    suspend fun save(item: ItemEntity, coverages: List<CoverageEntity>, proofs: List<ProofEntity> = emptyList()): Long {
        val id = if (item.id == 0L) dao.insert(item) else item.id.also { dao.update(item) }
        dao.deleteCoverages(id)
        coverages.forEach { dao.insertCoverage(it.copy(id = 0, itemId = id)) }
        proofs.forEach { dao.insertProof(it.copy(id = 0, itemId = id)) }
        return id
    }

    suspend fun addProof(proof: ProofEntity) = dao.insertProof(proof)
    suspend fun removeProof(proof: ProofEntity) = dao.deleteProof(proof)
    suspend fun updateCoverage(c: CoverageEntity) = dao.updateCoverage(c)

    suspend fun logRepair(repair: RepairEntity, coverageToCount: CoverageEntity? = null) {
        dao.insertRepair(repair)
        coverageToCount?.let { dao.updateCoverage(it.copy(servicesUsed = it.servicesUsed + 1)) }
    }

    suspend fun archive(item: ItemEntity, reason: ArchiveReason, on: LocalDate) =
        dao.update(item.copy(archivedReason = reason, archivedOn = on))

    suspend fun unarchive(item: ItemEntity) = dao.update(item.copy(archivedReason = null, archivedOn = null))

    /** Removes the item, its rows (by cascade) and its photos on disk. */
    suspend fun delete(item: ItemEntity) {
        val files = dao.observe(item.id).first()?.proofs?.map { java.io.File(it.filePath) }.orEmpty() + listOfNotNull(item.photoPath?.let(::File))
        dao.delete(item)
        files.forEach { it.delete() }
    }

    /** A receipt counts as a duplicate when its OCR text shares the same invoice line. */
    suspend fun findDuplicate(fingerprint: String?): ProofEntity? =
        fingerprint?.takeIf { it.length >= 6 }?.let { dao.proofMatching(it) }
}
