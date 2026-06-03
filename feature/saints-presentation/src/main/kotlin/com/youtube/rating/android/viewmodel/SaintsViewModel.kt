package com.youtube.rating.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.youtube.rating.android.data.prefs.SaintsPrefs
import com.youtube.rating.core.coroutines.makeIOCall

data class Saint(
    val id: String,
    val name: String,
    val feastDay: String,
    val story: String,
    val patronOf: List<String> = emptyList()
)

data class SaintsUiState(
    val saints: List<Saint> = emptyList(),
    val selectedSaintId: String? = null,
    val isEditMode: Boolean = false,
    val editName: String = "",
    val editFeastDay: String = "",
    val editPatrons: String = "",
    val editStory: String = "",
    val editId: String? = null,
    val isLoading: Boolean = true
)

class SaintsViewModel(private val appContext: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(SaintsUiState())
    val uiState: StateFlow<SaintsUiState> = _uiState.asStateFlow()

    init {
        makeIOCall {
            val stored = runCatching { SaintsPrefs.getSaintsJson(appContext) }.getOrNull()
            val parsed = parseSaintsJson(json = stored.orEmpty())
            val saints = if (parsed.isNotEmpty()) parsed else defaultSaints()
            if (stored.isNullOrBlank() || parsed.isEmpty()) {
                SaintsPrefs.setSaintsJson(appContext, saintsToJson(items = saints))
            }
            _uiState.update { it.copy(saints = saints, isLoading = false) }
        }
    }

    fun startNew() {
        _uiState.update {
            it.copy(
                isEditMode = true,
                editId = null,
                editName = "",
                editFeastDay = "",
                editPatrons = "",
                editStory = ""
            )
        }
    }

    fun startEdit(id: String) {
        val saint = _uiState.value.saints.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                isEditMode = true,
                editId = saint.id,
                editName = saint.name,
                editFeastDay = saint.feastDay,
                editPatrons = saint.patronOf.joinToString(", "),
                editStory = saint.story
            )
        }
    }

    fun select(id: String) {
        _uiState.update { it.copy(selectedSaintId = id) }
    }

    fun backToList() {
        _uiState.update { it.copy(selectedSaintId = null) }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(isEditMode = false, editId = null) }
    }

    fun updateName(value: String) = _uiState.update { it.copy(editName = value) }
    fun updateFeastDay(value: String) = _uiState.update { it.copy(editFeastDay = value) }
    fun updatePatrons(value: String) = _uiState.update { it.copy(editPatrons = value) }
    fun updateStory(value: String) = _uiState.update { it.copy(editStory = value) }

    fun saveEdit() {
        val state = _uiState.value
        if (state.editName.isBlank() || state.editStory.isBlank()) return

        val id = state.editId ?: UUID.randomUUID().toString()
        val patronList = state.editPatrons.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val updatedSaint = Saint(
            id = id,
            name = state.editName.trim(),
            feastDay = state.editFeastDay.trim(),
            story = state.editStory.trim(),
            patronOf = patronList
        )

        makeIOCall {
            val newList = _uiState.value.saints.toMutableList().apply {
                val index = indexOfFirst { it.id == id }
                if (index >= 0) set(index, updatedSaint) else add(updatedSaint)
            }
            SaintsPrefs.setSaintsJson(appContext, saintsToJson(items = newList))
            _uiState.update {
                it.copy(
                    saints = newList,
                    isEditMode = false,
                    editId = null,
                    selectedSaintId = updatedSaint.id
                )
            }
        }
    }

    private fun defaultSaints(): List<Saint> =
        listOf(
            Saint(
                id = "sveti-josip",
                name = "Sveti Josip",
                feastDay = "19. ožujka",
                story = """
                    Sveti Josip, zaručnik Blažene Djevice Marije i skrbnik Isusa Krista, bio je pošten i pravedan čovjek. 
                    
                    Kao tesar po zanimanju, radio je marljivo kako bi prehranio svoju obitelj. Kada je saznao da je Marija trudna, razmišljao je da je potajno napusti, ali mu se anđeo pojavio u snu i rekao mu istinu.
                    
                    Josip je pokazao veliku vjeru prihvaćajući Božji plan. Štitio je Mariju i Isusa, odvevši ih u Egipat kada je Herod htio ubiti Isusa. Bio je uzoran otac i muž.
                    
                    Sveti Josip umro je u Isusovim i Marijnim rukama. Poznat je kao zaštitnik obitelji, radnika, i onih koji su na samrti.
                """.trimIndent(),
                patronOf = listOf("Obitelji", "Radnika", "Tesara", "Umirućih")
            ),
            Saint(
                id = "sveta-terezija-avilska",
                name = "Sveta Terezija Avilska",
                feastDay = "15. listopada",
                story = """
                    Sveta Terezija od Isusa, poznata kao Terezija Avilska, bila je španjolska karmelićanska redovnica i jedan od najvećih mističara Crkve.
                    
                    Rođena 1515. godine u Španjolskoj, u mladosti je vodila svjetovni život, ali nakon teške bolesti doživjela je duhovnu preobrazbu.
                    
                    Reformirala je karmelićanski red, osnivajući brojne samostane gdje se prakticirala stroga kontemplativna molitva i jednostavnost života.
                    
                    Njezini spisi, posebno "Unutarnji Dvorac" i "Put savršenstva", postali su klasici duhovne literature. Učila je o stupnjevima molitve i intimnom odnosu s Bogom.
                    
                    Papa Pavao VI. proglasio ju je Učiteljicom Crkve 1970. godine.
                """.trimIndent(),
                patronOf = listOf("Španjolskih pisaca", "Glavobolje", "Onih u potrebi za molitvom")
            ),
            Saint(
                id = "sveti-franjo-asiski",
                name = "Sveti Franjo Asiški",
                feastDay = "4. listopada",
                story = """
                    Sveti Franjo, rođen kao Giovanni di Pietro di Bernardone 1181. godine u Assisiju, bio je sin bogatog trgovca tkaninama.
                    
                    U mladosti vodio je razuzdani život, ali nakon bolesti i zarobljavanja u ratu, doživio je duhovnu preobrazbu. Čuvši Božji glas u crkvi San Damiano, napustio je sve i posvetio se siromaštvu.
                    
                    Osnovao je franjevački red, živeći u apsolutnom siromaštvu i propovijedajući Evanđelje. Bio je poznat po ljubavi prema svim Božjim stvorenjima - ljudima, životinjama i prirodi.
                    
                    Godine 1224. primio je stigmate - rane Kristove. Napisao je poznatu "Pjesmu o svim stvorenjima" gdje sve stvoreno naziva bratom i sestrom.
                    
                    Umro je 3. listopada 1226. godine, a kanoniziran je već 1228. godine.
                """.trimIndent(),
                patronOf = listOf("Životinja", "Prirode", "Trgovaca", "Ekologije")
            ),
            Saint(
                id = "sveta-majka-terezija",
                name = "Sveta Majka Terezija",
                feastDay = "5. rujna",
                story = """
                    Sveta Terezija Kalkutska, rođena kao Anjezë Gonxhe Bojaxhiu 1910. u Skoplju, bila je katolička redovnica albanskog podrijetla.
                    
                    S 18 godina otišla je u Irsku i pridružila se sestrama Loreto, a zatim je poslana u Indiju gdje je predavala u školi.
                    
                    Godine 1946. doživjela je "poziv unutar poziva" - Isus ju je pozvao da napusti samostan i služi najsiromašnijima. Osnovala je Misionarke ljubavi.
                    
                    Kroz cijeli život služila je najsiromašnijima, umirućima, napuštenima i gubavima u Kalkuti i diljem svijeta. 
                    
                    Dobila je Nobelovu nagradu za mir 1979. godine. Umrla je 1997., a proglašena je svetom 2016. godine.
                    
                    Njezina poruka bila je: \"Ne možemo sve učiniti velike stvari, ali možemo činiti male stvari s velikom ljubavlju.\"
                """.trimIndent(),
                patronOf = listOf("Siromašnih", "Misionara", "Volontera")
            ),
            Saint(
                id = "sveti-ivan-krstitelj",
                name = "Sveti Ivan Krstitelj",
                feastDay = "24. lipnja",
                story = """
                    Sveti Ivan Krstitelj, sin Zaharije i Elizabete, rođen je šest mjeseci prije Isusa Krista. Njegov rođenje bilo je čudesno jer je njegova majka bila neplodna i u poznim godinama.
                    
                    Živio je asketskim životom u pustinji, noseći odjeću od devine dlake i hraneći se skakavcima i divljim medom.
                    
                    Propovjedao je pokajanje i kršćavao ljude u rijeci Jordan kao pripremu za dolazak Mesije. Krstio je i samog Isusa, prepoznavši ga kao Božjeg Jaganjca.
                    
                    Hrabro je propovijedao istinu, čak i kralju Herodu, optužujući ga za preljub. Zbog toga je bio uhićen i na kraju pogubljen na zahtjev Herodijade i njezine kćeri Salome.
                    
                    Bio je posljednji i najveći starozavjetni prorok koji je pripremio put Gospodinu.
                """.trimIndent(),
                patronOf = listOf("Krstitelja", "Propovjednika", "Obraćenika")
            ),
            Saint(
                id = "sveta-marija-goretti",
                name = "Sveta Marija Goretti",
                feastDay = "6. srpnja",
                story = """
                    Sveta Marija Goretti rođena je 1890. godine u siromašnoj talijanskoj obitelji. Nakon smrti oca, majka i djeca radili su kao najamni radnici.
                    
                    S 11 godina, 5. srpnja 1902., mladi čovjek Alessandro Serenelli pokušao ju je silom zavesti i seksualno zloupotrijebiti. Marija se branila govoreći: \"Ne! To je grijeh! Bog to ne želi!\"
                    
                    Alessandro ju je iz bijesa izboo nožem 14 puta. Na samrti, nakon 20 sati patnje, Marija je oprostila svom ubojici i rekla da ga želi vidjeti u raju.
                    
                    Alessandro je osuđen na 30 godina zatvora. U zatvoru se pokajao nakon što mu se Marija ukazala u snu. Nakon puštanja na slobodu, postao je laik franjevac.
                    
                    Papa Pio XII. kanonizirao ju je 1950. godine pred 500,000 vjernika, uključujući njezinu majku i Alessandra koji je bio prisutan.
                """.trimIndent(),
                patronOf = listOf("Čistoće", "Žrtava silovanja", "Mladih")
            )
        )

    private fun saintsToJson(items: List<Saint>): String {
        val arr = JSONArray()
        items.forEach { saint ->
            val obj = JSONObject()
            obj.put("id", saint.id)
            obj.put("name", saint.name)
            obj.put("feastDay", saint.feastDay)
            obj.put("story", saint.story)
            obj.put("patronOf", JSONArray(saint.patronOf))
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseSaintsJson(json: String): List<Saint> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val patronArr = obj.optJSONArray("patronOf") ?: JSONArray()
                    val patrons = buildList {
                        for (p in 0 until patronArr.length()) {
                            val value = patronArr.optString(p).trim()
                            if (value.isNotBlank()) add(value)
                        }
                    }
                    add(
                        Saint(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = obj.optString("name"),
                            feastDay = obj.optString("feastDay"),
                            story = obj.optString("story"),
                            patronOf = patrons
                        )
                    )
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            emptyList()
        }
    }
}