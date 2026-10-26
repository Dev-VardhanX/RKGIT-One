package com.rkgit.one

import com.rkgit.one.data.profile.DepartmentsCacheStore
import com.rkgit.one.data.profile.DepartmentCheck
import com.rkgit.one.data.profile.ProfileRepository
import com.rkgit.one.data.profile.ProfileStore
import com.rkgit.one.data.profile.StudentProfile
import com.rkgit.one.data.profile.activeSorted
import com.rkgit.one.data.profile.canContinue
import com.rkgit.one.data.profile.checkDepartment
import com.rkgit.one.data.profile.decodeDepartments
import com.rkgit.one.data.profile.encodeDepartments
import com.rkgit.one.data.profile.formatNoticesCaption
import com.rkgit.one.data.profile.isValidYear
import com.rkgit.one.data.profile.sanitizeStoredDepartment
import com.rkgit.one.data.profile.sanitizeStoredYear
import com.rkgit.one.data.profile.yearLabel
import com.rkgit.one.data.remote.dto.DepartmentDto
import com.rkgit.one.data.remote.dto.toDomain
import com.rkgit.one.domain.model.Department
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FakeProfileStore(initial: StudentProfile = StudentProfile()) : ProfileStore {
    var current: StudentProfile = initial
    override fun read(): StudentProfile = current
    override fun write(profile: StudentProfile) {
        current = profile
    }
}

class FakeDepartmentsCacheStore(initial: List<Department> = emptyList()) : DepartmentsCacheStore {
    var current: List<Department> = initial
    override fun read(): List<Department> = current
    override fun write(list: List<Department>) {
        current = list
    }
}

class ProfileLogicTest {

