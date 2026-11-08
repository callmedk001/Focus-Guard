package com.dk.focusguard.ui.protection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberCardVariant
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed
import com.dk.focusguard.ui.theme.TextGray
import com.dk.focusguard.ui.theme.TextMuted
import com.dk.focusguard.ui.theme.TextWhite

/**
 * Dialog enabling users to manage custom blocked domains:
 * add single domains with syntax validation, delete existing entries, and bulk import.
 */
@Composable
fun CustomDomainsDialog(
    customDomains: Set<String>,
    onAddDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit,
    onImportDomains: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newDomainText by remember { mutableStateOf("") }
    var domainError by remember { mutableStateOf<String?>(null) }
    var showImportBox by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCard,
        titleContentColor = TextWhite,
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Custom Domain Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add custom domains or subdomains to immediately sinkhole their DNS queries alongside the starter blocklist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )

                // Single Domain Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newDomainText,
                        onValueChange = {
                            newDomainText = it
                            domainError = null
                        },
                        placeholder = { Text("e.g. gambling-site.com", color = TextMuted) },
                        isError = domainError != null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            errorBorderColor = CyberRed
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val trimmed = newDomainText.trim().lowercase().removeSuffix(".")
                            val domainRegex = Regex("^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$")
                            if (trimmed.length in 3..253 && domainRegex.matches(trimmed)) {
                                onAddDomain(trimmed)
                                newDomainText = ""
                                domainError = null
                            } else {
                                domainError = "Invalid domain syntax"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = CyberBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                }

                domainError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberRed
                    )
                }

                // Bulk Import Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${customDomains.size} Custom Rules",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )

                    TextButton(onClick = { showImportBox = !showImportBox }) {
                        Text(
                            text = if (showImportBox) "Hide Bulk Import" else "Bulk Import",
                            color = CyberGreen,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (showImportBox) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            placeholder = { Text("Paste domains separated by comma or newlines", color = TextMuted) },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val splitDomains = importText.split(',', '\n', ' ')
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                onImportDomains(splitDomains)
                                importText = ""
                                showImportBox = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGreen,
                                contentColor = CyberBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("IMPORT VALID DOMAINS", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Custom Domains List
                if (customDomains.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom domains added yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(customDomains.toList()) { domain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCardVariant)
                                    .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = domain,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextWhite,
                                    fontFamily = FontFamily.Monospace
                                )

                                IconButton(
                                    onClick = { onRemoveDomain(domain) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = CyberRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = CyberBackground
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("DONE", fontWeight = FontWeight.Bold)
            }
        }
    )
}

