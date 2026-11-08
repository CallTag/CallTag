package id.calltag.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emergency_contacts",
    indices = [
        Index(value = ["scope", "province", "city_regency"]),
        Index(value = ["agency_type"])
    ]
)
data class EmergencyContact(
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "scope")
    val scope: String, // NATIONAL atau LOCAL

    @ColumnInfo(name = "agency_type")
    val agencyType: String, // TERPADU, POLISI, DAMKAR, AMBULANS_RS, SAR, PLN, JASA_MARGA

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "province")
    val province: String = "",

    @ColumnInfo(name = "city_regency")
    val cityRegency: String = "",

    @ColumnInfo(name = "short_code")
    val shortCode: String = "",

    @ColumnInfo(name = "primary_phone")
    val primaryPhone: String,

    @ColumnInfo(name = "secondary_phone")
    val secondaryPhone: String = "",

    @ColumnInfo(name = "whatsapp_number")
    val whatsappNumber: String = "",

    @ColumnInfo(name = "whatsapp_template")
    val whatsappTemplate: String = "",

    @ColumnInfo(name = "capabilities_summary")
    val capabilitiesSummary: String = "",

    @ColumnInfo(name = "address")
    val address: String = "",

    @ColumnInfo(name = "is_24_hours")
    val is24Hours: Boolean = true
)
