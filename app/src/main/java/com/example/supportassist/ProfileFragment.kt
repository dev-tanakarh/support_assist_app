package com.example.supportassist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.supportassist.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject
    lateinit var tokenManager: TokenManager

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Observe logout success to navigate away
        viewModel.logoutSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                performLocalLogout()
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val userState by viewModel.user.observeAsState()
                    val totalState by viewModel.totalTickets.observeAsState(0)
                    val resolvedState by viewModel.resolvedTickets.observeAsState(0)
                    var themeMode by remember { mutableStateOf(ThemeManager.getSavedMode(requireContext())) }

                    ProfileScreen(
                        user = userState,
                        totalTickets = totalState,
                        resolvedTickets = resolvedState,
                        currentThemeMode = themeMode,
                        onThemeChange = { mode ->
                            themeMode = mode
                            ThemeManager.setMode(requireContext(), mode)
                            activity?.recreate()
                        },
                        onLogout = { viewModel.logout() }, // Stage 2: Attempt server revocation
                        onEditProfile = {
                            val intent = Intent(requireContext(), EditProfileActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshProfile()
    }

    private fun performLocalLogout() {
        tokenManager.clear()
        DataRepository.getInstance(requireContext()).clearCache(requireContext())
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}
