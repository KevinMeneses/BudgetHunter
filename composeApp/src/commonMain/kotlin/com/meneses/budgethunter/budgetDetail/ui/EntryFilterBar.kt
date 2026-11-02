package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.any_category
import budgethunter.composeapp.generated.resources.any_date
import budgethunter.composeapp.generated.resources.category
import budgethunter.composeapp.generated.resources.clean
import budgethunter.composeapp.generated.resources.clear_search
import budgethunter.composeapp.generated.resources.custom_range
import budgethunter.composeapp.generated.resources.date
import budgethunter.composeapp.generated.resources.filtered_entries_summary
import budgethunter.composeapp.generated.resources.incomes_filter
import budgethunter.composeapp.generated.resources.last_7_days
import budgethunter.composeapp.generated.resources.last_month
import budgethunter.composeapp.generated.resources.outcomes_filter
import budgethunter.composeapp.generated.resources.remove_filter
import budgethunter.composeapp.generated.resources.search_entries
import budgethunter.composeapp.generated.resources.this_month
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetEntry.domain.EntryDateRange
import com.meneses.budgethunter.budgetEntry.ui.toStringResource
import com.meneses.budgethunter.commons.ui.DateRangePickerDialog
import com.meneses.budgethunter.commons.util.currentDate
import com.meneses.budgethunter.theme.AppColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Search field and filter chips above the budget entries. Every change applies right away.
 *
 * @param shownCount entries that pass [filter]
 * @param totalCount entries of the budget
 */
@Composable
fun EntryFilterBar(
    filter: BudgetEntryFilter,
    shownCount: Int,
    totalCount: Int,
    onFilterChange: (BudgetEntryFilter) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SearchField(
            query = filter.description.orEmpty(),
            onQueryChange = { onFilterChange(filter.copy(description = it)) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TypeChip(BudgetEntry.Type.OUTCOME, filter, onFilterChange)
            TypeChip(BudgetEntry.Type.INCOME, filter, onFilterChange)
            CategoryChip(filter, onFilterChange)
            DateChip(filter, onFilterChange)
            if (filter.isActive) {
                AssistChip(
                    onClick = onClear,
                    label = { Text(stringResource(Res.string.clean)) },
                    leadingIcon = {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }

        if (filter.isActive) {
            Text(
                text = pluralStringResource(Res.plurals.filtered_entries_summary, totalCount, shownCount, totalCount),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    // Edited locally so typing never waits for the state to come back. The only change made
    // elsewhere is clearing it (the "Clear" chip); following any other change could bring back an
    // older text while typing fast
    var text by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query.isEmpty()) text = ""
    }
    val keyboard = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onQueryChange(it)
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(Res.string.search_entries)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = {
                    text = ""
                    onQueryChange("")
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.clear_search))
                }
            }
        },
        singleLine = true,
        shape = AbsoluteRoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
    )
}

/** Tapping the selected type again shows both types. */
@Composable
private fun TypeChip(
    type: BudgetEntry.Type,
    filter: BudgetEntryFilter,
    onFilterChange: (BudgetEntryFilter) -> Unit
) {
    val selected = filter.type == type
    FilterChip(
        selected = selected,
        onClick = { onFilterChange(filter.copy(type = if (selected) null else type)) },
        label = {
            Text(
                stringResource(
                    if (type == BudgetEntry.Type.OUTCOME) Res.string.outcomes_filter else Res.string.incomes_filter
                )
            )
        },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        }
    )
}

@Composable
private fun CategoryChip(
    filter: BudgetEntryFilter,
    onFilterChange: (BudgetEntryFilter) -> Unit
) {
    val categories = remember { BudgetEntry.getCategories() }
    MenuChip(
        label = filter.category?.toStringResource() ?: stringResource(Res.string.category),
        selected = filter.category != null,
        onRemove = { onFilterChange(filter.copy(category = null)) }
    ) { dismiss ->
        MenuOption(
            text = stringResource(Res.string.any_category),
            checked = filter.category == null,
            onClick = {
                onFilterChange(filter.copy(category = null))
                dismiss()
            }
        )
        categories.forEach { category ->
            MenuOption(
                text = category.toStringResource(),
                checked = filter.category == category,
                onClick = {
                    onFilterChange(filter.copy(category = category))
                    dismiss()
                }
            )
        }
    }
}

@Composable
private fun DateChip(
    filter: BudgetEntryFilter,
    onFilterChange: (BudgetEntryFilter) -> Unit
) {
    var showRangePicker by remember { mutableStateOf(false) }
    val dateRange = filter.dateRange

    MenuChip(
        label = dateRange?.label() ?: stringResource(Res.string.date),
        selected = dateRange != null,
        onRemove = { onFilterChange(filter.copy(dateRange = null)) }
    ) { dismiss ->
        val select = { range: EntryDateRange? ->
            onFilterChange(filter.copy(dateRange = range))
            dismiss()
        }
        MenuOption(stringResource(Res.string.any_date), checked = dateRange == null) { select(null) }
        MenuOption(stringResource(Res.string.this_month), checked = dateRange == EntryDateRange.ThisMonth) {
            select(EntryDateRange.ThisMonth)
        }
        MenuOption(stringResource(Res.string.last_month), checked = dateRange == EntryDateRange.LastMonth) {
            select(EntryDateRange.LastMonth)
        }
        MenuOption(stringResource(Res.string.last_7_days), checked = dateRange == EntryDateRange.Last7Days) {
            select(EntryDateRange.Last7Days)
        }
        MenuOption(stringResource(Res.string.custom_range), checked = dateRange is EntryDateRange.Custom) {
            dismiss()
            showRangePicker = true
        }
    }

    if (showRangePicker) {
        val custom = dateRange as? EntryDateRange.Custom
        DateRangePickerDialog(
            onRangeSelected = { start, end ->
                onFilterChange(filter.copy(dateRange = EntryDateRange.Custom(start, end)))
            },
            onDismiss = { showRangePicker = false },
            initialStart = custom?.start,
            initialEnd = custom?.end
        )
    }
}

@Composable
private fun EntryDateRange.label(): String = when (this) {
    EntryDateRange.ThisMonth -> stringResource(Res.string.this_month)
    EntryDateRange.LastMonth -> stringResource(Res.string.last_month)
    EntryDateRange.Last7Days -> stringResource(Res.string.last_7_days)
    is EntryDateRange.Custom -> {
        val currentYear = remember { currentDate().year }
        if (start == end) start.short(currentYear) else "${start.short(currentYear)} – ${end.short(currentYear)}"
    }
}

/** "5/10", or "5/10/25" outside the current year. */
private fun LocalDate.short(currentYear: Int): String =
    if (year == currentYear) "$dayOfMonth/$monthNumber" else "$dayOfMonth/$monthNumber/${year % 100}"

/**
 * A chip that opens a menu of choices. Once something is chosen it shows the choice, and an ✕
 * that removes it without opening the menu.
 */
@Composable
private fun MenuChip(
    label: String,
    selected: Boolean,
    onRemove: () -> Unit,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.remove_filter),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(role = Role.Button, onClick = onRemove)
                    )
                } else {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = AbsoluteRoundedCornerShape(10.dp),
            containerColor = AppColors.surface
        ) {
            menuContent { expanded = false }
        }
    }
}

@Composable
private fun MenuOption(
    text: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
        trailingIcon = if (checked) {
            { Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.primary) }
        } else {
            null
        },
        modifier = Modifier.padding(end = 4.dp)
    )
}
