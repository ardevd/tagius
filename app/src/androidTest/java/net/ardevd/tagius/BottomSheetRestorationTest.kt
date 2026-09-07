package net.ardevd.tagius

import androidx.fragment.app.FragmentFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.ardevd.tagius.features.records.ui.add.AddRecordBottomSheet
import net.ardevd.tagius.features.records.ui.edit.EditRecordBottomSheet
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomSheetRestorationTest {
    private val fragmentFactory = FragmentFactory()

    @Test
    fun addRecordSheetCanBeRecreatedByFragmentManager() {
        val fragment =
            fragmentFactory.instantiate(
                AddRecordBottomSheet::class.java.classLoader!!,
                AddRecordBottomSheet::class.java.name,
            )

        assertTrue(fragment is AddRecordBottomSheet)
    }

    @Test
    fun editRecordSheetCanBeRecreatedByFragmentManager() {
        val fragment =
            fragmentFactory.instantiate(
                EditRecordBottomSheet::class.java.classLoader!!,
                EditRecordBottomSheet::class.java.name,
            )

        assertTrue(fragment is EditRecordBottomSheet)
    }
}
