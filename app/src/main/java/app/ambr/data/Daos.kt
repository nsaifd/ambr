package app.ambr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ItemWithDetails(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId") val coverages: List<CoverageEntity>,
    @Relation(parentColumn = "id", entityColumn = "itemId") val proofs: List<ProofEntity>,
    @Relation(parentColumn = "id", entityColumn = "itemId") val repairs: List<RepairEntity>,
)

@Dao
interface ItemDao {
    @Transaction
    @Query("SELECT * FROM items WHERE archivedReason IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<ItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM items WHERE archivedReason IS NOT NULL ORDER BY archivedOn DESC")
    fun observeArchived(): Flow<List<ItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: Long): Flow<ItemWithDetails?>

    @Transaction
    @Query("SELECT * FROM items WHERE archivedReason IS NULL")
    suspend fun activeOnce(): List<ItemWithDetails>

    @Transaction
    @Query("SELECT * FROM items WHERE archivedReason IS NOT NULL")
    suspend fun archivedOnce(): List<ItemWithDetails>

    @Query("SELECT COUNT(*) FROM items WHERE archivedReason IS NULL")
    suspend fun activeCount(): Int

    @Insert suspend fun insert(item: ItemEntity): Long
    @Update suspend fun update(item: ItemEntity)
    @Delete suspend fun delete(item: ItemEntity)

    @Insert suspend fun insertCoverage(coverage: CoverageEntity): Long
    @Update suspend fun updateCoverage(coverage: CoverageEntity)
    @Query("DELETE FROM coverages WHERE itemId = :itemId") suspend fun deleteCoverages(itemId: Long)

    @Insert suspend fun insertProof(proof: ProofEntity): Long
    @Delete suspend fun deleteProof(proof: ProofEntity)

    @Insert suspend fun insertRepair(repair: RepairEntity): Long

    @Query("SELECT * FROM proofs WHERE ocrText LIKE '%' || :fingerprint || '%' LIMIT 1")
    suspend fun proofMatching(fingerprint: String): ProofEntity?
}
