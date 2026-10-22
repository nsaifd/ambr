package app.ambr.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Category(val label: String) {
    Phones("Phones & tablets"),
    Computers("Computers"),
    TvAudio("TV & audio"),
    Kitchen("Kitchen appliances"),
    Cooling("Cooling"),
    Laundry("Laundry"),
    Vehicles("Vehicles"),
    Furniture("Furniture"),
    Tools("Tools"),
    Other("Other"),
}

enum class Room(val label: String) {
    LivingRoom("Living room"),
    Kitchen("Kitchen"),
    Bedroom("Bedroom"),
    Bathroom("Bathroom"),
    Office("Office"),
    CarBike("Car / bike"),
    Other("Other"),
}

enum class ArchiveReason { Sold, GaveAway }

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: Category = Category.Other,
    val room: Room = Room.Other,
    val brand: String? = null,
    val model: String? = null,
    val serial: String? = null,
    val priceTaka: Long? = null,
    val purchaseDate: LocalDate? = null,
    val store: String? = null,
    val photoPath: String? = null,
    val notes: String? = null,
    val serviceName: String? = null,
    val servicePhone: String? = null,
    val serviceAddress: String? = null,
    val archivedReason: ArchiveReason? = null,
    val archivedOn: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class ProofKind(val label: String) { Receipt("Receipt"), WarrantyCard("Warranty card"), Invoice("Invoice") }

@Entity(
    tableName = "proofs",
    foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("itemId")],
)
data class ProofEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val kind: ProofKind,
    val filePath: String,
    val page: Int = 0,
    val ocrText: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class CoverageType(val label: String) {
    Manufacturer("Manufacturer"),
    Shop("Shop"),
    Extended("Extended"),
    FreeServices("Free services"),
}

@Entity(
    tableName = "coverages",
    foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("itemId")],
)
data class CoverageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val type: CoverageType = CoverageType.Manufacturer,
    /** What it covers, e.g. "Full warranty" or "Compressor". */
    val label: String = "Full warranty",
    val startDate: LocalDate,
    /** Length in months. Zero for free-service coverages that only count visits. */
    val months: Int,
    val servicesTotal: Int? = null,
    val servicesUsed: Int = 0,
    val remindersSent: Int = 0,
    val reminderDismissed: Boolean = false,
)

enum class RepairOutcome(val label: String) { Fixed("Fixed"), Replaced("Replaced"), Rejected("Rejected"), Pending("Still waiting") }

@Entity(
    tableName = "repairs",
    foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("itemId")],
)
data class RepairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val date: LocalDate,
    val problem: String,
    val outcome: RepairOutcome,
    val costTaka: Long = 0,
    val notes: String? = null,
    val photoPath: String? = null,
)
