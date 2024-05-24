package ui

import androidx.compose.runtime.Composable
import com.sqlmaster.proto.LibraryOuterClass.UserRole
import org.jetbrains.compose.resources.stringArrayResource
import resources.Res
import resources.user_role_para

@Composable
fun UserRole.string(): String = stringArrayResource(Res.array.user_role_para)[ordinal]