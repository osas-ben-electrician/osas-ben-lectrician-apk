package com.example.data.local

import android.content.Context
import android.util.JsonReader
import com.example.data.model.NigeriaDataset
import com.example.data.model.NigeriaLga
import com.example.data.model.NigeriaState
import com.example.data.model.SpainCommunity
import com.example.data.model.SpainDataset
import com.example.data.model.SpainMunicipality
import com.example.data.model.SpainProvince
import com.example.data.model.SupportedCountry
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AdministrativeLocationRepository(private val context: Context) {
    private val mutex = Mutex()
    private var cachedNigeria: NigeriaDataset? = null
    private var cachedSpain: SpainDataset? = null

    suspend fun getNigeriaDataset(): NigeriaDataset = mutex.withLock {
        cachedNigeria?.let { return it }
        return withContext(Dispatchers.IO) {
            context.assets.open("nigeria_locations.json").use { parseNigeriaJson(it) }
        }.also { cachedNigeria = it }
    }

    suspend fun getSpainDataset(): SpainDataset = mutex.withLock {
        cachedSpain?.let { return it }
        return withContext(Dispatchers.IO) {
            context.assets.open("spain_locations.json").use { parseSpainJson(it) }
        }.also { cachedSpain = it }
    }

    suspend fun findCoordinatesForArea(
        country: SupportedCountry,
        region: String,
        province: String?,
        localArea: String
    ): Pair<Double, Double>? {
        return when (country) {
            SupportedCountry.NIGERIA -> {
                val data = getNigeriaDataset()
                val st = data.states.firstOrNull { it.name.equals(region, ignoreCase = true) }
                val lga = st?.lgas?.firstOrNull { it.name.equals(localArea, ignoreCase = true) }
                    ?: data.states.flatMap { it.lgas }.firstOrNull { it.name.equals(localArea, ignoreCase = true) }
                lga?.let { it.lat to it.lon }
            }
            SupportedCountry.SPAIN -> {
                val data = getSpainDataset()
                val com = data.communities.firstOrNull { it.name.equals(region, ignoreCase = true) }
                val prov = com?.provinces?.firstOrNull {
                    province.isNullOrBlank() || it.name.equals(province, ignoreCase = true)
                }
                val mun = prov?.municipalities?.firstOrNull { it.name.equals(localArea, ignoreCase = true) }
                    ?: com?.provinces?.flatMap { it.municipalities }
                        ?.firstOrNull { it.name.equals(localArea, ignoreCase = true) }
                if (mun != null && (mun.lat != 0.0 || mun.lon != 0.0)) {
                    mun.lat to mun.lon
                } else {
                    val fallback = prov?.municipalities?.firstOrNull { it.lat != 0.0 || it.lon != 0.0 }
                    fallback?.let { it.lat to it.lon }
                }
            }
        }
    }

    companion object {
        fun parseNigeriaJson(stream: InputStream): NigeriaDataset {
            var statesCount = 0
            var lgasCount = 0
            val states = mutableListOf<NigeriaState>()
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "statesCount" -> statesCount = reader.nextInt()
                        "lgasCount" -> lgasCount = reader.nextInt()
                        "states" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                var stateName = ""
                                val lgas = mutableListOf<NigeriaLga>()
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "name" -> stateName = reader.nextString()
                                        "lgas" -> {
                                            reader.beginArray()
                                            while (reader.hasNext()) {
                                                var lgaName = ""
                                                var lat = 0.0
                                                var lon = 0.0
                                                reader.beginObject()
                                                while (reader.hasNext()) {
                                                    when (reader.nextName()) {
                                                        "name" -> lgaName = reader.nextString()
                                                        "lat" -> lat = reader.nextDouble()
                                                        "lon" -> lon = reader.nextDouble()
                                                        else -> reader.skipValue()
                                                    }
                                                }
                                                reader.endObject()
                                                lgas.add(NigeriaLga(lgaName, lat, lon))
                                            }
                                            reader.endArray()
                                        }
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                states.add(NigeriaState(stateName, lgas))
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
            return NigeriaDataset(statesCount, lgasCount, states)
        }

        fun parseSpainJson(stream: InputStream): SpainDataset {
            var communitiesCount = 0
            var provincesCount = 0
            var municipalitiesCount = 0
            val communities = mutableListOf<SpainCommunity>()
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "communitiesCount" -> communitiesCount = reader.nextInt()
                        "provincesCount" -> provincesCount = reader.nextInt()
                        "municipalitiesCount" -> municipalitiesCount = reader.nextInt()
                        "communities" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                var cCode = ""
                                var cName = ""
                                var cSubtype = ""
                                val provinces = mutableListOf<SpainProvince>()
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "code" -> cCode = reader.nextString()
                                        "name" -> cName = reader.nextString()
                                        "subtype" -> cSubtype = reader.nextString()
                                        "provinces" -> {
                                            reader.beginArray()
                                            while (reader.hasNext()) {
                                                var pCode = ""
                                                var pName = ""
                                                val municipalities = mutableListOf<SpainMunicipality>()
                                                reader.beginObject()
                                                while (reader.hasNext()) {
                                                    when (reader.nextName()) {
                                                        "code" -> pCode = reader.nextString()
                                                        "name" -> pName = reader.nextString()
                                                        "municipalities" -> {
                                                            reader.beginArray()
                                                            while (reader.hasNext()) {
                                                                var mCode = ""
                                                                var mName = ""
                                                                var lat = 0.0
                                                                var lon = 0.0
                                                                reader.beginObject()
                                                                while (reader.hasNext()) {
                                                                    when (reader.nextName()) {
                                                                        "code" -> mCode = reader.nextString()
                                                                        "name" -> mName = reader.nextString()
                                                                        "lat" -> lat = reader.nextDouble()
                                                                        "lon" -> lon = reader.nextDouble()
                                                                        else -> reader.skipValue()
                                                                    }
                                                                }
                                                                reader.endObject()
                                                                municipalities.add(SpainMunicipality(mCode, mName, lat, lon))
                                                            }
                                                            reader.endArray()
                                                        }
                                                        else -> reader.skipValue()
                                                    }
                                                }
                                                reader.endObject()
                                                provinces.add(SpainProvince(pCode, pName, municipalities))
                                            }
                                            reader.endArray()
                                        }
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                communities.add(SpainCommunity(cCode, cName, cSubtype, provinces))
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
            return SpainDataset(communitiesCount, provincesCount, municipalitiesCount, communities)
        }
    }
}
