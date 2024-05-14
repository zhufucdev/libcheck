@file:OptIn(ExperimentalResourceApi::class)

package ui

import androidx.compose.runtime.Composable
import com.sqlmaster.proto.LibraryOuterClass.ReaderTier
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringArrayResource
import resources.Res
import resources.reader_tiers_para

@Composable
fun ReaderTier.stringRes() = stringArrayResource(Res.array.reader_tiers_para)[number]
