package ui

import androidx.compose.runtime.Composable
import model.AccountCapability
import org.jetbrains.compose.resources.stringArrayResource
import resources.Res
import resources.change_password_result_para

@Composable
fun AccountCapability.ChangePasswordResult.string() = stringArrayResource(Res.array.change_password_result_para)[ordinal]