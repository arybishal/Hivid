package com.hivid.app.ui.screens.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hivid.app.R
import com.hivid.app.ui.components.GlossaryRow
import com.hivid.app.ui.components.InfoCard
import com.hivid.app.ui.components.SectionLabel

@Composable
fun GuideScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SectionLabel(text = stringResource(R.string.guide_title))

        InfoCard(
            title = "FREQUENTLY ASKED QUESTIONS",
            description = "Understanding how on-device HDR mastering, tone curves, and social media platforms work."
        ) {
            GlossaryRow(
                term = stringResource(R.string.faq_q1),
                explanation = stringResource(R.string.faq_a1)
            )
            GlossaryRow(
                term = stringResource(R.string.faq_q2),
                explanation = stringResource(R.string.faq_a2)
            )
            GlossaryRow(
                term = stringResource(R.string.faq_q3),
                explanation = stringResource(R.string.faq_a3)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionLabel(text = "TECHNICAL ARCHITECTURE")

        InfoCard(
            title = "100% ON-DEVICE PRIVACY",
            description = "Hivid runs entirely offline using your device's hardware HEVC encoder and GPU shaders. No videos are ever uploaded to any cloud server, and no internet permissions exist in the application manifest."
        ) {
            GlossaryRow(
                term = "Zero Uploads & Network Safety",
                explanation = "Your private media remains in local device memory and app cache. Even with network connections active, the app contains no network sockets or telemetry beacons."
            )
            GlossaryRow(
                term = "Direct Hardware EGL Surfaces",
                explanation = "Decoding and encoding happen through native GPU buffers without copying raw 4K frames into Java heap memory, preventing memory crashes and maximizing encoding speed."
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
