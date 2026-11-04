package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.amount
import budgethunter.composeapp.generated.resources.attach_invoice
import budgethunter.composeapp.generated.resources.attach_receipt_subtitle
import budgethunter.composeapp.generated.resources.created_by
import budgethunter.composeapp.generated.resources.edited_by
import budgethunter.composeapp.generated.resources.file_missing
import budgethunter.composeapp.generated.resources.more_categories
import budgethunter.composeapp.generated.resources.other_date
import budgethunter.composeapp.generated.resources.scan_receipt
import budgethunter.composeapp.generated.resources.scan_receipt_subtitle
import budgethunter.composeapp.generated.resources.today
import budgethunter.composeapp.generated.resources.view_receipt_subtitle
import budgethunter.composeapp.generated.resources.yesterday
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.SimpleDatePickerDialog
import com.meneses.budgethunter.commons.ui.ThousandSeparatorTransformation
import com.meneses.budgethunter.commons.ui.shortDateLabel
import com.meneses.budgethunter.commons.util.currentDate
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.green_success
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.stringResource

/** Expense or income, as two halves of one pill; the selected half takes the type's color. */
@Composable
fun EntryTypeSelector(
    type: BudgetEntry.Type,
    onTypeSelected: (BudgetEntry.Type) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .border(1.dp, AppColors.outline, CircleShape)
    ) {
        BudgetEntry.getItemTypes().forEach { option ->
            val isSelected = option == type
            val (container, content) = when {
                !isSelected -> AppColors.surface to AppColors.onSurface
                option == BudgetEntry.Type.OUTCOME -> AppColors.errorContainer to AppColors.onErrorContainer
                else -> green_success.copy(alpha = 0.2f) to green_success
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(container)
                    .clickable { onTypeSelected(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.toStringResource(),
                    color = content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * The amount as the screen's headline: large, centered, with the currency sign beside it.
 * Takes the same input rules as [AmountField].
 */
@Composable
fun AmountHero(
    amount: String,
    onAmountChanged: (String) -> Unit,
    amountError: String?,
    focusRequester: FocusRequester
) {
    val color = if (amountError != null) AppColors.error else AppColors.onSurface
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.amount),
            fontSize = 13.sp,
            color = AppColors.onSurfaceVariant
        )
        BasicTextField(
            value = amount,
            onValueChange = { onAmountChanged(sanitizeAmountInput(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium,
                color = color
            ),
            cursorBrush = SolidColor(AppColors.primary),
            visualTransformation = ThousandSeparatorTransformation(),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Decimal
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$",
                        fontSize = 26.sp,
                        color = AppColors.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                        if (amount.isEmpty()) {
                            Text(
                                text = "0",
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
        if (amountError != null) {
            Text(
                text = amountError,
                color = AppColors.error,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Where a receipt goes. Without one, it invites to scan it, and says the AI will fill in the
 * form when the AI is on; with one, it names the file and opens it.
 */
@Composable
fun ReceiptCard(
    invoice: String?,
    isFileValid: Boolean,
    isAiEnabled: Boolean,
    onClick: () -> Unit
) {
    val isMissing = invoice != null && !isFileValid
    val isAiOffer = invoice == null && isAiEnabled
    val accent = when {
        isMissing -> AppColors.error
        isAiOffer -> AppColors.tertiary
        else -> AppColors.onSurfaceVariant
    }
    val fileName = invoice?.split("/")?.last()
    val title = when {
        invoice == null && isAiEnabled -> stringResource(Res.string.scan_receipt)
        invoice == null -> stringResource(Res.string.attach_invoice)
        isMissing -> stringResource(Res.string.file_missing, fileName.orEmpty())
        else -> fileName.orEmpty()
    }
    val subtitle = when {
        invoice == null && isAiEnabled -> stringResource(Res.string.scan_receipt_subtitle)
        invoice == null -> stringResource(Res.string.attach_receipt_subtitle)
        else -> stringResource(Res.string.view_receipt_subtitle)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isAiOffer) AppColors.tertiaryContainer.copy(alpha = 0.35f) else AppColors.surfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.6f), MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isAiOffer) Icons.Default.AutoAwesome else Icons.Default.AttachFile,
            contentDescription = null,
            tint = accent
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isAiOffer || isMissing) accent else AppColors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = AppColors.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppColors.onSurfaceVariant
        )
    }
}

/** The categories most entries fall into, always one tap away in the form. */
private val COMMON_CATEGORIES = listOf(
    BudgetEntry.Category.FOOD,
    BudgetEntry.Category.GROCERIES,
    BudgetEntry.Category.TRANSPORTATION,
    BudgetEntry.Category.SERVICES
)

/**
 * The common categories as chips, plus the selected one when it is not among them, and a "More"
 * chip that opens the rest in a menu. Two rows instead of five, so the form keeps its save
 * button in view.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    selected: BudgetEntry.Category,
    onCategorySelected: (BudgetEntry.Category) -> Unit
) {
    // The selection goes first when it is not a common one, so it is always visible
    val visible = remember(selected) {
        if (selected in COMMON_CATEGORIES) COMMON_CATEGORIES else listOf(selected) + COMMON_CATEGORIES
    }
    val others = remember(visible) { BudgetEntry.getCategories().filterNot { it in visible } }
    var isMenuOpen by remember { mutableStateOf(false) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        visible.forEach { category ->
            CategoryChip(
                category = category,
                isSelected = category == selected,
                onClick = { onCategorySelected(category) }
            )
        }

        Box {
            FilterChip(
                selected = false,
                onClick = { isMenuOpen = true },
                label = { Text(stringResource(Res.string.more_categories), fontSize = 13.sp) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                shape = MaterialTheme.shapes.small
            )
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
                shape = MaterialTheme.shapes.medium,
                containerColor = AppColors.surfaceContainer
            ) {
                others.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.toStringResource(), fontSize = 15.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = AppColors.onSurfaceVariant
                            )
                        },
                        onClick = {
                            isMenuOpen = false
                            onCategorySelected(category)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: BudgetEntry.Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(category.toStringResource(), fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.primaryContainer,
            selectedLabelColor = AppColors.onPrimaryContainer,
            selectedLeadingIconColor = AppColors.onPrimaryContainer
        )
    )
}

/** Today and yesterday cover most entries; any other day goes through the calendar. */
@Composable
fun DateShortcuts(
    date: String,
    onDateSelected: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = currentDate()
    val todayText = today.toString()
    val yesterdayText = today.minus(DatePeriod(days = 1)).toString()
    val isOtherDate = date != todayText && date != yesterdayText

    SimpleDatePickerDialog(
        showDialog = showDatePicker,
        onDateSelected = onDateSelected,
        onDismiss = { showDatePicker = false },
        initialDate = date
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateChip(
            label = stringResource(Res.string.today),
            selected = date == todayText,
            onClick = { onDateSelected(todayText) }
        )
        DateChip(
            label = stringResource(Res.string.yesterday),
            selected = date == yesterdayText,
            onClick = { onDateSelected(yesterdayText) }
        )
        DateChip(
            label = if (isOtherDate) shortDateLabel(date) else stringResource(Res.string.other_date),
            selected = isOtherDate,
            onClick = { showDatePicker = true },
            showCalendarIcon = true
        )
    }
}

@Composable
private fun DateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showCalendarIcon: Boolean = false
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = if (showCalendarIcon) {
            {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        shape = MaterialTheme.shapes.small
    )
}

/** Who wrote the entry, for entries that came from the server. Nothing for a new one. */
@Composable
fun EntryAuthorship(entry: BudgetEntry) {
    val created = entry.createdByEmail?.takeIf { it.isNotBlank() } ?: return
    val updated = entry.updatedByEmail?.takeIf { it.isNotBlank() && it != created }
    val text = buildString {
        append(stringResource(Res.string.created_by, created))
        if (updated != null) {
            append(" · ")
            append(stringResource(Res.string.edited_by, updated))
        }
    }
    Text(
        text = text,
        fontSize = 12.sp,
        color = AppColors.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Label above a group of controls in the form. */
@Composable
fun FormSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = AppColors.onSurfaceVariant
    )
}
