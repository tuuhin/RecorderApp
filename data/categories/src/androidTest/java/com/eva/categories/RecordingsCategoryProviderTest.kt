package com.eva.categories

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.eva.categories.domain.exceptions.RecordingCategoryNotFoundException
import com.eva.categories.domain.exceptions.UnModifiableRecordingCategoryException
import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.categories.domain.provider.RecordingCategoryProvider
import com.eva.database.dao.RecordingCategoryDao
import com.eva.utils.Resource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RecordingsCategoryProviderTest {

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	@Inject
	lateinit var provider: RecordingCategoryProvider

	@Inject
	lateinit var dao: RecordingCategoryDao

	@BeforeTest
	fun setUp() = hiltRule.inject()

	@AfterTest
	fun tearDown() = runBlocking {
		dao.deleteAllCategories()
	}

	@Test
	fun create_new_category_simple() = runTest {
		val entry = provider.createCategory("Category")
		assertTrue(entry is Resource.Success, "New entry is successfully created")

		val check = provider.getCategoryFromId(entry.data.id)
		assertTrue(check is Resource.Success, "The entry is actually present")
	}

	@Test
	fun create_new_category() = runTest {
		val entry = provider.createCategory(
			name = "Category",
			color = CategoryColor.COLOR_INDIGO,
			type = CategoryType.CATEGORY_GROUP
		)
		assertTrue(entry is Resource.Success, "New entry is successfully created")
		assertEquals(
			CategoryColor.COLOR_INDIGO,
			entry.data.categoryColor,
			"Same color created as mentioned"
		)
		assertEquals(
			CategoryType.CATEGORY_GROUP,
			entry.data.categoryType,
			"Same category type created"
		)

		val check = provider.getCategoryFromId(entry.data.id)
		assertTrue(check is Resource.Success, "The entry is actually present")
	}

	@Test
	fun create_and_then_update_category() = runTest {
		val originalName = "Category"
		val create = provider.createCategory(originalName)
		assertTrue(create is Resource.Success, "New entry is successfully created")
		assertEquals(originalName, create.data.name)

		val updatedName = "Updated Category"
		val update = provider.updateCategory(create.data.copy(name = updatedName))
		assertTrue(update is Resource.Success, "New entry is successfully created")
		assertEquals(updatedName, update.data.name)
	}

	@Test
	fun try_updating_a_fake_category() = runTest {
		val fakeCategory = RecordingCategoryModel(id = 20L, name = "Fakes")
		val update = provider.updateCategory(fakeCategory)
		assertFalse(update is Resource.Success, "Cannot update the category as it not exists")
	}

	@Test
	fun create_category_then_delete_it() = runTest {
		val originalName = "Category"
		val create = provider.createCategory(originalName)
		assertTrue(create is Resource.Success, "New entry is successfully created")
		assertEquals(originalName, create.data.name)

		val delete = provider.deleteCategory(create.data)
		assertTrue(delete is Resource.Success, "Entry is deleted")

		val entry = provider.getCategoryFromId(create.data.id)
		assertTrue(entry is Resource.Error, "Entry cannot be found")
		assertIs<RecordingCategoryNotFoundException>(entry.error)
	}

	@Test
	fun try_deleting_the_all_category_model() = runTest {
		val delete = provider.deleteCategory(RecordingCategoryModel.ALL_CATEGORY)
		assertTrue(delete is Resource.Error, "Entry cannot be deleted")
		assertIs<UnModifiableRecordingCategoryException>(delete.error)
	}

	@Test
	fun check_adding_update_effect_flow() = runTest {
		provider.recordingCategoryAsResourceFlow.test {
			val firstEmit = awaitItem()
			assertIs<Resource.Loading>(firstEmit)

			val secondEmit = awaitItem()
			assertIs<Resource.Success<List<RecordingCategoryModel>, Exception>>(secondEmit)
			// it will always have the default the recording added
			assertEquals(1, secondEmit.data.size)

			// now add an item
			val category = provider.createCategory("Category")
			assertIs<Resource.Success<RecordingCategoryModel, Exception>>(category)

			val thirdEmit = awaitItem()
			assertIs<Resource.Success<List<RecordingCategoryModel>, Exception>>(thirdEmit)
			assertEquals(2, thirdEmit.data.size)

			// delete the item
			provider.deleteCategory(category.data)

			val fourthEmit = awaitItem()
			assertIs<Resource.Success<List<RecordingCategoryModel>, Exception>>(fourthEmit)
			assertEquals(1, fourthEmit.data.size)

			cancelAndIgnoreRemainingEvents()
		}
	}
}