    private var originalLocale: Locale? = null

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        originalLocale?.let { Locale.setDefault(it) }
    }

    @Test
    fun activeSorted_dropsInactive_sortsBySortOrderThenNameIgnoreCase_stable_empty() {
        assertTrue(emptyList<Department>().activeSorted().isEmpty())

        val d1 = Department(id = "1", code = "it", name = "Information Technology", sortOrder = 2, active = true)
        val d2 = Department(id = "2", code = "cse", name = "computer science", sortOrder = 1, active = true)
        val d3 = Department(id = "3", code = "ece-a", name = "ECE A", sortOrder = 1, active = true)
        val d4 = Department(id = "4", code = "ece-b", name = "ece a", sortOrder = 1, active = true) // equal key
        val dInactive = Department(id = "5", code = "me", name = "Mechanical", sortOrder = 0, active = false)

        val result = listOf(d1, d2, dInactive, d3, d4).activeSorted()

        assertEquals(4, result.size)
        assertFalse(result.any { it.code == "me" })

        // sortOrder 1 first: "computer science", then "ECE A", then "ece a" (stable)
        assertEquals("cse", result[0].code)
        assertEquals("ece-a", result[1].code)
        assertEquals("ece-b", result[2].code) // preserved order for equal keys
        // sortOrder 2 next
        assertEquals("it", result[3].code)
    }

    @Test
    fun checkDepartment_validatesProperly() {
        val departments = listOf(
            Department("1", "cse", "CSE", 1, active = true),
            Department("2", "me", "ME", 2, active = false)
        )

        assertEquals(DepartmentCheck.NONE, checkDepartment(null, departments))
        assertEquals(DepartmentCheck.NONE, checkDepartment("", departments))
        assertEquals(DepartmentCheck.NONE, checkDepartment("   ", departments))

        assertEquals(DepartmentCheck.OK, checkDepartment("cse", departments))
        assertEquals(DepartmentCheck.UNAVAILABLE, checkDepartment("me", departments))
        assertEquals(DepartmentCheck.UNAVAILABLE, checkDepartment("unknown", departments))
    }

    @Test
    fun canContinue_respectsAllRules() {
        // year null -> false whatever else
        assertFalse(canContinue(year = null, departmentCode = null, departmentsFailed = false))
        assertFalse(canContinue(year = null, departmentCode = "cse", departmentsFailed = false))
        assertFalse(canContinue(year = null, departmentCode = null, departmentsFailed = true))
        assertFalse(canContinue(year = null, departmentCode = "cse", departmentsFailed = true))

        // invalid year 0 or 5 -> false
        assertFalse(canContinue(year = 0, departmentCode = "cse", departmentsFailed = false))
        assertFalse(canContinue(year = 5, departmentCode = "cse", departmentsFailed = false))
        assertFalse(canContinue(year = 0, departmentCode = null, departmentsFailed = true))
        assertFalse(canContinue(year = 5, departmentCode = null, departmentsFailed = true))

        // year set and department set -> true
        assertTrue(canContinue(year = 2, departmentCode = "it", departmentsFailed = false))
        assertTrue(canContinue(year = 2, departmentCode = "it", departmentsFailed = true))

        // year set, no department, departmentsFailed false -> false
        assertFalse(canContinue(year = 2, departmentCode = null, departmentsFailed = false))

        // year set, no department, departmentsFailed true -> true
        assertTrue(canContinue(year = 2, departmentCode = null, departmentsFailed = true))
    }

    @Test
    fun profileRepository_managesProfileStateAndStore() {
        val fakeStore = FakeProfileStore(StudentProfile(setupDone = false, year = null, departmentCode = null))
        val repo = ProfileRepository(fakeStore)

        // initial state read from store
        assertEquals(StudentProfile(setupDone = false, year = null, departmentCode = null), repo.profile.value)
        assertFalse(repo.isSetupDone())

        // save(2, "it")
        repo.save(2, "it")
        assertEquals(StudentProfile(setupDone = true, year = 2, departmentCode = "it"), repo.profile.value)
        assertEquals(StudentProfile(setupDone = true, year = 2, departmentCode = "it"), fakeStore.current)
        assertTrue(repo.isSetupDone())

        // save(7, "") -> year null, code null but setupDone true
        repo.save(7, "")
        assertEquals(StudentProfile(setupDone = true, year = null, departmentCode = null), repo.profile.value)
        assertEquals(StudentProfile(setupDone = true, year = null, departmentCode = null), fakeStore.current)
        assertTrue(repo.isSetupDone())

        // skip() -> setupDone true, nulls
        fakeStore.current = StudentProfile(setupDone = false, year = 2, departmentCode = "it")
        repo.skip()
        assertEquals(StudentProfile(setupDone = true, year = null, departmentCode = null), repo.profile.value)
        assertEquals(StudentProfile(setupDone = true, year = null, departmentCode = null), fakeStore.current)

        // setYear keeps department, setDepartment keeps year
        repo.save(1, "it")
        repo.setYear(3)
        assertEquals(StudentProfile(setupDone = true, year = 3, departmentCode = "it"), repo.profile.value)
        assertEquals(StudentProfile(setupDone = true, year = 3, departmentCode = "it"), fakeStore.current)

        repo.setDepartment("cse")
        assertEquals(StudentProfile(setupDone = true, year = 3, departmentCode = "cse"), repo.profile.value)
        assertEquals(StudentProfile(setupDone = true, year = 3, departmentCode = "cse"), fakeStore.current)

        // isSetupDone reflects store
        fakeStore.current = StudentProfile(setupDone = false)
        assertFalse(repo.isSetupDone())
        fakeStore.current = StudentProfile(setupDone = true)
        assertTrue(repo.isSetupDone())

        // Pure sanitising function: year 9 reads as null
        assertNull(sanitizeStoredYear(9))
        assertNull(sanitizeStoredYear(0))
        assertEquals(1, sanitizeStoredYear(1))
        assertEquals(4, sanitizeStoredYear(4))
        assertNull(sanitizeStoredYear(5))

        assertNull(sanitizeStoredDepartment(null))
        assertNull(sanitizeStoredDepartment(""))
        assertNull(sanitizeStoredDepartment("   "))
        assertEquals("it", sanitizeStoredDepartment("it"))
    }

    @Test
    fun encodeDecodeDepartments_roundTripAndCorruptInputs() {
        val list = listOf(
            Department("1", "cs", "Computer Science", 1, true),
            Department("2", "it", "Information Technology", 2, true)
        )
        val encoded = encodeDepartments(list)
        val decoded = decodeDepartments(encoded)
        assertEquals(list, decoded)

        // null, blank, not json, and wrong shape all return empty list without throwing
        assertEquals(emptyList<Department>(), decodeDepartments(null))
        assertEquals(emptyList<Department>(), decodeDepartments(""))
        assertEquals(emptyList<Department>(), decodeDepartments("   "))
        assertEquals(emptyList<Department>(), decodeDepartments("not json"))
        assertEquals(emptyList<Department>(), decodeDepartments("{}"))
        assertEquals(emptyList<Department>(), decodeDepartments("123"))
        assertEquals(emptyList<Department>(), decodeDepartments("[1, 2, 3]"))
        assertEquals(emptyList<Department>(), decodeDepartments("""[{"id": 123}]"""))
    }

    @Test
    fun departmentDto_decodesDefaultsAndExtraFieldsAndMapsToDomain() {
        val json = Json { ignoreUnknownKeys = true }

        // JSON without sort_order and active -> defaults 0 and true
        val basicJson = """{"id":"d1","code":"cs","name":"Computer Science"}"""
        val basicDto = json.decodeFromString<DepartmentDto>(basicJson)
        assertEquals(0, basicDto.sortOrder)
        assertTrue(basicDto.active)

        // unknown extra fields ignored
        val extraJson = """{"id":"d2","code":"it","name":"IT","extra_prop":"foo","sort_order":5,"active":false}"""
        val extraDto = json.decodeFromString<DepartmentDto>(extraJson)
        assertEquals(5, extraDto.sortOrder)
        assertFalse(extraDto.active)

        // toDomain maps all fields
        val domain = extraDto.toDomain()
        assertEquals("d2", domain.id)
        assertEquals("it", domain.code)
        assertEquals("IT", domain.name)
        assertEquals(5, domain.sortOrder)
        assertFalse(domain.active)
    }

    @Test
    fun yearLabelAndIsValidYear() {
        assertFalse(isValidYear(0))
        assertTrue(isValidYear(1))
        assertTrue(isValidYear(4))
        assertFalse(isValidYear(5))
        assertFalse(isValidYear(null))

        assertEquals("Year 0", yearLabel(0))
        assertEquals("Year 1", yearLabel(1))
        assertEquals("Year 4", yearLabel(4))
        assertEquals("Year 5", yearLabel(5))
    }

    @Test
    fun formatNoticesCaption_formatsCorrectly() {
        val depts = listOf(Department("1", "it", "Information Technology", 1, true))

        assertEquals(
            "Showing notices for all years and branches.",
            formatNoticesCaption(null, null, depts)
        )
        assertEquals(
            "Showing notices for Year 2, Information Technology",
            formatNoticesCaption(2, "it", depts)
        )
        assertEquals(
            "Showing notices for Year 2, all branches",
            formatNoticesCaption(2, null, depts)
        )
        assertEquals(
            "Showing notices for all years, Information Technology",
            formatNoticesCaption(null, "it", depts)
        )
        assertEquals(
            "Showing notices for Year 2, CSE",
            formatNoticesCaption(2, "cse", depts)
        )
    }
}
